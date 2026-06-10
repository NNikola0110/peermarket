package servent.handler;

import market.Item;
import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

public class WelcomeHandler implements MessageHandler {

	private final Message clientMessage;

	public WelcomeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.WELCOME) {
			AppConfig.timestampedErrorPrint("Welcome handler got: " + clientMessage.getMessageType());
			return;
		}

		WelcomeMessage welcomeMsg = (WelcomeMessage) clientMessage;
		AppConfig.chordState.init(welcomeMsg.getSenderPort());

		int myChordId = AppConfig.myServentInfo.getChordId();
		if (welcomeMsg.getItems() != null) {
			for (Item item : welcomeMsg.getItems()) {
				Item mine = item.copy();
				mine.setPrimary(true);
				mine.setOwnerChordId(myChordId);
				AppConfig.marketState.addPrimary(mine);
			}
		}

		UpdateMessage um = new UpdateMessage(
				AppConfig.myServentInfo.getListenerPort(),
				AppConfig.chordState.getNextNodePort(), "");
		MessageUtil.sendMessage(um);
	}
}
