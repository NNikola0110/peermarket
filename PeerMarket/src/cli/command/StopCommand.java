package cli.command;

import app.AppConfig;
import app.ServentInfo;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class StopCommand implements CLICommand {

	private final CLIParser parser;
	private final SimpleServentListener listener;

	public StopCommand(CLIParser parser, SimpleServentListener listener) {
		this.parser = parser;
		this.listener = listener;
	}

	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping gracefully...");

		int myPort = AppConfig.myServentInfo.getListenerPort();
		for (ServentInfo si : AppConfig.chordState.getAllNodeInfo()) {
			if (si.getListenerPort() != myPort) {
				MessageUtil.sendMessage(new BasicMessage(MessageType.NODE_LEFT, myPort,
						si.getListenerPort(), String.valueOf(myPort)));
			}
		}

		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		AppConfig.failureDetector.stop();
		listener.stop();
		parser.stop();
	}
}
