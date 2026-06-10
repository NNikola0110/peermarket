package market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.ChordState;

public class MarketState {

	private final Map<Integer, Item> items = new ConcurrentHashMap<>();

	private final Set<Integer> subscribers = ConcurrentHashMap.newKeySet();

	private final Map<String, BuyOutcome> processedCommits = new HashMap<>();

	private final Map<Integer, Long> searchSeen = new HashMap<>();

	public static class BuyOutcome {
		public final boolean success;
		public final int remaining;

		public final Item updated;

		public BuyOutcome(boolean success, int remaining, Item updated) {
			this.success = success;
			this.remaining = remaining;
			this.updated = updated;
		}
	}

	public synchronized boolean addPrimary(Item item) {
		if (items.containsKey(item.getItemId())) {
			return false;
		}
		Item stored = item.copy();
		stored.setPrimary(true);
		items.put(stored.getItemId(), stored);
		return true;
	}

	public synchronized void upsertBackup(Item incoming) {
		Item existing = items.get(incoming.getItemId());
		if (existing == null) {
			Item stored = incoming.copy();
			stored.setPrimary(false);
			items.put(stored.getItemId(), stored);
			return;
		}
		if (incoming.getVersion() > existing.getVersion()) {
			existing.setQuantity(incoming.getQuantity());
			existing.setVersion(incoming.getVersion());
			existing.setOwnerChordId(incoming.getOwnerChordId());

		}
	}

	public synchronized BuyOutcome tryBuy(int itemId, int want, String commitId) {
		BuyOutcome cached = processedCommits.get(commitId);
		if (cached != null) {
			return cached;
		}

		Item item = items.get(itemId);
		BuyOutcome outcome;
		if (item == null || !item.isPrimary()) {
			outcome = new BuyOutcome(false, -1, null);
		} else if (want <= 0) {
			outcome = new BuyOutcome(false, item.getQuantity(), null);
		} else if (item.getQuantity() >= want) {
			item.setQuantity(item.getQuantity() - want);
			item.setVersion(item.getVersion() + 1);
			outcome = new BuyOutcome(true, item.getQuantity(), item.copy());
		} else {
			outcome = new BuyOutcome(false, item.getQuantity(), null);
		}
		processedCommits.put(commitId, outcome);
		return outcome;
	}

	public synchronized boolean isPrimaryHere(int itemId) {
		Item item = items.get(itemId);
		return item != null && item.isPrimary();
	}

	public synchronized boolean markSearchSeen(int origin, long seq) {
		Long max = searchSeen.get(origin);
		if (max != null && seq <= max) {
			return false;
		}
		searchSeen.put(origin, seq);
		return true;
	}

	public synchronized List<Item> searchPrimary(String query) {
		List<Item> result = new ArrayList<>();
		for (Item item : items.values()) {
			if (item.isPrimary() && item.getName().equalsIgnoreCase(query)) {
				result.add(item.copy());
			}
		}
		return result;
	}

	public synchronized List<Item> handOverItemsNoLongerMine() {
		List<Item> moved = new ArrayList<>();
		for (Item item : items.values()) {
			if (!item.isPrimary()) {
				continue;
			}
			int key = ChordState.chordHash(item.getItemId());
			if (!AppConfig.chordState.isKeyMine(key)) {
				moved.add(item.copy());
				item.setPrimary(false);
			}
		}
		return moved;
	}

	public synchronized List<Item> promoteMyBackups(int myChordId) {
		List<Item> promoted = new ArrayList<>();
		for (Item item : items.values()) {
			if (item.isPrimary()) {
				continue;
			}
			int key = ChordState.chordHash(item.getItemId());
			if (AppConfig.chordState.isKeyMine(key)) {
				item.setPrimary(true);
				item.setOwnerChordId(myChordId);
				promoted.add(item.copy());
			}
		}
		return promoted;
	}

	public void addSubscriber(int port) {
		subscribers.add(port);
	}

	public Set<Integer> getSubscribersSnapshot() {
		return new java.util.HashSet<>(subscribers);
	}

	public synchronized List<Item> allItemsSnapshot() {
		List<Item> result = new ArrayList<>();
		for (Item item : items.values()) {
			result.add(item.copy());
		}
		return result;
	}
}
