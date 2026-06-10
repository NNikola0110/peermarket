package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import market.MarketState;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class BuyCommitHandler implements MessageHandler {

	private final Message clientMessage;

	public BuyCommitHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.BUY_COMMIT) {
			return;
		}
		String[] p = clientMessage.getMessageText().split(",");
		int routingKey = Integer.parseInt(p[0].trim());
		int itemId = Integer.parseInt(p[1].trim());
		int qty = Integer.parseInt(p[2].trim());
		int buyerPort = Integer.parseInt(p[3].trim());
		int commitSeq = Integer.parseInt(p[4].trim());
		String commitId = buyerPort + ":" + commitSeq;

		int myPort = AppConfig.myServentInfo.getListenerPort();

		if (AppConfig.chordState.isKeyMine(routingKey)) {
			MarketState.BuyOutcome outcome = AppConfig.marketState.tryBuy(itemId, qty, commitId);
			if (outcome.success && outcome.updated != null) {
				MessageUtil.replicate(outcome.updated);
			}
			int success = outcome.success ? 1 : 0;
			MessageUtil.sendMessage(new BasicMessage(MessageType.BUY_REPLY,
					myPort, buyerPort, commitId + "," + success + "," + outcome.remaining));
		} else {
			ServentInfo next = AppConfig.chordState.getNextNodeForKey(routingKey);
			MessageUtil.sendMessage(new BasicMessage(MessageType.BUY_COMMIT,
					myPort, next.getListenerPort(), clientMessage.getMessageText()));
		}
	}
}
