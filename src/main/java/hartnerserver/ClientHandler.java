package hartnerserver;

import hartnerserver.util.DbLink;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Created by niklas on 04.09.17.
 */

@WebSocket
public class ClientHandler {
	private final Player PLAYER = new Player();

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
		LobbyHandler.INSTANCE.getLobby(PLAYER.getLobbyId()).leave(PLAYER);//TODO: besser
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		System.out.println("OnWebSocketError: " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("Connect: " + session.getRemoteAddress().getAddress());
		PLAYER.setSession(session);
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		PLAYER.got(message);
	}
}