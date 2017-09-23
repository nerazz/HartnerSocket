package hartnerserver;

import com.google.gson.Gson;
import hartnerserver.enums.GameState;
import hartnerserver.enums.PlayerEvent;
import hartnerserver.enums.PlayerState;
import hartnerserver.enums.ServerEvent;
import hartnerserver.jsonobj.ServerPlayData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by niklas on 05.09.17.
 */
public class Timer implements Runnable {//TODO: umbenennen; fields aufräumen
	private static final Gson GSON = new Gson();
	private final Lobby LOBBY;
	//private final List<Player> PLAYER_LIST;//TODO: list benutzen?
	private Player[] players;//TODO: von lobby getten?
	private final List<Rock> ROCKS = new Vector<>();//TODO: ringbuffer
	private int count = 0;
	private static final int DIVIDER = 20;
	private final List<ServerEvent> SERVER_EVENTS = new ArrayList<>();
	private final List<List<PlayerEvent>> PLAYER_EVENTS = new ArrayList<>();//TODO: besser (mit eigener klasse!)
	//TODO: player-flag: modified: wenn mit dem player was passiert ist -> true damit server weiß, dass er update schicken muss

	Timer(Lobby lobby) {//TODO: besser
		//PLAYER_LIST = lobby.getPlayers();

		LOBBY = lobby;
	}

	void init() {
		players = new Player[LOBBY.getPlayers().size()];
		LOBBY.getPlayers().toArray(players);
		for (int i = 0; i < players.length; i++) {
			PLAYER_EVENTS.add(new ArrayList<>());
		}
	}


	@Override
	//@SuppressWarnings("unchecked")
	public void run() {
		try {
			for (Player player : players) {
				if (player.getPlayerState() == PlayerState.LOST) {
					continue;
				}
				if (player.isHard()) {
					player.subLife(5);
				}
			}

			Rock rock;
			for (Iterator<Rock> it = ROCKS.iterator(); it.hasNext();) {//TODO: benchmarken; timer kommt mir komisch vor
				rock = it.next();
				if (rock.checkHit()) {
					for (Player player : players) {
						if (player.getPlayerState() == PlayerState.LOST) {
							continue;
						}
						if (!player.isHard()) {
							player.subLife(100);
							PLAYER_EVENTS.get(player.getSlot()).add(PlayerEvent.HIT);//FIXME: für player0
						} else {
							PLAYER_EVENTS.get(player.getSlot()).add(PlayerEvent.BLOCK);
						}
					}
					it.remove();
				}
			}

			Long spawnHit = 0L;//TODO: besser
			if (count % DIVIDER == 0) {
				if (Math.random() > 0.7) {//TODO: use nextInt()
					rock = new Rock();
					ROCKS.add(rock);
					SERVER_EVENTS.add(ServerEvent.SPAWN);
					spawnHit = rock.getHIT_TIME();
				}
			}

			ServerPlayData spd = new ServerPlayData();//TODO: in lobby
			spd.players = new ServerPlayData.Player[players.length];
			for (int i = 0; i < players.length; i++) {
				if (!players[i].isModified()) {
					spd.players[i] = null;
					continue;
				}
				players[i].setModified(false);
				spd.players[i] = new ServerPlayData.Player();
				spd.players[i].life = players[i].getLife();
				spd.players[i].hard = players[i].isHard();

				PlayerEvent[] playerEvents = new PlayerEvent[PLAYER_EVENTS.get(i).size()];//TODO: besser
				spd.players[i].events = PLAYER_EVENTS.get(i).toArray(playerEvents);
			}

			ServerPlayData.InServerEvent[] inSE = new ServerPlayData.InServerEvent[SERVER_EVENTS.size()];
			for (int i = 0; i < SERVER_EVENTS.size(); i++) {
				inSE[i] = new ServerPlayData.InServerEvent();
				inSE[i].event = SERVER_EVENTS.get(i);
				inSE[i].info = spawnHit;//TODO: besser (siehe spawnhit)
			}
			spd.events = inSE;

			LOBBY.sendToPlayers(GSON.toJson(spd));

			for (List<PlayerEvent> list : PLAYER_EVENTS) {
				list.clear();
			}
			SERVER_EVENTS.clear();
			if (LOBBY.getCurrentGameState() == GameState.END) {
				LOBBY.endGame();
			}

			count++;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
