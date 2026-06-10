package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import servent.message.NewNodeMessage;
import servent.message.util.MessageUtil;

public class ServentInitializer implements Runnable {

	private int getSomeServentPort() {
		int retVal = -2;
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("Hail\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			bsWriter.flush();
			Scanner bsScanner = new Scanner(bsSocket.getInputStream());
			retVal = bsScanner.nextInt();
			bsSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retVal;
	}

	@Override
	public void run() {
		int someServentPort = getSomeServentPort();

		if (someServentPort == -2) {
			AppConfig.timestampedErrorPrint("Error contacting bootstrap. Exiting...");
			System.exit(0);
		}
		if (someServentPort == -1) {
			AppConfig.timestampedStandardPrint("First node in the Chord system.");
			AppConfig.JOINED = true;
			AppConfig.failureDetector.start();
		} else {
			NewNodeMessage nnm = new NewNodeMessage(AppConfig.myServentInfo.getListenerPort(), someServentPort);
			MessageUtil.sendMessage(nnm);
		}
	}
}
