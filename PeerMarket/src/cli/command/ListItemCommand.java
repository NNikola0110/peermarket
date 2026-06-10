package cli.command;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import market.Item;
import servent.message.BasicMessage;
import servent.message.ItemMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class ListItemCommand implements CLICommand {

	@Override
	public String commandName() {
		return "list_item";
	}

	@Override
	public void execute(String args) {
		if (!AppConfig.JOINED) {
			AppConfig.timestampedErrorPrint("Not joined yet; cannot list_item.");
			return;
		}
		String[] toks = args == null ? new String[0] : args.trim().split("\\s+");
		if (toks.length < 3) {
			AppConfig.timestampedErrorPrint("Usage: list_item [item_id] [naziv] [kolicina]");
			return;
		}
		int itemId, qty;
		try {
			itemId = Integer.parseInt(toks[0]);
			qty = Integer.parseInt(toks[toks.length - 1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("item_id and kolicina must be integers.");
			return;
		}
		StringBuilder nameBuilder = new StringBuilder();
		for (int i = 1; i < toks.length - 1; i++) {
			if (i > 1) {
				nameBuilder.append(" ");
			}
			nameBuilder.append(toks[i]);
		}
		String name = nameBuilder.toString();

		int routingKey = ChordState.chordHash(itemId);
		int myPort = AppConfig.myServentInfo.getListenerPort();
		int myChordId = AppConfig.myServentInfo.getChordId();
		Item item = new Item(itemId, name, qty, myChordId, myChordId, true, 1);

		if (AppConfig.chordState.isKeyMine(routingKey)) {
			AppConfig.marketState.addPrimary(item);
			MessageUtil.replicate(item);
			AppConfig.tagPrint("[MARKET-LIST] item_id:" + itemId + " name:\"" + name + "\" qty:" + qty);
			for (int subscriberPort : AppConfig.marketState.getSubscribersSnapshot()) {
				MessageUtil.sendMessage(new BasicMessage(MessageType.NOTIFY,
						myPort, subscriberPort, myChordId + "," + itemId));
			}
		} else {
			ServentInfo responsible = AppConfig.chordState.getNextNodeForKey(routingKey);
			MessageUtil.sendMessage(new ItemMessage(MessageType.ADD_ITEM,
					myPort, responsible.getListenerPort(), item, routingKey, myPort));

		}
	}
}
