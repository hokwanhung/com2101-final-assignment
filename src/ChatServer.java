import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatServer implements Runnable {
	// Runnable means can be passed to a thread or thread pool,
	// and thus be executed concurrently (with other classes).

	private ArrayList<ConnectionHandler> connectionArrayList;
	private ServerSocket serverSocket;
	private boolean isDone;
	private ExecutorService poolService;

	// Thread pool is used for short-lived, yet multiple threads usage.

	public ChatServer() {
		connectionArrayList = new ArrayList<>();
		isDone = false;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(15001);
			poolService = Executors.newCachedThreadPool();

			// Create a ScheduledExecutorService to print the message every 15 seconds.
			ScheduledExecutorService sExecutorService = Executors.newScheduledThreadPool(1);
			final long startTime = System.currentTimeMillis();
			sExecutorService.scheduleWithFixedDelay(() -> {
				long currentTime = System.currentTimeMillis();
				long elapsedTime = (currentTime - startTime) / 1000;
				System.out.println("INFO: System running normal for " + elapsedTime + " seconds.");
			}, 0, 15, TimeUnit.SECONDS);

			while (!isDone) {
				// Continuously accept new connections until done.
				// Whenever a new client connection is accepted,
				Socket client = serverSocket.accept();
				ConnectionHandler connectionHandler = new ConnectionHandler(client);
				connectionArrayList.add(connectionHandler);
				poolService.execute(connectionHandler);
			}
		} catch (Exception e) {
			System.out.println("FATAL: " + e);
			shutdown();
		}

	}

	public void broadcast(String messageString) {
		// Send a message to all connected clients.
		for (ConnectionHandler cHandler : connectionArrayList) {
			if (cHandler != null) {
				cHandler.sendMessage(messageString);
			}
		}
	}

	public void shutdown() {
		try {
			System.out.println("WARN: A shutdown is requested.");
			isDone = true;
			
			if (serverSocket != null && !serverSocket.isClosed()) { // modified
				serverSocket.close();
				System.out.println("The server socket is closed.");
			}
			for (ConnectionHandler cHandler : connectionArrayList) {
				cHandler.shutdown();
			}
		} catch (IOException e) {
			System.out.println("FATAL: " + e);
		}

	}

	class ConnectionHandler implements Runnable {
		// Handle the client connection and then accept it.
		private Socket client;
		private BufferedReader inReader;
		private PrintWriter outWriter;

		private String nicknameString;

		public ConnectionHandler(Socket client) {
			this.client = client;
		}

		@Override
		public void run() {
			try {
				outWriter = new PrintWriter(client.getOutputStream(), true);
				inReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
				outWriter.println("Please enter a nickname: ");
				nicknameString = inReader.readLine();
				System.out.println("INFO: " + nicknameString + " connected!");
				broadcast(nicknameString + " joined the chat!");

				String messageString;
				while ((messageString = inReader.readLine()) != null) {
					// Whenever clients input something,
					if (messageString.startsWith("/nick ")) {
						String[] messageStrings = messageString.split(" ", 2);

						if (messageStrings.length == 2) {
							broadcast(nicknameString + " renamed themselves to " + messageStrings[1]);
							System.out.println("INFO: " + nicknameString + " renamed themselves to " + messageStrings[1]);
							nicknameString = messageStrings[1];
							outWriter.println("Successfully changed nickname to " + nicknameString);
						} else {
							outWriter.println("No nickname provided!");
						}
					} else if (messageString.startsWith("/quit")) {
						broadcast(nicknameString + " left the chat!");
						System.out.println("INFO: " + nicknameString + " left the chat.");
						shutdown();
						return; // modified: Prevent calling inReader again.
					} else {
						broadcast(nicknameString + ": " + messageString);
					}

				}
			} catch (IOException e) {
				System.out.println("FATAL: " + e);
				shutdown();
			}
		}

		public void sendMessage(String messageString) {
			outWriter.println(messageString);
		}

		public void shutdown() {
			try {
				inReader.close();
				System.out.println("TEst1");
				outWriter.close();
				System.out.println("TEst2");
				if (client != null && !client.isClosed()) {
					System.out.println("TEst3");
					client.close();
				}
			} catch (IOException e) {
				System.out.println("FATAL: " + e);
			}

		}
	}

	public static void main(String[] args) {
		ChatServer server = new ChatServer();
		server.run();
	}
}
