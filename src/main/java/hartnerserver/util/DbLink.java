package hartnerserver.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by niklas on 06.09.17.
 */
public class DbLink {
	private static final HikariDataSource DATA_SOURCE;

	static {
		HikariConfig config = new HikariConfig("/hikari.properties");
		DATA_SOURCE = new HikariDataSource(config);
	}

	private DbLink() {}

	public static void changePlayers(int lobbyId, String modification) {
		String query;
		if (modification.toLowerCase().equals("inc")) {
			query = "UPDATE Hartner SET players = players + 1 WHERE id = ?";

		} else if (modification.toLowerCase().equals("dec")) {
			query = "UPDATE Hartner SET players = players - 1 WHERE id = ?";
		} else {
			System.out.println("error in changePlayers!");
			return;
		}
		try (Connection link = DATA_SOURCE.getConnection(); PreparedStatement ps = link.prepareStatement(query)) {
			ps.setInt(1, lobbyId);
			ps.executeUpdate();
			link.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int getLobbySize(int lobbyId) {
		String query = "SELECT maxPlayers FROM Hartner WHERE id = ?";
		try (Connection link = DATA_SOURCE.getConnection(); PreparedStatement ps = link.prepareStatement(query)) {
			ps.setInt(1, lobbyId);
			ps.executeQuery();
			ResultSet rs = ps.getResultSet();
			if (!rs.next()) {
				System.out.println("ERROR, lobbyId nicht gefunden!");
				return -1;
			}
			return rs.getInt("maxPlayers");

		} catch(SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
