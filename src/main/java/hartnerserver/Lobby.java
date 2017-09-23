package hartnerserver;

import com.google.gson.Gson;
import hartnerserver.enums.GameState;
import hartnerserver.jsonobj.GameStateChanger;
import hartnerserver.jsonobj.ServerEndData;
import hartnerserver.jsonobj.ServerInitData;
import hartnerserver.jsonobj.ServerLobbyData;
import hartnerserver.util.DbLink;

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
	private static final long START_DELAY = 4000;
	private static final Gson GSON = new Gson();

	private GameState currentGameState = GameState.INIT;//direkt LOBBY?

	private final int ID;
	private final transient int SIZE;//TODO: überhaupt notwendig?
	private final List<Player> PLAYER_LIST = Collections.synchronizedList(new ArrayList<Player>());
	private final transient Timer TIMER;
	private final transient ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

	private int nextPlace;
	private int deadCount = 0;

	Lobby(int id, int size) {
		ID = id;
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
			player.close();
			return;
		}
		boolean dupId = false;
		for (Player p : PLAYER_LIST) {
			if (player.getId() == p.getId()) {
				dupId = true;
			}
		}
		if (dupId) {
			GameStateChanger gsc = new GameStateChanger();
			gsc.changeState = GameState.ERROR;
			player.send(GSON.toJson(gsc));//TODO: gut so oder vorher mitschicken oder pur clientseitig?
			player.close();
			return;
		}
		PLAYER_LIST.add(player);
		player.setSlot(slot);
		DbLink.changePlayers(ID, PLAYER_LIST.size());
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
		DbLink.changePlayers(ID, PLAYER_LIST.size());
		if (PLAYER_LIST.size() == 0) {
			LobbyHandler.INSTANCE.closeLobby(ID);
			return;
		}
		sendToPlayers(getLobbyData());
	}

	List<Player> getPlayers() {
		return PLAYER_LIST;
	}

	public void start() {//TODO:
		boolean allReady = true;
		for (Player player : PLAYER_LIST) {//ohne host
			if (player.getSlot() == 0) {
				continue;
			}
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
			nextPlace = PLAYER_LIST.size();
			EXECUTOR.scheduleAtFixedRate(TIMER, START_DELAY, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);
		}
	}

	public void setPlaceOfPlayer(Player player) {//sollte automatisch durch timer synchronisiert sein
		deadCount++;
		player.setPlace(nextPlace--);//TODO: nicht zu unwahrscheinlich, dass 2 player gleichzeitig sterben; sollten gleichen platz bekommen
		if (deadCount == PLAYER_LIST.size()) {
			currentGameState = GameState.END;
		}
	}

	public void endGame() {
		System.out.println("ending Game...");
		EXECUTOR.shutdown();
		ServerEndData sed = new ServerEndData();
		ServerEndData.Player[] players = new ServerEndData.Player[PLAYER_LIST.size()];
		sed.players = players;
		for (int i = 0; i < PLAYER_LIST.size(); i++) {
			players[i] = new ServerEndData.Player();
			players[i].name = PLAYER_LIST.get(i).getName();
			players[i].place = PLAYER_LIST.get(i).getPlace();
		}

		GameStateChanger gsc = new GameStateChanger();
		gsc.changeState = GameState.END;

		sendToPlayers(GSON.toJson(gsc));
		sendToPlayers(GSON.toJson(sed));

		LobbyHandler.INSTANCE.closeLobby(ID);//TODO: reicht das?

	}

	public GameState getCurrentGameState() {
		return currentGameState;
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
