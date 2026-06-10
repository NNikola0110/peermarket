package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import failure.FailureDetector;
import market.MarketState;
import mutex.RicartAgrawalaMutex;

public class AppConfig {

	public static ServentInfo myServentInfo;

	public static int BOOTSTRAP_PORT;
	public static int SERVENT_COUNT;

	public static long WEAK_LIMIT;
	public static long STRONG_LIMIT;

	public static ChordState chordState;
	public static MarketState marketState;
	public static RicartAgrawalaMutex ricartAgrawalaMutex;
	public static FailureDetector failureDetector;

	public static volatile boolean JOINED = false;

	private static final Object TAG_LOCK = new Object();
	private static String lastNeighbors = null;

	public static void timestampedStandardPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		System.out.println(timeFormat.format(new Date()) + " - " + message);
	}

	public static void timestampedErrorPrint(String message) {
		DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
		System.err.println(timeFormat.format(new Date()) + " - " + message);
	}

	public static void tagPrint(String message) {
		synchronized (TAG_LOCK) {
			System.out.println(message);
		}
	}

	public static synchronized void announceNeighborsIfChanged() {
		List<Integer> ids = chordState.getNeighborChordIds();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(ids.get(i));
		}
		String neighbors = sb.toString();
		if (neighbors.isEmpty()) {

			lastNeighbors = neighbors;
			return;
		}
		if (!neighbors.equals(lastNeighbors)) {
			lastNeighbors = neighbors;
			tagPrint("[SYS-NEIGHBORS] my_id:" + myServentInfo.getChordId() + " neighbors:" + neighbors);
		}
	}

	public static void readConfig(String configName, int serventId) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(configName)));
		} catch (IOException e) {
			timestampedErrorPrint("Couldn't open properties file. Exiting...");
			System.exit(0);
		}

		try {
			BOOTSTRAP_PORT = Integer.parseInt(properties.getProperty("bs.port"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading bs.port. Exiting...");
			System.exit(0);
		}

		try {
			SERVENT_COUNT = Integer.parseInt(properties.getProperty("servent_count"));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading servent_count. Exiting...");
			System.exit(0);
		}

		try {
			ChordState.CHORD_SIZE = Integer.parseInt(properties.getProperty("chord_size"));
			chordState = new ChordState();
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading chord_size (must be a power of 2). Exiting...");
			System.exit(0);
		}

		WEAK_LIMIT = Long.parseLong(properties.getProperty("weak_limit", "4000"));
		STRONG_LIMIT = Long.parseLong(properties.getProperty("strong_limit", "10000"));

		String portProperty = "servent" + serventId + ".port";
		int serventPort = -1;
		try {
			serventPort = Integer.parseInt(properties.getProperty(portProperty));
		} catch (NumberFormatException e) {
			timestampedErrorPrint("Problem reading " + portProperty + ". Exiting...");
			System.exit(0);
		}

		myServentInfo = new ServentInfo("localhost", serventPort);

		marketState = new MarketState();
		ricartAgrawalaMutex = new RicartAgrawalaMutex();
		failureDetector = new FailureDetector();
	}
}
