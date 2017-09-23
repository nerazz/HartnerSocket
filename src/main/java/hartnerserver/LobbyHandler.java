package hartnerserver;

import hartnerserver.util.DbLink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by niklas on 06.09.17.
 */

public enum LobbyHandler {
	INSTANCE;

	private final Map<Integer, Lobby> LOBBY_LIST = new ConcurrentHashMap<>();

	public Lobby getLobby(Integer id) {
		return LOBBY_LIST.get(id);
	}

	public Lobby createLobby(Integer id, int size) {
		Lobby lobby = new Lobby(id, size);
		System.out.println("created new lobby ID: " + id);
		LOBBY_LIST.put(id, lobby);
		return lobby;
	}

	public void closeLobby(Integer id) {//TODO:
		//Lobby lobby = LOBBY_LIST.get(id);
		//lobby.stopTimer();//stoppt lobby ihren eigenen timer automatisch?
		LOBBY_LIST.remove(id);
		DbLink.closeLobby(id);
	}

}
