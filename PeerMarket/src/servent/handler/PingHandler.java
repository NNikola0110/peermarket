package servent.handler;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class PingHandler implements MessageHandler {

	private final Message clientMessage;

	public PingHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.PING) {
			MessageUtil.sendMessage(new BasicMessage(MessageType.PONG,
					AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(), "PONG"));
		}
	}
}
