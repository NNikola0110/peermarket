package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import market.Item;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class AddItemHandler implements MessageHandler {

	private final Message clientMessage;

	public AddItemHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.ADD_ITEM) {
			return;
		}
		ItemMessage im = (ItemMessage) clientMessage;
		int routingKey = im.getRoutingKey();
		int myPort = AppConfig.myServentInfo.getListenerPort();

		if (AppConfig.chordState.isKeyMine(routingKey)) {
			Item stored = im.getItem().copy();
			stored.setPrimary(true);
			stored.setOwnerChordId(AppConfig.myServentInfo.getChordId());
			AppConfig.marketState.addPrimary(stored);
			MessageUtil.replicate(stored);
			MessageUtil.sendMessage(new ItemMessage(MessageType.ADD_ITEM_OK,
					myPort, im.getOriginPort(), stored, routingKey, im.getOriginPort()));
		} else {
			ServentInfo next = AppConfig.chordState.getNextNodeForKey(routingKey);
			MessageUtil.sendMessage(new ItemMessage(MessageType.ADD_ITEM,
					myPort, next.getListenerPort(), im.getItem(), routingKey, im.getOriginPort()));
		}
	}
}
