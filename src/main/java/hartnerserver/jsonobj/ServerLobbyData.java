package hartnerserver.jsonobj;

/**
 * Created by niklas on 07.09.17.
 */
public class ServerLobbyData {
	public Player[] players;

	public static class Player {
		public String NAME;
		public boolean ready;
	}
}
