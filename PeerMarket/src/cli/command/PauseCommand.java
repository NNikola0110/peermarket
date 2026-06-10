package cli.command;

import app.AppConfig;

public class PauseCommand implements CLICommand {

	@Override
	public String commandName() {
		return "pause";
	}

	@Override
	public void execute(String args) {
		try {
			int timeToSleep = Integer.parseInt(args.trim());
			if (timeToSleep < 0) {
				throw new NumberFormatException();
			}
			Thread.sleep(timeToSleep);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("pause expects one non-negative int (ms).");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
