package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class MutexReplyHandler implements MessageHandler {

	private final Message clientMessage;

	public MutexReplyHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.MUTEX_REPLY) {
			return;
		}
		String[] p = clientMessage.getMessageText().split(",");
		int echoReqSeq = Integer.parseInt(p[0].trim());
		long ts = Long.parseLong(p[1].trim());
		AppConfig.ricartAgrawalaMutex.onReply(clientMessage.getSenderPort(), echoReqSeq, ts);
	}
}
