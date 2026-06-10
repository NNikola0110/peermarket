package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MultipleServentStarter {

	private static final String CLASSPATH = "out";
	private static final long NODE_START_SPACING_MS = 6000;

	private static class ServentCLI implements Runnable {
		private final List<Process> serventProcesses;
		private final Process bsProcess;

		public ServentCLI(List<Process> serventProcesses, Process bsProcess) {
			this.serventProcesses = serventProcesses;
			this.bsProcess = bsProcess;
		}

		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			while (true) {
				String line = sc.nextLine();
				if (line.equals("stop")) {
					for (Process process : serventProcesses) {
						hardKill(process);
					}
					hardKill(bsProcess);
					break;
				}
				if (line.startsWith("kill ")) {

					try {
						int idx = Integer.parseInt(line.substring(5).trim());
						if (idx >= 0 && idx < serventProcesses.size()) {
							hardKill(serventProcesses.get(idx));
							System.out.println("Killed servent " + idx);
						}
					} catch (NumberFormatException e) {
						System.out.println("Usage: kill <index>");
					}
				}
			}
			sc.close();
		}

		private void hardKill(Process process) {
			process.descendants().forEach(ProcessHandle::destroyForcibly);
			process.destroyForcibly();
		}
	}

	private static void startServentTest(String testName) {
		List<Process> serventProcesses = new ArrayList<>();

		AppConfig.readConfig(testName + "/servent_list.properties", 0);

		AppConfig.timestampedStandardPrint("Starting multiple servent runner. "
				+ "Type \"kill <i>\" to crash a node, or \"stop\" to halt everything.");

		Process bsProcess = null;
		ProcessBuilder bsBuilder = new ProcessBuilder("java", "-cp", CLASSPATH,
				"app.BootstrapServer", String.valueOf(AppConfig.BOOTSTRAP_PORT));
		try {
			bsProcess = bsBuilder.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		int serventCount = AppConfig.SERVENT_COUNT;
		for (int i = 0; i < serventCount; i++) {
			try {
				ProcessBuilder builder = new ProcessBuilder("java", "-cp", CLASSPATH,
						"app.ServentMain", testName + "/servent_list.properties", String.valueOf(i));
				builder.redirectOutput(new File(testName + "/output/servent" + i + "_out.txt"));
				builder.redirectError(new File(testName + "/error/servent" + i + "_err.txt"));
				builder.redirectInput(new File(testName + "/input/servent" + i + "_in.txt"));
				Process p = builder.start();
				serventProcesses.add(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(NODE_START_SPACING_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Thread t = new Thread(new ServentCLI(serventProcesses, bsProcess));
		t.start();

		for (Process process : serventProcesses) {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		AppConfig.timestampedStandardPrint("All servent processes finished. Type \"stop\" to halt bootstrap.");
		try {
			if (bsProcess != null) {
				bsProcess.waitFor();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String testName = args.length > 0 ? args[0] : "peermarket";
		startServentTest(testName);
	}
}
