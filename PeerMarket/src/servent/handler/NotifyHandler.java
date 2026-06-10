package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class NotifyHandler implements MessageHandler {

	private final Message clientMessage;

	public NotifyHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NOTIFY) {
			String[] parts = clientMessage.getMessageText().split(",");
			int creatorChordId = Integer.parseInt(parts[0].trim());
			int itemId = Integer.parseInt(parts[1].trim());
			AppConfig.tagPrint("[MARKET-NOTIFICATION] node:" + creatorChordId + " posted item_id:" + itemId);
		}
	}
}
