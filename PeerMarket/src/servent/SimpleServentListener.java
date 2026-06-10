package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.AppConfig;
import app.Cancellable;
import servent.handler.AddItemBackupHandler;
import servent.handler.AddItemHandler;
import servent.handler.AddItemOkHandler;
import servent.handler.BuyCommitHandler;
import servent.handler.BuyReplyHandler;
import servent.handler.MessageHandler;
import servent.handler.MutexReplyHandler;
import servent.handler.MutexRequestHandler;
import servent.handler.NewNodeHandler;
import servent.handler.NodeFailedHandler;
import servent.handler.NodeLeftHandler;
import servent.handler.NotifyHandler;
import servent.handler.NullHandler;
import servent.handler.PingHandler;
import servent.handler.PongHandler;
import servent.handler.SearchQueryHandler;
import servent.handler.SearchReplyHandler;
import servent.handler.SorryHandler;
import servent.handler.SubscribeHandler;
import servent.handler.SuspectCheckHandler;
import servent.handler.SuspectReplyHandler;
import servent.handler.UpdateHandler;
import servent.handler.WelcomeHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;

	private final ExecutorService threadPool = Executors.newWorkStealingPool();

	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on "
					+ AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}

		while (working) {
			try {
				Socket clientSocket = listenerSocket.accept();
				Message clientMessage = MessageUtil.readMessage(clientSocket);
				if (clientMessage == null) {
					continue;
				}

				if (clientMessage.getMessageType() != MessageType.POISON) {
					AppConfig.failureDetector.markSeen(clientMessage.getSenderPort());
				}

				MessageHandler handler;
				switch (clientMessage.getMessageType()) {
				case NEW_NODE:        handler = new NewNodeHandler(clientMessage); break;
				case WELCOME:         handler = new WelcomeHandler(clientMessage); break;
				case SORRY:           handler = new SorryHandler(clientMessage); break;
				case UPDATE:          handler = new UpdateHandler(clientMessage); break;

				case PING:            handler = new PingHandler(clientMessage); break;
				case PONG:            handler = new PongHandler(clientMessage); break;
				case SUSPECT_CHECK:   handler = new SuspectCheckHandler(clientMessage); break;
				case SUSPECT_REPLY:   handler = new SuspectReplyHandler(clientMessage); break;
				case NODE_FAILED:     handler = new NodeFailedHandler(clientMessage); break;
				case NODE_LEFT:       handler = new NodeLeftHandler(clientMessage); break;

				case ADD_ITEM:        handler = new AddItemHandler(clientMessage); break;
				case ADD_ITEM_OK:     handler = new AddItemOkHandler(clientMessage); break;
				case ADD_ITEM_BACKUP: handler = new AddItemBackupHandler(clientMessage); break;
				case SEARCH_QUERY:    handler = new SearchQueryHandler(clientMessage); break;
				case SEARCH_REPLY:    handler = new SearchReplyHandler(clientMessage); break;
				case SUBSCRIBE:       handler = new SubscribeHandler(clientMessage); break;
				case NOTIFY:          handler = new NotifyHandler(clientMessage); break;

				case BUY_COMMIT:      handler = new BuyCommitHandler(clientMessage); break;
				case BUY_REPLY:       handler = new BuyReplyHandler(clientMessage); break;

				case MUTEX_REQUEST:   handler = new MutexRequestHandler(clientMessage); break;
				case MUTEX_REPLY:     handler = new MutexReplyHandler(clientMessage); break;

				case POISON:          continue;
				default:              handler = new NullHandler(clientMessage); break;
				}

				threadPool.submit(handler);
			} catch (SocketTimeoutException timeout) {

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			listenerSocket.close();
		} catch (IOException ignored) {
		}
		threadPool.shutdown();
		try {
			if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
			}
		} catch (InterruptedException e) {
			threadPool.shutdownNow();
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}
}
