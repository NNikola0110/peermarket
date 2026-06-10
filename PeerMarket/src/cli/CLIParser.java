package cli;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import app.AppConfig;
import app.Cancellable;
import cli.command.BuyCommand;
import cli.command.CLICommand;
import cli.command.InfoCommand;
import cli.command.ListItemCommand;
import cli.command.PauseCommand;
import cli.command.SearchCommand;
import cli.command.StopCommand;
import cli.command.SubscribeCommand;
import servent.SimpleServentListener;

public class CLIParser implements Runnable, Cancellable {

	private volatile boolean working = true;
	private final List<CLICommand> commandList;

	public CLIParser(SimpleServentListener listener) {
		this.commandList = new ArrayList<>();
		commandList.add(new InfoCommand());
		commandList.add(new PauseCommand());
		commandList.add(new ListItemCommand());
		commandList.add(new SearchCommand());
		commandList.add(new SubscribeCommand());
		commandList.add(new BuyCommand());
		commandList.add(new StopCommand(this, listener));
	}

	@Override
	public void run() {
		Scanner sc = new Scanner(System.in);

		while (working) {
			if (!sc.hasNextLine()) {
				break;
			}
			String commandLine;
			try {
				commandLine = sc.nextLine();
			} catch (NoSuchElementException e) {
				break;
			}

			int spacePos = commandLine.indexOf(" ");
			String commandName;
			String commandArgs = null;
			if (spacePos != -1) {
				commandName = commandLine.substring(0, spacePos);
				commandArgs = commandLine.substring(spacePos + 1);
			} else {
				commandName = commandLine;
			}

			boolean found = false;
			for (CLICommand cliCommand : commandList) {
				if (cliCommand.commandName().equals(commandName)) {
					cliCommand.execute(commandArgs);
					found = true;
					break;
				}
			}
			if (!found && !commandName.trim().isEmpty()) {
				AppConfig.timestampedErrorPrint("Unknown command: " + commandName);
			}
		}

		sc.close();
	}

	@Override
	public void stop() {
		this.working = false;
	}
}
