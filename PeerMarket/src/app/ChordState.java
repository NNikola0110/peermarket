package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChordState {

	public static int CHORD_SIZE;

	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}

	private final int chordLevel;

	private ServentInfo[] successorTable;
	private ServentInfo predecessorInfo;

	private final List<ServentInfo> allNodeInfo;

	private final Set<Integer> deadPorts;

	public ChordState() {
		this.chordLevel = Integer.numberOfTrailingZeros(CHORD_SIZE);
		if (CHORD_SIZE != (1 << chordLevel)) {
			throw new NumberFormatException("CHORD_SIZE must be a power of 2");
		}
		successorTable = new ServentInfo[chordLevel];
		predecessorInfo = null;
		allNodeInfo = new ArrayList<>();
		deadPorts = new HashSet<>();
	}

	public synchronized void init(int welcomeSenderPort) {
		successorTable[0] = new ServentInfo("localhost", welcomeSenderPort);
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			bsWriter.flush();
			bsSocket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't notify bootstrap of successful join: " + e.getMessage());
		}
	}

	public synchronized int getChordLevel() {
		return chordLevel;
	}

	public synchronized ServentInfo[] getSuccessorTable() {
		return successorTable;
	}

	public synchronized int getNextNodePort() {
		if (successorTable[0] == null) {
			return AppConfig.myServentInfo.getListenerPort();
		}
		return successorTable[0].getListenerPort();
	}

	public synchronized ServentInfo getPredecessor() {
		return predecessorInfo;
	}

	public synchronized void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public synchronized List<ServentInfo> getAllNodeInfo() {
		return new ArrayList<>(allNodeInfo);
	}

	public synchronized boolean isDead(int port) {
		return deadPorts.contains(port);
	}

	public synchronized boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}
		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = AppConfig.myServentInfo.getChordId();

		if (predecessorChordId < myChordId) {
			return key <= myChordId && key > predecessorChordId;
		} else {
			return key <= myChordId || key > predecessorChordId;
		}
	}

	public synchronized ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}
		int myId = AppConfig.myServentInfo.getChordId();
		int myPort = AppConfig.myServentInfo.getListenerPort();

		for (int i = chordLevel - 1; i >= 0; i--) {
			ServentInfo finger = successorTable[i];
			if (finger == null || finger.getListenerPort() == myPort) {
				continue;
			}
			if (inOpenInterval(finger.getChordId(), myId, key)) {
				return finger;
			}
		}

		if (!allNodeInfo.isEmpty()) {
			return allNodeInfo.get(0);
		}
		return AppConfig.myServentInfo;
	}

	private boolean inOpenInterval(int x, int a, int b) {
		if (a == b) {
			return x != a;
		}
		if (a < b) {
			return x > a && x < b;
		}
		return x > a || x < b;
	}

	public synchronized void addNodes(List<ServentInfo> newNodes) {
		int myPort = AppConfig.myServentInfo.getListenerPort();
		for (ServentInfo si : newNodes) {
			int port = si.getListenerPort();
			if (port == myPort || deadPorts.contains(port)) {
				continue;
			}
			boolean exists = false;
			for (ServentInfo existing : allNodeInfo) {
				if (existing.getListenerPort() == port) {
					exists = true;
					break;
				}
			}
			if (!exists) {
				allNodeInfo.add(si);
			}
		}
		reorderAndSetPredecessor();
		updateSuccessorTable();
	}

	public synchronized void removeNode(int deadPort) {
		deadPorts.add(deadPort);
		allNodeInfo.removeIf(si -> si.getListenerPort() == deadPort);

		if (allNodeInfo.isEmpty()) {
			predecessorInfo = null;
			for (int i = 0; i < chordLevel; i++) {
				successorTable[i] = AppConfig.myServentInfo;
			}
			return;
		}
		reorderAndSetPredecessor();
		updateSuccessorTable();
	}

	private void reorderAndSetPredecessor() {
		if (allNodeInfo.isEmpty()) {
			predecessorInfo = null;
			return;
		}
		allNodeInfo.sort(Comparator.comparingInt(ServentInfo::getChordId));

		int myId = AppConfig.myServentInfo.getChordId();
		List<ServentInfo> after = new ArrayList<>();
		List<ServentInfo> before = new ArrayList<>();
		for (ServentInfo si : allNodeInfo) {
			if (si.getChordId() < myId) {
				before.add(si);
			} else {
				after.add(si);
			}
		}
		allNodeInfo.clear();
		allNodeInfo.addAll(after);
		allNodeInfo.addAll(before);

		if (!before.isEmpty()) {
			predecessorInfo = before.get(before.size() - 1);
		} else {
			predecessorInfo = after.get(after.size() - 1);
		}
	}

	private void updateSuccessorTable() {
		List<ServentInfo> ring = selfInclusiveSortedRing();
		if (ring.size() == 1) {
			for (int i = 0; i < chordLevel; i++) {
				successorTable[i] = AppConfig.myServentInfo;
			}
			return;
		}
		int myId = AppConfig.myServentInfo.getChordId();
		ServentInfo[] newTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			int target = (myId + (1 << i)) % CHORD_SIZE;
			newTable[i] = ownerOf(ring, target);
		}
		successorTable = newTable;
	}

	private List<ServentInfo> selfInclusiveSortedRing() {
		Set<Integer> seenPorts = new HashSet<>();
		List<ServentInfo> ring = new ArrayList<>();
		ring.add(AppConfig.myServentInfo);
		seenPorts.add(AppConfig.myServentInfo.getListenerPort());
		for (ServentInfo si : allNodeInfo) {
			if (seenPorts.add(si.getListenerPort())) {
				ring.add(si);
			}
		}
		ring.sort(Comparator.comparingInt(ServentInfo::getChordId));
		return ring;
	}

	private ServentInfo ownerOf(List<ServentInfo> sortedRing, int key) {
		for (ServentInfo si : sortedRing) {
			if (si.getChordId() >= key) {
				return si;
			}
		}
		return sortedRing.get(0);
	}

	public synchronized List<ServentInfo> getImmediateSuccessors(int k) {
		List<ServentInfo> result = new ArrayList<>();
		for (ServentInfo si : allNodeInfo) {
			result.add(si);
			if (result.size() >= k) {
				break;
			}
		}
		return result;
	}

	public synchronized ServentInfo successorOfChordId(int deadChordId) {
		List<ServentInfo> ring = selfInclusiveSortedRing();
		ring.removeIf(si -> si.getChordId() == deadChordId);
		if (ring.isEmpty()) {
			return AppConfig.myServentInfo;
		}
		for (ServentInfo si : ring) {
			if (si.getChordId() > deadChordId) {
				return si;
			}
		}
		return ring.get(0);
	}

	public synchronized List<ServentInfo> getMonitoredNodes() {
		int myPort = AppConfig.myServentInfo.getListenerPort();
		Set<ServentInfo> set = new LinkedHashSet<>();
		if (predecessorInfo != null && predecessorInfo.getListenerPort() != myPort) {
			set.add(predecessorInfo);
		}
		int count = 0;
		for (ServentInfo si : allNodeInfo) {
			if (si.getListenerPort() == myPort) {
				continue;
			}
			set.add(si);
			if (++count >= 3) {
				break;
			}
		}
		return new ArrayList<>(set);
	}

	public synchronized List<Integer> getNeighborChordIds() {
		int myPort = AppConfig.myServentInfo.getListenerPort();
		Set<Integer> ids = new HashSet<>();
		for (ServentInfo si : successorTable) {
			if (si != null && si.getListenerPort() != myPort) {
				ids.add(si.getChordId());
			}
		}
		if (predecessorInfo != null && predecessorInfo.getListenerPort() != myPort) {
			ids.add(predecessorInfo.getChordId());
		}
		List<Integer> sorted = new ArrayList<>(ids);
		sorted.sort(Integer::compareTo);
		return sorted;
	}
}
