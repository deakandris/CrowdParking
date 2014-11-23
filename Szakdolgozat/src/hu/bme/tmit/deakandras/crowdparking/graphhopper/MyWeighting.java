package hu.bme.tmit.deakandras.crowdparking.graphhopper;

import hu.bme.tmit.deakandras.crowdparking.activity.SettingsFragment;
import hu.bme.tmit.deakandras.crowdparking.database.DatabaseManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIteratorState;

public class MyWeighting implements Weighting {

	Graph graph;
	FlagEncoder encoder;
	Context context;
	DatabaseManager database;
	double walkDistance;
	double searchTime;
	double endLat;
	double endLon;

	public MyWeighting(Graph graph, FlagEncoder encoder, double endLat,
			double endLon, Context context) {
		this.graph = graph;
		this.encoder = encoder;
		this.endLat = endLat;
		this.endLon = endLon;
		this.context = context;
		database = new DatabaseManager(context);

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		walkDistance = Double.parseDouble(sharedPreferences.getString(
				SettingsFragment.KEY_MAX_WALK_DISTANCE_PREFERENCE, "0"));
		searchTime = Double.parseDouble(sharedPreferences.getString(
				SettingsFragment.KEY_MAX_SEARCH_TIME_PREFERENCE, "0"));
	}

	@Override
	public double calcWeight(EdgeIteratorState edgeState) {
		database.open(true);
		double result = edgeState.getDistance();

		double baseLat = graph.getLatitude(edgeState.getBaseNode());
		double baseLon = graph.getLongitude(edgeState.getBaseNode());
		double adjLat = graph.getLatitude(edgeState.getAdjNode());
		double adjLon = graph.getLongitude(edgeState.getAdjNode());

		long baseNodeId = database.getNodeId(baseLat, baseLon);
		long adjNodeId = database.getNodeId(adjLat, adjLon);
		long wayId = database.getWayId(baseNodeId, adjNodeId);

		double occupancy = database.getOccupancy(wayId);

		/*
		 * At this point we don't know the exact distance from the end point so
		 * we have to calculate an estimated distance. The straight line is
		 * obviously an underestimation so we go with right-angled distance
		 * which results in a good upper estimate, assuming that the majority of
		 * road turns take you closer to the destination and are lesser than 90°
		 */
		DistanceCalcEarth dce = new DistanceCalcEarth();
		double distanceFromEnd = dce.calcDist(baseLat, baseLon, endLat, endLon);
		distanceFromEnd *= Math.sqrt(2);

		/*
		 * If we are farther than max walk distance, stick with shortest route.
		 * Else, calculate modified weight.
		 */
		if (distanceFromEnd < walkDistance) {
			if (distanceFromEnd / encoder.getMaxSpeed() > searchTime) {
				result = Double.POSITIVE_INFINITY;
			} else {
				result *= 1 + 100 / occupancy;
			}
		}
		database.close();
		return result;
	}

	@Override
	public double getMinWeight(double currDistToGoal) {
		return currDistToGoal;
	}

	@Override
	public double revertWeight(EdgeIteratorState edgeState, double weight) {
		return weight;
	}

}
