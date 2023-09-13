import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient implements Runnable {

	private Socket client;
	private BufferedReader inReader;
	private PrintWriter outWriter;
	private boolean isDone;

	@Override
	public void run() {
		try {
			client = new Socket("127.0.0.1", 15001);
			outWriter = new PrintWriter(client.getOutputStream(), true);
			inReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			InputHandler inputHandler = new InputHandler();
			Thread thread = new Thread(inputHandler);
			thread.start();
			
			String inString;
			while ((inString = inReader.readLine()) != null) {
				System.out.println(inString);
			}
		} catch (Exception e) {
			shutdown();
		}
	}
	
	public void shutdown() {
		isDone = true;
		try {
			inReader.close();
			outWriter.close();
			if (!client.isClosed()) {
				client.close();
			}
		} catch (IOException e) {
			// TODO: handle exception
		}
	}

	class InputHandler implements Runnable {

		@Override
		public void run() {
			try {
				BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
				while (!isDone) {
					String messageString = inReader.readLine();
					if (messageString.equals("/quit")) {
						outWriter.println("/quit");
						inReader.close();
						shutdown();
					} else {
						outWriter.println(messageString);
					}
				}
			} catch (IOException e) {
				shutdown();
			}
		}
	}
	
	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		chatClient.run();
	}
}
