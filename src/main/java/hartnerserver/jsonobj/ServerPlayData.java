package hartnerserver.jsonobj;

import hartnerserver.enums.PlayerEvent;
import hartnerserver.enums.ServerEvent;

/**
 * Created by niklas on 07.09.17.
 */
public class ServerPlayData {
	public Player[] players;
	public InServerEvent[] events;

	public static class Player {
		public int life;
		public boolean hard;
		public PlayerEvent[] events;
	}

	public static class InServerEvent {
		public ServerEvent event;
		public Object info;//TODO: wirklich nur Object?
	}
}
