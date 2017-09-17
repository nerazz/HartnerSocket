package hartnerserver;

import com.google.gson.Gson;
import hartnerserver.enums.GameState;
import hartnerserver.jsonobj.GameStateChanger;
import hartnerserver.jsonobj.ServerInitData;
import hartnerserver.jsonobj.ServerLobbyData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by niklas on 06.09.17.
 */
public class Lobby {
	private static final int TICK_RATE = 32;
	private static final Gson GSON = new Gson();

	private GameState currentGameState = GameState.INIT;//direkt LOBBY?

	private final transient int SIZE;//TODO: überhaupt notwendig?
	private final List<Player> PLAYER_LIST = Collections.synchronizedList(new ArrayList<Player>());
	private final transient Timer TIMER;

	Lobby(int size) {
		SIZE = size;
		TIMER = new Timer(this);
	}

	synchronized void join(Player player) {//checken, ob noch slot frei -> update DB on success
		int slot = PLAYER_LIST.size();//erster freier slot
		if (slot > SIZE) {//lobby schon voll
			slot = -1;
			ServerInitData sid = new ServerInitData();
			sid.slot = slot;
			player.send(GSON.toJson(sid));
			GameStateChanger gsc = new GameStateChanger();
			gsc.changeState = GameState.ERROR;
			player.send(GSON.toJson(gsc));//TODO: gut so oder vorher mitschicken oder pur clientseitig?
			return;
		}
		PLAYER_LIST.add(player);
		player.setSlot(slot);
		ServerInitData sid = new ServerInitData();
		sid.slot = slot;
		player.send(GSON.toJson(sid));
		GameStateChanger gsc = new GameStateChanger();
		gsc.changeState = GameState.LOBBY;
		player.send(GSON.toJson(gsc));//TODO: gut so oder vorher mitschicken oder pur clientseitig?
		player.changeGameState(GameState.LOBBY);
		sendToPlayers(getLobbyData());
	}

	void leave(Player player) {
		PLAYER_LIST.remove(player);
		sendToPlayers(getLobbyData());
	}

	List<Player> getPlayers() {
		return PLAYER_LIST;
	}

	public void start() {//TODO:
		boolean allReady = true;
		for (Player player : PLAYER_LIST) {
			if (!player.isReady()) {
				allReady = false;
			}
		}
		if (allReady) {
			GameStateChanger gsc = new GameStateChanger();
			gsc.changeState = GameState.PLAY;
			sendToPlayers(GSON.toJson(gsc));
			for (Player p : PLAYER_LIST) {
				p.changeGameState(GameState.PLAY);
			}
			TIMER.init();
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(TIMER, 3000, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);
		}
	}

	public String getLobbyData() {
		ServerLobbyData sld = new ServerLobbyData();
		ServerLobbyData.Player[] players = new ServerLobbyData.Player[PLAYER_LIST.size()];

		Player player;
		for (int i = 0; i < PLAYER_LIST.size(); i++) {
			players[i] = new ServerLobbyData.Player();
			player = PLAYER_LIST.get(i);
			players[i].NAME = player.getName();
			players[i].ready = player.isReady();
		}
		sld.players = players;
		return GSON.toJson(sld);
	}

	public void sendToPlayers(String message) {//TODO: nur objekt übergeben und hier mit gson parsen?
		for (Player player : PLAYER_LIST) {
			player.send(message);
		}
	}
}
