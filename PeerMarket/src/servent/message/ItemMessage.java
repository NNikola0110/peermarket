package servent.message;

import market.Item;

public class ItemMessage extends BasicMessage {

	private static final long serialVersionUID = 1100229933774551001L;

	private final Item item;
	private final int routingKey;
	private final int originPort;

	public ItemMessage(MessageType type, int senderPort, int receiverPort,
			Item item, int routingKey, int originPort) {
		super(type, senderPort, receiverPort);
		this.item = item;
		this.routingKey = routingKey;
		this.originPort = originPort;
	}

	public Item getItem() {
		return item;
	}

	public int getRoutingKey() {
		return routingKey;
	}

	public int getOriginPort() {
		return originPort;
	}
}
