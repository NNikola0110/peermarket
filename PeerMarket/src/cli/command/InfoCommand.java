package cli.command;

import market.Item;
import app.AppConfig;

public class InfoCommand implements CLICommand {

	@Override
	public String commandName() {
		return "info";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("My info: " + AppConfig.myServentInfo
				+ " predecessor=" + AppConfig.chordState.getPredecessor()
				+ " neighbors=" + AppConfig.chordState.getNeighborChordIds());
		for (Item item : AppConfig.marketState.allItemsSnapshot()) {
			AppConfig.timestampedStandardPrint("  " + (item.isPrimary() ? "PRIMARY" : "backup ") + " " + item);
		}
	}
}
