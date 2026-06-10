package servent.message.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.List;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import market.Item;
import servent.message.ItemMessage;
import servent.message.Message;
import servent.message.MessageType;

public class MessageUtil {

	public static final boolean MESSAGE_UTIL_PRINTING = false;

	public static Message readMessage(Socket socket) {
		Message clientMessage = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			clientMessage = (Message) ois.readObject();
			socket.close();
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Error reading socket on " +
					socket.getInetAddress() + ":" + socket.getPort());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if (MESSAGE_UTIL_PRINTING && clientMessage != null) {
			AppConfig.timestampedStandardPrint("Got message " + clientMessage);
		}
		return clientMessage;
	}

	public static void sendMessage(Message message) {
		Thread sender = new Thread(new DelayedMessageSender(message));
		sender.setDaemon(true);
		sender.start();
	}

	public static void replicate(Item item) {
		int routingKey = ChordState.chordHash(item.getItemId());
		int myPort = AppConfig.myServentInfo.getListenerPort();
		List<ServentInfo> successors = AppConfig.chordState.getImmediateSuccessors(2);
		for (ServentInfo succ : successors) {
			Item replica = item.copy();
			replica.setPrimary(false);
			sendMessage(new ItemMessage(MessageType.ADD_ITEM_BACKUP, myPort, succ.getListenerPort(),
					replica, routingKey, myPort));
		}
	}
}
