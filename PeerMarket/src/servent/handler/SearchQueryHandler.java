package servent.handler;

import app.AppConfig;
import app.ChordState;
import market.Item;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.SearchQueryMessage;
import servent.message.util.MessageUtil;

public class SearchQueryHandler implements MessageHandler {

	private final Message clientMessage;

	public SearchQueryHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.SEARCH_QUERY) {
			return;
		}
		SearchQueryMessage q = (SearchQueryMessage) clientMessage;
		int myPort = AppConfig.myServentInfo.getListenerPort();

		if (!AppConfig.marketState.markSearchSeen(q.getOriginPort(), q.getSeq())) {
			return;
		}

		for (Item item : AppConfig.marketState.searchPrimary(q.getQuery())) {
			MessageUtil.sendMessage(new ItemMessage(MessageType.SEARCH_REPLY,
					myPort, q.getOriginPort(), item, ChordState.chordHash(item.getItemId()), myPort));
		}

		int nextPort = AppConfig.chordState.getNextNodePort();
		if (q.getHopsLeft() > 1 && nextPort != q.getOriginPort() && nextPort != myPort) {
			MessageUtil.sendMessage(new SearchQueryMessage(myPort, nextPort,
					q.getOriginPort(), q.getSeq(), q.getHopsLeft() - 1, q.getQuery()));
		}
	}
}
