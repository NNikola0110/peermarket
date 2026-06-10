package mutex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import app.AppConfig;
import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class RicartAgrawalaMutex {

	private final ReentrantLock lock = new ReentrantLock();
	private final Condition granted = lock.newCondition();

	private long lamportClock = 0;
	private boolean requesting = false;
	private long myRequestTs = Long.MAX_VALUE;
	private int myReqSeq = 0;
	private int reqSeqCounter = 0;

	private Set<Integer> pendingReplies = new HashSet<>();

	private final Map<Integer, Integer> deferredReplies = new HashMap<>();

	private int myPort() {
		return AppConfig.myServentInfo.getListenerPort();
	}

	public void lock() {
		long ts;
		int reqSeq;
		List<Integer> peers;

		lock.lock();
		try {
			lamportClock++;
			myRequestTs = lamportClock;
			requesting = true;
			myReqSeq = ++reqSeqCounter;
			ts = myRequestTs;
			reqSeq = myReqSeq;
			pendingReplies = snapshotPeers();
			peers = new ArrayList<>(pendingReplies);
		} finally {
			lock.unlock();
		}

		broadcastRequest(peers, ts, reqSeq);

		lock.lock();
		try {
			long lastResend = System.currentTimeMillis();
			while (!pendingReplies.isEmpty()) {
				granted.await(500, TimeUnit.MILLISECONDS);

				pendingReplies.removeIf(p -> AppConfig.chordState.isDead(p));
				if (pendingReplies.isEmpty()) {
					break;
				}
				long now = System.currentTimeMillis();
				if (now - lastResend > 3000) {
					broadcastRequest(new ArrayList<>(pendingReplies), ts, reqSeq);
					lastResend = now;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			lock.unlock();
		}
	}

	public void unlock() {
		List<int[]> toReply = new ArrayList<>();
		lock.lock();
		try {
			requesting = false;
			myRequestTs = Long.MAX_VALUE;
			for (Map.Entry<Integer, Integer> e : deferredReplies.entrySet()) {
				toReply.add(new int[] { e.getKey(), e.getValue() });
			}
			deferredReplies.clear();
			pendingReplies.clear();
		} finally {
			lock.unlock();
		}
		for (int[] r : toReply) {
			sendReply(r[0], r[1]);
		}
	}

	public void onRequest(int fromPort, long theirTs, int theirReqSeq) {
		boolean defer;
		lock.lock();
		try {
			lamportClock = Math.max(lamportClock, theirTs) + 1;
			defer = requesting && (myRequestTs < theirTs
					|| (myRequestTs == theirTs && myPort() < fromPort));
			if (defer) {
				deferredReplies.put(fromPort, theirReqSeq);
			}
		} finally {
			lock.unlock();
		}
		if (!defer) {
			sendReply(fromPort, theirReqSeq);
		}
	}

	public void onReply(int fromPort, int echoReqSeq, long theirTs) {
		lock.lock();
		try {
			lamportClock = Math.max(lamportClock, theirTs) + 1;
			if (requesting && echoReqSeq == myReqSeq) {
				if (pendingReplies.remove(fromPort) && pendingReplies.isEmpty()) {
					granted.signalAll();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void peerGone(int port) {
		lock.lock();
		try {
			deferredReplies.remove(port);
			if (pendingReplies.remove(port) && pendingReplies.isEmpty()) {
				granted.signalAll();
			}
		} finally {
			lock.unlock();
		}
	}

	private Set<Integer> snapshotPeers() {
		Set<Integer> ports = new HashSet<>();
		for (ServentInfo si : AppConfig.chordState.getAllNodeInfo()) {
			int p = si.getListenerPort();
			if (p != myPort() && !AppConfig.chordState.isDead(p)) {
				ports.add(p);
			}
		}
		return ports;
	}

	private void broadcastRequest(List<Integer> peers, long ts, int reqSeq) {
		String text = ts + "," + reqSeq;
		for (int port : peers) {
			MessageUtil.sendMessage(new BasicMessage(MessageType.MUTEX_REQUEST, myPort(), port, text));
		}
	}

	private void sendReply(int toPort, int echoReqSeq) {
		long ts;
		lock.lock();
		try {
			lamportClock++;
			ts = lamportClock;
		} finally {
			lock.unlock();
		}
		String text = echoReqSeq + "," + ts;
		MessageUtil.sendMessage(new BasicMessage(MessageType.MUTEX_REPLY, myPort(), toPort, text));
	}
}
