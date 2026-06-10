package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class SuspectCheckHandler implements MessageHandler {

	private final Message clientMessage;

	public SuspectCheckHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUSPECT_CHECK) {
			int targetPort = Integer.parseInt(clientMessage.getMessageText().trim());
			AppConfig.failureDetector.onSuspectCheck(clientMessage.getSenderPort(), targetPort);
		}
	}
}
