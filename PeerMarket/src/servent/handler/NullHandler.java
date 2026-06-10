package servent.handler;

import app.AppConfig;
import servent.message.Message;

public class NullHandler implements MessageHandler {

	private final Message clientMessage;

	public NullHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		AppConfig.timestampedErrorPrint("UNHANDLED message type "
				+ clientMessage.getMessageType() + ": " + clientMessage);
	}
}
