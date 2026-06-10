package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BootstrapServer {

	private volatile boolean working = true;
	private final List<Integer> activeServents;

	private class CLIWorker implements Runnable {
		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			while (true) {
				String line = sc.nextLine();
				if (line.equals("stop")) {
					working = false;
					break;
				}
			}
			sc.close();
		}
	}

	public BootstrapServer() {
		activeServents = new ArrayList<>();
	}

	public void doBootstrap(int bsPort) {
		Thread cliThread = new Thread(new CLIWorker());
		cliThread.start();

		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(bsPort);
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e1) {
			AppConfig.timestampedErrorPrint("Problem while opening listener socket.");
			System.exit(0);
		}

		Random rand = new Random(System.currentTimeMillis());

		while (working) {
			try {
				Socket newServentSocket = listenerSocket.accept();

				Scanner socketScanner = new Scanner(newServentSocket.getInputStream());
				String message = socketScanner.nextLine();

				if (message.equals("Hail")) {
					int newServentPort = socketScanner.nextInt();
					System.out.println("Hail from " + newServentPort);
					PrintWriter socketWriter = new PrintWriter(newServentSocket.getOutputStream());
					if (activeServents.size() == 0) {
						socketWriter.write(String.valueOf(-1) + "\n");
						activeServents.add(newServentPort);
					} else {
						int randServent = activeServents.get(rand.nextInt(activeServents.size()));
						socketWriter.write(String.valueOf(randServent) + "\n");
					}
					socketWriter.flush();
					newServentSocket.close();
				} else if (message.equals("New")) {
					int newServentPort = socketScanner.nextInt();
					System.out.println("Adding " + newServentPort);
					activeServents.add(newServentPort);
					newServentSocket.close();
				}
			} catch (SocketTimeoutException e) {

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			AppConfig.timestampedErrorPrint("Bootstrap started without port argument.");
		}
		int bsPort = 0;
		try {
			bsPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Bootstrap port not valid: " + args[0]);
			System.exit(0);
		}
		AppConfig.timestampedStandardPrint("Bootstrap server started on port: " + bsPort);
		BootstrapServer bs = new BootstrapServer();
		bs.doBootstrap(bsPort);
	}
}
