package servent.message.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import servent.message.Message;

public class DelayedMessageSender implements Runnable {

	private final Message messageToSend;

	public DelayedMessageSender(Message messageToSend) {
		this.messageToSend = messageToSend;
	}

	@Override
	public void run() {
		try {
			Thread.sleep((long) (Math.random() * 400) + 100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}

		if (MessageUtil.MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
		}

		try {
			Socket sendSocket = new Socket(messageToSend.getReceiverIpAddress(), messageToSend.getReceiverPort());
			ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
			oos.writeObject(messageToSend);
			oos.flush();
			sendSocket.close();
		} catch (IOException e) {

			if (MessageUtil.MESSAGE_UTIL_PRINTING) {
				AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend);
			}
		}
	}
}
