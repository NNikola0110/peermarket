package servent.handler;

import app.AppConfig;
import market.Item;
import servent.message.BasicMessage;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class AddItemOkHandler implements MessageHandler {

	private final Message clientMessage;

	public AddItemOkHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.ADD_ITEM_OK) {
			return;
		}
		ItemMessage im = (ItemMessage) clientMessage;
		Item item = im.getItem();

		AppConfig.tagPrint("[MARKET-LIST] item_id:" + item.getItemId()
				+ " name:\"" + item.getName() + "\" qty:" + item.getQuantity());

		int myPort = AppConfig.myServentInfo.getListenerPort();
		int myChordId = AppConfig.myServentInfo.getChordId();
		for (int subscriberPort : AppConfig.marketState.getSubscribersSnapshot()) {
			MessageUtil.sendMessage(new BasicMessage(MessageType.NOTIFY,
					myPort, subscriberPort, myChordId + "," + item.getItemId()));
		}
	}
}
