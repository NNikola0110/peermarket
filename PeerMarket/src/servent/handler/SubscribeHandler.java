package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class SubscribeHandler implements MessageHandler {

	private final Message clientMessage;

	public SubscribeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.SUBSCRIBE) {
			AppConfig.marketState.addSubscriber(clientMessage.getSenderPort());
		}
	}
}
