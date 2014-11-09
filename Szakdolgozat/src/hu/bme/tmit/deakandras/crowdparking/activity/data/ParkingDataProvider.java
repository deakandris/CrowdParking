package hu.bme.tmit.deakandras.crowdparking.activity.data;

import java.util.Date;
import java.util.List;

import com.nutiteq.components.MapPos;

public class ParkingDataProvider {
	
	public List<MapPos> getDirection(MapPos startPos, MapPos endPos,
			float maxWalk, float maxTime, float maxCost, float parkingTime) {
		// TODO: calculate route
		return null;
	};

	public int getNumberOfParkingSpots(long id){
		// TODO: query from server
		return 0;
	};
	
	public float getParkingDensity(long id){
		// TODO: query from server
		return 0;
	}
	
	public ParkingSpotHistory getParkingSpots(long id, Date startTime, Date endTime){
		// TODO: query from server
		return null;
	}
	
	public ParkingInfo getParkingPrice(long id){
		// TODO: query from server
		return null;
	}
}
