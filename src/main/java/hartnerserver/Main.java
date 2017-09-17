package hartnerserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Created by niklas on 04.09.17.
 */

public class Main {

	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);
		WebSocketHandler wsHandler = new WebSocketHandler() {
			@Override
			public void configure(WebSocketServletFactory factory) {
				factory.register(ClientHandler.class);
			}
		};
		server.setHandler(wsHandler);
		System.out.println("starting server");
		server.start();
		server.join();
	}
}
