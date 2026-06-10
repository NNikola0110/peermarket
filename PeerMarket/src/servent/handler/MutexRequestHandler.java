package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class MutexRequestHandler implements MessageHandler {

	private final Message clientMessage;

	public MutexRequestHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.MUTEX_REQUEST) {
			return;
		}
		String[] p = clientMessage.getMessageText().split(",");
		long ts = Long.parseLong(p[0].trim());
		int reqSeq = Integer.parseInt(p[1].trim());
		AppConfig.ricartAgrawalaMutex.onRequest(clientMessage.getSenderPort(), ts, reqSeq);
	}
}
