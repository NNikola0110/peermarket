package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class SorryHandler implements MessageHandler {

	private final Message clientMessage;

	public SorryHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SORRY) {
			AppConfig.timestampedErrorPrint("Couldn't join: Chord id collision. Change my listener port.");
			System.exit(0);
		}
	}
}
