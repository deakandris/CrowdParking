package hu.bme.tmit.deakandras.crowdparking.pathfinding;

import android.content.Context;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;

public class MyGraphHopper extends GraphHopper {
	
	Context context;
	double endLat;
	double endLon;
	
	public MyGraphHopper(Context context) {
		super();
		this.context = context;
	}
	
	@Override
	public MyGraphHopper forMobile() {
		super.forMobile();
		return this;
	}
	
	@Override
	protected Weighting createWeighting(String weighting, FlagEncoder encoder) {
		return new MyWeighting(getGraph(), encoder, endLat, endLon, context);
	}
	
	public void setDestination(double lat, double lon) {
		endLat = lat;
		endLon = lon;
	}
}
