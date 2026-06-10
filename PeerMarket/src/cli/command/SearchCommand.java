package cli.command;

import java.util.concurrent.atomic.AtomicLong;

import app.AppConfig;
import market.Item;
import servent.message.MessageType;
import servent.message.SearchQueryMessage;
import servent.message.util.MessageUtil;

public class SearchCommand implements CLICommand {

	private static final AtomicLong SEARCH_SEQ = new AtomicLong(0);

	@Override
	public String commandName() {
		return "search";
	}

	@Override
	public void execute(String args) {
		if (!AppConfig.JOINED) {
			AppConfig.timestampedErrorPrint("Not joined yet; cannot search.");
			return;
		}
		if (args == null || args.trim().isEmpty()) {
			AppConfig.timestampedErrorPrint("Usage: search [naziv]");
			return;
		}
		String query = args.trim();
		int myPort = AppConfig.myServentInfo.getListenerPort();

		for (Item item : AppConfig.marketState.searchPrimary(query)) {
			AppConfig.tagPrint("[MARKET-SEARCH-RESULT] item_id:" + item.getItemId()
					+ " name:\"" + item.getName() + "\" qty:" + item.getQuantity()
					+ " owner_id:" + item.getOwnerChordId());
		}

		long seq = SEARCH_SEQ.incrementAndGet();
		AppConfig.marketState.markSearchSeen(myPort, seq);
		int hops = AppConfig.chordState.getAllNodeInfo().size() + 2;
		int nextPort = AppConfig.chordState.getNextNodePort();
		if (nextPort != myPort) {
			MessageUtil.sendMessage(new SearchQueryMessage(myPort, nextPort, myPort, seq, hops, query));
		}
	}
}
