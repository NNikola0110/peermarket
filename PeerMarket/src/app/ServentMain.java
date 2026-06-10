package app;

import cli.CLIParser;
import servent.SimpleServentListener;

public class ServentMain {

	public static void main(String[] args) {
		if (args.length != 2) {
			AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
			System.exit(0);
		}

		int serventId = -1;
		try {
			serventId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
			System.exit(0);
		}

		AppConfig.readConfig(args[0], serventId);

		int portNumber = AppConfig.myServentInfo.getListenerPort();
		if (portNumber < 1000 || portNumber > 2000) {
			AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
			System.exit(0);
		}

		AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);

		SimpleServentListener simpleListener = new SimpleServentListener();
		Thread listenerThread = new Thread(simpleListener);
		listenerThread.start();

		CLIParser cliParser = new CLIParser(simpleListener);
		Thread cliThread = new Thread(cliParser);
		cliThread.start();

		ServentInitializer serventInitializer = new ServentInitializer();
		Thread initializerThread = new Thread(serventInitializer);
		initializerThread.start();
	}
}
