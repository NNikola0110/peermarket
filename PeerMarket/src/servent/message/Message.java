package servent.message;

import java.io.Serializable;

public interface Message extends Serializable {

	int getSenderPort();

	int getReceiverPort();

	String getReceiverIpAddress();

	MessageType getMessageType();

	String getMessageText();

	int getMessageId();
}
