package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class NodeLeftHandler implements MessageHandler {

	private final Message clientMessage;

	public NodeLeftHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NODE_LEFT) {
			int leftPort = Integer.parseInt(clientMessage.getMessageText().trim());
			AppConfig.failureDetector.handleNodeGone(leftPort);
		}
	}
}
