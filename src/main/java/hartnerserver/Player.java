package hartnerserver;

import com.google.gson.Gson;
import hartnerserver.enums.GameState;
import hartnerserver.jsonobj.ClientInitData;
import hartnerserver.jsonobj.ClientLobbyData;
import hartnerserver.enums.PlayerState;
import hartnerserver.jsonobj.ClientPlayData;
import hartnerserver.jsonobj.GameStateChanger;
import hartnerserver.util.DbLink;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

/**
 * Created by niklas on 05.09.17.
 */
public class Player {//TODO: fields aufräumen
	private static final Gson GSON = new Gson();

	private int ID;
	private String NAME;
	private int LOBBY_ID;

	private Lobby lobby;

	private int slot;//final machen? (+bei constructor übergeben)
	private transient Session session;
	private transient RemoteEndpoint endpoint;
	private transient boolean ready;
	private transient volatile boolean modified = false;
	private transient GameState currentGameState = GameState.INIT;

	private int life = 1000;
	private boolean hard;
	private PlayerState gameState = PlayerState.PLAYING;

	private void init(int ID, String NAME, int LOBBY_ID) {
		this.ID = ID;
		this.NAME = NAME;
		this.LOBBY_ID = LOBBY_ID;
		//lobby = LobbyHandler.INSTANCE.getLobby(LOBBY_ID);TODO: hier initen?
	}

	public String getName() {
		return NAME;
	}

	public int getLobbyId() {
		return LOBBY_ID;
	}

	public int getSlot() {
		return slot;
	}

	public void setSlot(int slot) {
		this.slot = slot;
	}

	public int getLife() {
		return life;
	}

	public void subLife(int life) {
		this.life -= life;
		if (this.life <= 0) {
			gameState = PlayerState.LOST;
			System.out.println("Player " + slot + " lost!");
		}
		modified = true;
	}

	public boolean isHard() {
		return hard;
	}

	public PlayerState getGameState() {
		return gameState;
	}

	public void setSession(Session session) {
		this.session = session;
		endpoint = session.getRemote();
		System.out.println("session & endpoint set");
	}

	public void close() {
		session.close();
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public boolean isReady() {
		return ready;
	}

	public boolean isModified() {
		return modified;
	}

	public void send(String message) {
		try {
			endpoint.sendString(message);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void got(String message) {//TODO: umbenennen; geht bestimmt besser (nicht jedesmal neues gson, ...)

		if(message.equals("start")) {//TODO: anders
			if (slot == 0) {
				lobby.start();
			}
			return;
		}

		switch(currentGameState) {
			case INIT:
				evaluateInitData(message);
				break;

			case LOBBY:
				evaluateLobbyData(message);
				break;

			case PLAY:
				evaluatePlayData(message);
				break;
		}

		/*switch(message) {
			case "start":
				//TODO: start timer
				break;
			default:
				Gson gson = new Gson();
				hartnerserver.enums.PlayerState pState = gson.fromJson(message, hartnerserver.enums.PlayerState.class);
				hard = pState.hard();
				modified = true;
		}*/

	}

	@Override
	public String toString() {
		return "\"" + NAME + "\"";//TODO: in eigene toJson verlagern, aber auf null aufpassen!
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		if (that == this) return true;
		if (!(that instanceof Player)) return false;
		Player o = (Player)that;
		return o.ID == this.ID;
		/*ArrayTest o = (ArrayTest) obj;
		return o.i == this.i;*/
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public void changeGameState(GameState gameState) {
		System.out.println("changing GameState to " + gameState);
		currentGameState = gameState;
	}

	private void evaluateInitData(String data) {
		ClientInitData cid = GSON.fromJson(data, ClientInitData.class);
		init(cid.ID, cid.NAME, cid.LOBBY_ID);
		DbLink.changePlayers(getLobbyId(), "inc");

		LobbyHandler lobbyHandler = LobbyHandler.INSTANCE;
		Lobby lobby = lobbyHandler.getLobby(getLobbyId());
		if (lobby == null) {
			int lobbySize = DbLink.getLobbySize(getLobbyId());
			if (lobbySize < 0) {
				System.out.println("ERROR, lobbySize < 0!!");
				return;
			}
			lobby = lobbyHandler.createLobby(getLobbyId(), lobbySize);//TODO: besser
		}
		lobby.join(this);
		this.lobby = lobby;
	}

	private void evaluateLobbyData(String data) {
		ClientLobbyData cld = GSON.fromJson(data, ClientLobbyData.class);
		ready = cld.ready;

		this.lobby.sendToPlayers(this.lobby.getLobbyData());
	}

	private void evaluatePlayData(String data) {
		ClientPlayData cpd = GSON.fromJson(data, ClientPlayData.class);
		hard = cpd.hard;
		modified = true;
	}
}
