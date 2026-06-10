package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class SuspectReplyHandler implements MessageHandler {

	private final Message clientMessage;

	public SuspectReplyHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUSPECT_REPLY) {
			String[] parts = clientMessage.getMessageText().split(",");
			int targetPort = Integer.parseInt(parts[0].trim());
			int status = Integer.parseInt(parts[1].trim());
			AppConfig.failureDetector.onSuspectReply(targetPort, status);
		}
	}
}
