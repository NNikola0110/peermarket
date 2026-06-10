package servent.handler;

import app.AppConfig;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;

public class AddItemBackupHandler implements MessageHandler {

	private final Message clientMessage;

	public AddItemBackupHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.ADD_ITEM_BACKUP) {
			return;
		}
		ItemMessage im = (ItemMessage) clientMessage;
		AppConfig.marketState.upsertBackup(im.getItem());
	}
}
