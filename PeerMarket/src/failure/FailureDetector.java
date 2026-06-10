package failure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import market.Item;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class FailureDetector {

	private static final long HEARTBEAT_PERIOD_MS = 2000;
	private static final long CHECK_PERIOD_MS = 1000;
	private static final long CONFIRM_TIMEOUT_MS = 4000;

	private enum State { ALIVE, SUSPECT, DEAD }

	private static class Monitor {
		volatile State state = State.ALIVE;
		volatile long suspectSince = 0;

		volatile int confirm = 0;
	}

	private final Map<Integer, Long> lastSeen = new ConcurrentHashMap<>();

	private final Map<Integer, Monitor> monitors = new ConcurrentHashMap<>();

	private final Set<Integer> failedNodes = ConcurrentHashMap.newKeySet();

	private ScheduledExecutorService scheduler;

	private static int myPort() {
		return AppConfig.myServentInfo.getListenerPort();
	}

	public void start() {
		ThreadFactory daemonFactory = r -> {
			Thread t = new Thread(r, "failure-detector");
			t.setDaemon(true);
			return t;
		};
		scheduler = Executors.newScheduledThreadPool(1, daemonFactory);
		scheduler.scheduleAtFixedRate(this::heartbeatTick, HEARTBEAT_PERIOD_MS, HEARTBEAT_PERIOD_MS, TimeUnit.MILLISECONDS);
		scheduler.scheduleAtFixedRate(this::checkTick, CHECK_PERIOD_MS, CHECK_PERIOD_MS, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

	public void markSeen(int port) {
		long now = System.currentTimeMillis();
		lastSeen.merge(port, now, Math::max);
	}

	private void heartbeatTick() {
		try {
			List<ServentInfo> monitored = AppConfig.chordState.getMonitoredNodes();
			Set<Integer> monitoredPorts = ConcurrentHashMap.newKeySet();
			long now = System.currentTimeMillis();
			for (ServentInfo si : monitored) {
				int p = si.getListenerPort();
				monitoredPorts.add(p);
				monitors.computeIfAbsent(p, k -> new Monitor());
				lastSeen.putIfAbsent(p, now);
				MessageUtil.sendMessage(new BasicMessage(MessageType.PING, myPort(), p, "PING"));
			}

			monitors.keySet().removeIf(p -> !monitoredPorts.contains(p) && !failedNodes.contains(p));
		} catch (Exception e) {
			AppConfig.timestampedErrorPrint("Heartbeat tick error: " + e.getMessage());
		}
	}

	private void checkTick() {
		try {
			long now = System.currentTimeMillis();
			for (Map.Entry<Integer, Monitor> entry : new ArrayList<>(monitors.entrySet())) {
				int port = entry.getKey();
				Monitor m = entry.getValue();
				if (m.state == State.DEAD) {
					continue;
				}
				long seen = lastSeen.getOrDefault(port, now);
				long elapsed = now - seen;
				int chordId = ChordState.chordHash(port);

				switch (m.state) {
				case ALIVE:
					if (elapsed > AppConfig.WEAK_LIMIT) {
						m.state = State.SUSPECT;
						m.suspectSince = now;
						m.confirm = 0;
						AppConfig.tagPrint("[SYS-WEAK-FAIL] node:" + chordId);
						sendSuspectCheck(port);
					}
					break;
				case SUSPECT:
					if (elapsed < AppConfig.WEAK_LIMIT || m.confirm == 1) {

						m.state = State.ALIVE;
						m.confirm = 0;
						lastSeen.merge(port, now, Math::max);
					} else if (elapsed > AppConfig.STRONG_LIMIT
							&& (m.confirm == -1 || now - m.suspectSince > CONFIRM_TIMEOUT_MS)) {

						m.state = State.DEAD;
						declareDead(port, chordId);
					}
					break;
				default:
					break;
				}
			}
		} catch (Exception e) {
			AppConfig.timestampedErrorPrint("Check tick error: " + e.getMessage());
		}
	}

	private void sendSuspectCheck(int targetPort) {
		Integer confirmer = pickConfirmer(targetPort);
		if (confirmer != null) {
			MessageUtil.sendMessage(new BasicMessage(MessageType.SUSPECT_CHECK, myPort(), confirmer,
					String.valueOf(targetPort)));
		}
	}

	private Integer pickConfirmer(int targetPort) {
		for (ServentInfo si : AppConfig.chordState.getAllNodeInfo()) {
			int p = si.getListenerPort();
			if (p != targetPort && p != myPort()) {
				return p;
			}
		}
		return null;
	}

	public void onSuspectCheck(int fromPort, int targetPort) {
		long now = System.currentTimeMillis();
		Long seen = lastSeen.get(targetPort);
		int status;
		if (seen != null && now - seen < AppConfig.WEAK_LIMIT) {
			status = 1;
		} else if (seen != null) {
			status = 0;
		} else {
			status = -1;
			MessageUtil.sendMessage(new BasicMessage(MessageType.PING, myPort(), targetPort, "PING"));
		}
		MessageUtil.sendMessage(new BasicMessage(MessageType.SUSPECT_REPLY, myPort(), fromPort,
				targetPort + "," + status));
	}

	public void onSuspectReply(int targetPort, int status) {
		Monitor m = monitors.get(targetPort);
		if (m == null || m.state != State.SUSPECT) {
			return;
		}
		if (status == 1) {
			m.confirm = 1;
		} else if (status == 0) {
			m.confirm = -1;
		}

	}

	private void declareDead(int deadPort, int deadChordId) {
		if (failedNodes.contains(deadPort)) {
			return;
		}
		AppConfig.tagPrint("[SYS-STRONG-FAIL] node:" + deadChordId);

		for (ServentInfo si : AppConfig.chordState.getAllNodeInfo()) {
			int p = si.getListenerPort();
			if (p != myPort() && p != deadPort) {
				MessageUtil.sendMessage(new BasicMessage(MessageType.NODE_FAILED, myPort(), p,
						String.valueOf(deadPort)));
			}
		}
		handleNodeGone(deadPort);
	}

	public void handleNodeGone(int deadPort) {
		if (!failedNodes.add(deadPort)) {
			return;
		}
		int deadChordId = ChordState.chordHash(deadPort);

		AppConfig.ricartAgrawalaMutex.peerGone(deadPort);

		ServentInfo successor = AppConfig.chordState.successorOfChordId(deadChordId);
		boolean iAmSuccessor = successor.getListenerPort() == myPort();

		AppConfig.chordState.removeNode(deadPort);

		if (iAmSuccessor) {
			List<Item> promoted = AppConfig.marketState.promoteMyBackups(AppConfig.myServentInfo.getChordId());
			if (!promoted.isEmpty()) {
				for (Item item : promoted) {
					MessageUtil.replicate(item);
				}
				List<Integer> ids = new ArrayList<>();
				for (Item item : promoted) {
					ids.add(item.getItemId());
				}
				ids.sort(Integer::compareTo);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < ids.size(); i++) {
					if (i > 0) {
						sb.append(",");
					}
					sb.append(ids.get(i));
				}
				AppConfig.tagPrint("[SYS-BACKUP-TAKEOVER] node:" + deadChordId + " item_ids:" + sb);
			}
		}

		monitors.remove(deadPort);
		lastSeen.remove(deadPort);
		Monitor dm = new Monitor();
		dm.state = State.DEAD;
		monitors.put(deadPort, dm);

		AppConfig.announceNeighborsIfChanged();
	}
}
