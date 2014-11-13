package hu.bme.tmit.deakandras.crowdparking.data;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.components.MapPos;

public class Way {
	public List<Node> nodes;
	public float occupancy;
	public final long id;
	
	public List<MapPos> getNodesAsMapPos() {
		List<MapPos> mapPosList = new ArrayList<MapPos>();
		for(Node node : nodes) {
			mapPosList.add(new MapPos(node.lon, node.lat));
		}
		return mapPosList;
	}

	public Way(long id) {
		this.id = id;
		nodes = new ArrayList<Node>();
		occupancy = 0;
	}
	
	public Way(long id, ArrayList<Node> nodes) {
		this.id = id;
		this.nodes = nodes;
		occupancy = 0;
	}
	
	public Way(long id, ArrayList<Node> nodes, float occupancy) {
		this.id = id;
		this.nodes = nodes;
		this.occupancy = occupancy;
	}

	@Override
	public String toString() {
		return occupancy + "% " + nodes.toString();
	}
}
