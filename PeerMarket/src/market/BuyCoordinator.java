package market;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BuyCoordinator {

	private static final Map<String, BlockingQueue<int[]>> pending = new ConcurrentHashMap<>();

	public static void register(String commitId) {
		pending.put(commitId, new ArrayBlockingQueue<>(1));
	}

	public static int[] await(String commitId, long timeoutMs) throws InterruptedException {
		BlockingQueue<int[]> q = pending.get(commitId);
		if (q == null) {
			return null;
		}
		return q.poll(timeoutMs, TimeUnit.MILLISECONDS);
	}

	public static void complete(String commitId, int success, int remaining) {
		BlockingQueue<int[]> q = pending.get(commitId);
		if (q != null) {
			q.offer(new int[] { success, remaining });
		}
	}

	public static void cleanup(String commitId) {
		pending.remove(commitId);
	}
}
