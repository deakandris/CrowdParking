package hu.bme.tmit.deakandras.crowdparking.activity.data;

import java.util.ArrayList;
import java.util.List;

import com.nutiteq.components.MapPos;

public class Road {
	List<MapPos> nodes;
	float occupancy;
	
	public List<MapPos> getNodes() {
		return nodes;
	}

	public void setNodes(List<MapPos> nodes) {
		this.nodes = nodes;
	}

	public float getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(float occupancy) {
		if(occupancy > 100) {
			this.occupancy = 100;
		} else if (occupancy < 0) {
			this.occupancy = 0;
		} else {
			this.occupancy = occupancy;
		}
	}

	public Road() {
		nodes = new ArrayList<MapPos>();
		occupancy = 0;
	}

	public Road(ArrayList<MapPos> nodes, float occupancy) {
		this.nodes = nodes;
		this.occupancy = occupancy;
	}
	
	public void addNode(MapPos node) {
		nodes.add(node);
	}

	@Override
	public String toString() {
		return occupancy + "% " + nodes.toString();
	}
}
