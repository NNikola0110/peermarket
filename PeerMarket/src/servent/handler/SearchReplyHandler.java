package servent.handler;

import app.AppConfig;
import market.Item;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;

public class SearchReplyHandler implements MessageHandler {

	private final Message clientMessage;

	public SearchReplyHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.SEARCH_REPLY) {
			return;
		}
		ItemMessage im = (ItemMessage) clientMessage;
		Item item = im.getItem();
		AppConfig.tagPrint("[MARKET-SEARCH-RESULT] item_id:" + item.getItemId()
				+ " name:\"" + item.getName() + "\" qty:" + item.getQuantity()
				+ " owner_id:" + item.getOwnerChordId());
	}
}
