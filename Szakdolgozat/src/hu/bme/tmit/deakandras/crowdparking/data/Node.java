package hu.bme.tmit.deakandras.crowdparking.data;

import com.nutiteq.components.MapPos;

public class Node {
	public final float lat;
	public final float lon;
	public final long id;
	
	public Node(long id, float lat, float lon) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
	}
	
	public Node(long id, MapPos pos) {
		this.id = id;
		lon = (float) pos.x;
		lat = (float) pos.y;
	}
}
