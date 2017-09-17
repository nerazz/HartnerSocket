package hartnerserver;

/**
 * Created by niklas on 05.09.17.
 */
public class Rock {//TODO: schnellere und langsamere rocks
	private int timeUntilHit = 55;//TODO: aus initdaten berechnen
	private int id;
	private static int currentId = 0;
	private int speed;//TODO: benutzen

	Rock() {
		id = currentId++;
	}

	int getTimeUntilHit() {
		return timeUntilHit;
	}

	void fly() {
		timeUntilHit--;
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) return false;
		if (that == this) return true;
		if (!(that instanceof Rock)) return false;
		Rock o = (Rock) that;
		return o.id == this.id;
	}
}
