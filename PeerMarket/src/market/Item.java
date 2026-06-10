package market;

import java.io.Serializable;

public class Item implements Serializable {

	private static final long serialVersionUID = 7711992031337006001L;

	private final int itemId;
	private final String name;
	private int quantity;
	private final int creatorChordId;
	private int ownerChordId;
	private boolean primary;
	private long version;

	public Item(int itemId, String name, int quantity, int creatorChordId, int ownerChordId,
			boolean primary, long version) {
		this.itemId = itemId;
		this.name = name;
		this.quantity = quantity;
		this.creatorChordId = creatorChordId;
		this.ownerChordId = ownerChordId;
		this.primary = primary;
		this.version = version;
	}

	public int getItemId() {
		return itemId;
	}

	public String getName() {
		return name;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getCreatorChordId() {
		return creatorChordId;
	}

	public int getOwnerChordId() {
		return ownerChordId;
	}

	public void setOwnerChordId(int ownerChordId) {
		this.ownerChordId = ownerChordId;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public Item copy() {
		return new Item(itemId, name, quantity, creatorChordId, ownerChordId, primary, version);
	}

	@Override
	public String toString() {
		return "Item{id=" + itemId + ", name='" + name + "', qty=" + quantity +
				", creator=" + creatorChordId + ", owner=" + ownerChordId +
				", primary=" + primary + ", v=" + version + "}";
	}
}
