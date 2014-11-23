package hu.bme.tmit.deakandras.crowdparking.listener;

import com.nutiteq.components.MapPos;
import com.nutiteq.geometry.VectorElement;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.services.routing.RouteActivity;
import com.nutiteq.ui.MapListener;

public class MapEventListener extends MapListener {

	private RouteActivity activity;
    private MapPos startPos;
    private MapPos stopPos;
	
	public MapEventListener(RouteActivity activity) {
		this.activity = activity;
	}

	@Override
	public void onLabelClicked(VectorElement arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onMapClicked(double x, double y, boolean longClick) {
		if(startPos == null){
		    // set start, or start again
		    startPos = (new EPSG3857()).toWgs84(x,y);
		    activity.setStartMarker(new MapPos(x,y));
		}else if(stopPos == null){
		    // set stop and calculate
		    stopPos = (new EPSG3857()).toWgs84(x,y);
		    activity.setStopMarker(new MapPos(x,y));
	        activity.showRoute(startPos.y, startPos.x, stopPos.y, stopPos.x);
		 
	        // restart to force new route next time
	        startPos = null;
	        stopPos = null;
		}
	}

	@Override
	public void onMapMoved() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVectorElementClicked(VectorElement element, double x,
			double y, boolean longClick) {
		onMapClicked(x, y, longClick);

	}

}
