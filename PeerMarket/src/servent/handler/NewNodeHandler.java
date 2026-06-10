package servent.handler;

import java.util.List;

import app.AppConfig;
import app.ServentInfo;
import market.Item;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NewNodeMessage;
import servent.message.SorryMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

public class NewNodeHandler implements MessageHandler {

	private final Message clientMessage;

	public NewNodeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.NEW_NODE) {
			AppConfig.timestampedErrorPrint("NEW_NODE handler got: " + clientMessage.getMessageType());
			return;
		}

		int newNodePort = clientMessage.getSenderPort();
		ServentInfo newNodeInfo = new ServentInfo("localhost", newNodePort);

		if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
			MessageUtil.sendMessage(new SorryMessage(AppConfig.myServentInfo.getListenerPort(), newNodePort));
			return;
		}

		boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
		if (isMyPred) {

			AppConfig.chordState.setPredecessor(newNodeInfo);
			List<Item> handover = AppConfig.marketState.handOverItemsNoLongerMine();
			MessageUtil.sendMessage(new WelcomeMessage(
					AppConfig.myServentInfo.getListenerPort(), newNodePort, handover));
		} else {
			ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
			MessageUtil.sendMessage(new NewNodeMessage(newNodePort, nextNode.getListenerPort()));
		}
	}
}
