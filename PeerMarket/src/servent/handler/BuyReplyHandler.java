package servent.handler;

import market.BuyCoordinator;
import servent.message.Message;
import servent.message.MessageType;

public class BuyReplyHandler implements MessageHandler {

	private final Message clientMessage;

	public BuyReplyHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.BUY_REPLY) {
			return;
		}

		String[] p = clientMessage.getMessageText().split(",");
		String commitId = p[0].trim();
		int success = Integer.parseInt(p[1].trim());
		int remaining = Integer.parseInt(p[2].trim());
		BuyCoordinator.complete(commitId, success, remaining);
	}
}
