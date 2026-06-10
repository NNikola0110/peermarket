package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class NodeFailedHandler implements MessageHandler {

	private final Message clientMessage;

	public NodeFailedHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NODE_FAILED) {
			int deadPort = Integer.parseInt(clientMessage.getMessageText().trim());
			AppConfig.failureDetector.handleNodeGone(deadPort);
		}
	}
}
