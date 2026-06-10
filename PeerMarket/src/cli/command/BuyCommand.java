package cli.command;

import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import market.BuyCoordinator;
import market.MarketState;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class BuyCommand implements CLICommand {

	private static final AtomicInteger COMMIT_SEQ = new AtomicInteger(0);
	private static final long REPLY_TIMEOUT_MS = 6000;

	@Override
	public String commandName() {
		return "buy";
	}

	@Override
	public void execute(String args) {
		if (!AppConfig.JOINED) {
			AppConfig.timestampedErrorPrint("Not joined yet; cannot buy.");
			return;
		}
		String[] toks = args == null ? new String[0] : args.trim().split("\\s+");
		if (toks.length != 2) {
			AppConfig.timestampedErrorPrint("Usage: buy [item_id] [kolicina]");
			return;
		}
		int itemId, qty;
		try {
			itemId = Integer.parseInt(toks[0]);
			qty = Integer.parseInt(toks[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("item_id and kolicina must be integers.");
			return;
		}

		int routingKey = ChordState.chordHash(itemId);
		int myPort = AppConfig.myServentInfo.getListenerPort();

		AppConfig.tagPrint("[MUTEX-REQUEST] item_id:" + itemId);
		AppConfig.ricartAgrawalaMutex.lock();
		try {
			AppConfig.tagPrint("[MUTEX-ACQUIRED]");

			int[] result = doBuy(itemId, qty, routingKey, myPort);
			if (result != null && result[0] == 1) {
				AppConfig.tagPrint("[MARKET-BUY-SUCCESS] item_id:" + itemId
						+ " qty_bought:" + qty + " remaining_qty:" + result[1]);
			} else {
				AppConfig.tagPrint("[MARKET-BUY-FAIL] item_id:" + itemId + " reason:OUT_OF_STOCK");
			}
		} finally {
			AppConfig.ricartAgrawalaMutex.unlock();
			AppConfig.tagPrint("[MUTEX-RELEASED]");
		}
	}

	private int[] doBuy(int itemId, int qty, int routingKey, int myPort) {
		if (AppConfig.chordState.isKeyMine(routingKey)) {
			String commitId = myPort + ":" + COMMIT_SEQ.incrementAndGet();
			MarketState.BuyOutcome outcome = AppConfig.marketState.tryBuy(itemId, qty, commitId);
			if (outcome.success && outcome.updated != null) {
				MessageUtil.replicate(outcome.updated);
			}
			return new int[] { outcome.success ? 1 : 0, outcome.remaining };
		}

		int commitSeq = COMMIT_SEQ.incrementAndGet();
		String commitId = myPort + ":" + commitSeq;
		BuyCoordinator.register(commitId);
		try {
			sendCommit(routingKey, itemId, qty, myPort, commitSeq);
			int[] res = awaitReply(commitId);
			if (res == null) {

				sendCommit(routingKey, itemId, qty, myPort, commitSeq);
				res = awaitReply(commitId);
			}
			return res;
		} finally {
			BuyCoordinator.cleanup(commitId);
		}
	}

	private void sendCommit(int routingKey, int itemId, int qty, int myPort, int commitSeq) {
		ServentInfo responsible = AppConfig.chordState.getNextNodeForKey(routingKey);
		String text = routingKey + "," + itemId + "," + qty + "," + myPort + "," + commitSeq;
		MessageUtil.sendMessage(new BasicMessage(MessageType.BUY_COMMIT,
				myPort, responsible.getListenerPort(), text));
	}

	private int[] awaitReply(String commitId) {
		try {
			return BuyCoordinator.await(commitId, REPLY_TIMEOUT_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}
}
