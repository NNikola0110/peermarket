package servent.message;

import java.util.List;

import market.Item;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private final List<Item> items;

	public WelcomeMessage(int senderPort, int receiverPort, List<Item> items) {
		super(MessageType.WELCOME, senderPort, receiverPort);
		this.items = items;
	}

	public List<Item> getItems() {
		return items;
	}
}
