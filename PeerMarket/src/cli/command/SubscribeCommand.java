package cli.command;

import app.AppConfig;
import servent.message.BasicMessage;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SubscribeCommand implements CLICommand {

	@Override
	public String commandName() {
		return "subscribe";
	}

	@Override
	public void execute(String args) {
		if (args == null || args.trim().isEmpty()) {
			AppConfig.timestampedErrorPrint("Usage: subscribe [ip:port]");
			return;
		}
		String target = args.trim();
		int sellerPort;
		try {

			String portPart = target.contains(":") ? target.substring(target.indexOf(":") + 1) : target;
			sellerPort = Integer.parseInt(portPart.trim());
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("subscribe expects ip:port (e.g. localhost:1200)");
			return;
		}
		int myPort = AppConfig.myServentInfo.getListenerPort();
		MessageUtil.sendMessage(new BasicMessage(MessageType.SUBSCRIBE, myPort, sellerPort, ""));
		AppConfig.timestampedStandardPrint("Subscribed to seller on port " + sellerPort);
	}
}
