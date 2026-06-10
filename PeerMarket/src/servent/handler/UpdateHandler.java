package servent.handler;

import java.util.ArrayList;
import java.util.List;

import app.AppConfig;
import app.ServentInfo;
import market.Item;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.util.MessageUtil;

public class UpdateHandler implements MessageHandler {

	private final Message clientMessage;

	public UpdateHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		if (clientMessage.getMessageType() != MessageType.UPDATE) {
			AppConfig.timestampedErrorPrint("Update handler got: " + clientMessage.getMessageType());
			return;
		}

		int myPort = AppConfig.myServentInfo.getListenerPort();

		if (clientMessage.getSenderPort() != myPort) {

			ServentInfo joiner = new ServentInfo("localhost", clientMessage.getSenderPort());
			List<ServentInfo> newNodes = new ArrayList<>();
			newNodes.add(joiner);
			AppConfig.chordState.addNodes(newNodes);
			AppConfig.announceNeighborsIfChanged();

			String newText;
			if (clientMessage.getMessageText().isEmpty()) {
				newText = String.valueOf(myPort);
			} else {
				newText = clientMessage.getMessageText() + "," + myPort;
			}
			MessageUtil.sendMessage(new UpdateMessage(
					clientMessage.getSenderPort(),
					AppConfig.chordState.getNextNodePort(), newText));
		} else {

			String[] ports = clientMessage.getMessageText().split(",");
			List<ServentInfo> allNodes = new ArrayList<>();
			for (String port : ports) {
				if (!port.isEmpty()) {
					allNodes.add(new ServentInfo("localhost", Integer.parseInt(port)));
				}
			}
			AppConfig.chordState.addNodes(allNodes);

			AppConfig.JOINED = true;
			AppConfig.failureDetector.start();
			AppConfig.announceNeighborsIfChanged();

			for (Item item : AppConfig.marketState.allItemsSnapshot()) {
				if (item.isPrimary()) {
					MessageUtil.replicate(item);
				}
			}
		}
	}
}
