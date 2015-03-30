/**
 * 
 */
package hu.bme.tmit.deakandras.crowdparking.gui.map;

import hu.bme.tmit.deakandras.crowdparking.R;
import hu.bme.tmit.deakandras.crowdparking.data.ParkingDataLoader;
import hu.bme.tmit.deakandras.crowdparking.data.Way;
import hu.bme.tmit.deakandras.crowdparking.gui.MainActivity;
import hu.bme.tmit.deakandras.crowdparking.pathfinding.MyGraphHopper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.Path;
import com.graphhopper.util.PointList;
import com.nutiteq.MapView;
import com.nutiteq.components.Bounds;
import com.nutiteq.components.Color;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.datasources.raster.MapsforgeRasterDataSource;
import com.nutiteq.geometry.Geometry;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Marker;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.EPSG4326;
import com.nutiteq.projections.Projection;
import com.nutiteq.rasterlayers.RasterLayer;
import com.nutiteq.services.routing.Route;
import com.nutiteq.services.routing.RouteActivity;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.ui.Label;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;
import com.nutiteq.vectorlayers.MarkerLayer;

public class MapFragment extends Fragment implements RouteActivity {
	
	private final class GraphCalculatorTask extends AsyncTask<Void, Void, GHResponse> {
		private final GHRequest req;
		
		private GraphCalculatorTask(GHRequest req) {
			this.req = req;
		}
		
		protected GHResponse doInBackground(final Void... v) {
			final GHResponse resp = gh.route(req);
			return resp;
		}
		
		protected void onPostExecute(final GHResponse res) {
			
			routeLayer.clear();
			routeLayer.add(createPolyline(startMarker.getMapPos(), stopMarker.getMapPos(), res));
			
		}
	}
	
	private final class LoadingViewShowerAdapter extends AnimatorListenerAdapter {
		private final boolean show;
		
		private LoadingViewShowerAdapter(final boolean show) {
			this.show = show;
		}
		
		@Override
		public void onAnimationEnd(final Animator animation) {
			loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
		}
	}
	
	private final class MapViewShowerAdapter extends AnimatorListenerAdapter {
		private final boolean show;
		
		private MapViewShowerAdapter(final boolean show) {
			this.show = show;
		}
		
		@Override
		public void onAnimationEnd(final Animator animation) {
			mapView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	private final class MyLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(final Location location) {
			if (location.getAccuracy() < 500) {
				if (myLocation == null) {
					Toast.makeText(context, R.string.location_found_jumping_to_your_location, Toast.LENGTH_SHORT)
							.show();
				}
				if (myLocation == null || location.getAccuracy() < myLocation.getAccuracy()) {
					myLocation = location;
				}
				// jump to location
				mapView.setFocusPoint(mapView.getLayers()
												.getBaseLayer()
												.getProjection()
												.fromWgs84(myLocation.getLongitude(), myLocation.getLatitude()));
				
				// draw location
				// Bitmap pointMarker =
				// UnscaledBitmapLoader.decodeResource(
				// getResources(), R.drawable.point);
				// PointStyle pointStyle =
				// PointStyle.builder().setBitmap(pointMarker)
				// .setSize(0.1f).setColor(Color.GREEN).build();
				// locationLayer.add(new Point(new MapPos(19.040833,
				// 47.498333),
				// new DefaultLabel("Your Position"), pointStyle,
				// null));
				
				drawLocation(myLocation);
				
				if (location.getAccuracy() < 5) {
					locationManager.removeUpdates(this);
				}
			}
		}
		
		@Override
		public void onStatusChanged(final String provider, final int status, final Bundle extras) {
		}
		
		@Override
		public void onProviderEnabled(final String provider) {
			if (provider.equals(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
				Toast.makeText(context, R.string.getting_your_location, Toast.LENGTH_SHORT)
						.show();
			} else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
			}
		}
		
		@Override
		public void onProviderDisabled(final String provider) {
			if (provider.equals(LocationManager.GPS_PROVIDER)) {
				Toast.makeText(context, R.string.gps_is_disabled_toast, Toast.LENGTH_LONG)
						.show();
			}
		}
	}
	
	private final class ParkingDataLoaderTask extends AsyncTask<Void, Void, List<Way>> {
		@Override
		protected List<Way> doInBackground(final Void... params) {
			List<Way> roads = new ArrayList<Way>();
			try {
				final MapPos center = (new EPSG3857()).toWgs84(mapView.getFocusPoint().x, mapView.getFocusPoint().y);
				final double lat = center.y;
				final double lon = center.x;
				final double radius = 10000f / mapView.getZoom() + 1;
				roads = ParkingDataLoader.getWays(context, lat, lon, radius);
			} catch (final ParserConfigurationException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
				Toast.makeText(context, "Unable to connect to server. Check your connection!", Toast.LENGTH_LONG)
						.show();
			}
			return roads;
		}
		
		@Override
		protected void onPreExecute() {
			// showProgress(true);
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(final List<Way> result) {
			for (final Way way : result) {
				final int color = android.graphics.Color.HSVToColor(new float[] { (100 - way.occupancy) * 3.6f * 0.35f, 1, 1 });
				final StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
				final PointStyle pointStyle = PointStyle.builder()
														.setBitmap(pointMarker)
														.setSize(0.1f)
														.setColor(color)
														.build();
				pointStyleSet.setZoomStyle(MIN_ZOOM_FOR_OBJECTS, pointStyle);
				final StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
				lineStyleSet.setZoomStyle(MIN_ZOOM_FOR_OBJECTS, LineStyle.builder()
																			.setBitmap(lineMarker)
																			.setWidth(0.1f)
																			.setColor(color)
																			.setPointStyle(null)
																			.build());
				geomLayer.add(new Line(way.getNodesAsMapPos(), null, lineStyleSet, null));
			}
			// showProgress(false);
			super.onPostExecute(result);
		}
	}
	
	private final class GraphLoaderTask extends AsyncTask<Void, Void, Path> {
		protected Path doInBackground(final Void... v) {
			try {
				final MyGraphHopper tmpHopp = new MyGraphHopper(context).forMobile();
				tmpHopp.setCHShortcuts("fastest");
				tmpHopp.load("storage/external_SD/hungary-gh");
				LOGGER.info("found graph with " + tmpHopp.getGraph()
															.getNodes() + " nodes");
				gh = tmpHopp;
				graphLoaded = true;
			} catch (final Throwable t) {
				t.printStackTrace();
				return null;
			}
			return null;
		}
		
		protected void onPostExecute(final Path o) {
			if (graphLoaded)
				Toast.makeText(context, "graph loaded, click on map to set route start and end", Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(context, "graph loading problem", Toast.LENGTH_SHORT)
						.show();
		}
	}
	
	private static final Logger LOGGER = Logger.getLogger(ParkingDataLoader.class.getName());
	private static final int MIN_ZOOM_FOR_OBJECTS = 15;
	
	private Context context;
	private View loadingView;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location myLocation;
	
	private MapView mapView;
	private GeometryLayer geomLayer;
	private GeometryLayer locationLayer;
	private GeometryLayer routeLayer;
	private MarkerLayer markerLayer;
	private Marker startMarker;
	private Marker stopMarker;
	private Bitmap pointMarker;
	private Bitmap lineMarker;
	
	private MyGraphHopper gh;
	private boolean graphLoaded;
	
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(1);
	}
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		context = getActivity();
		setUpLocationManager();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		
		final View root = inflater.inflate(R.layout.fragment_map, container, false);
		loadingView = root.findViewById(R.id.loading_screen);
		
		createMapView(root);
		createParkingDataObjects();
		createRoutingObjects();
		
		final MapEventListener mapListener = new MapEventListener(this);
		mapView.getOptions()
				.setMapListener(mapListener);
		
		return root;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// Start the map - mandatory
		mapView.startMapping();
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (item.getItemId() == R.id.action_locate) {
			if (myLocation != null) {
				mapView.setFocusPoint(mapView.getLayers()
												.getBaseLayer()
												.getProjection()
												.fromWgs84(myLocation.getLongitude(), myLocation.getLatitude()));
				return true;
			} else {
				Toast.makeText(context, "location not found yet", Toast.LENGTH_LONG)
						.show();
			}
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void setStartMarker(final MapPos startPos) {
		routeLayer.clear();
		stopMarker.setVisible(false);
		startMarker.setMapPos(startPos);
		startMarker.setVisible(true);
		
	}
	
	@Override
	public void setStopMarker(final MapPos stopPos) {
		stopMarker.setMapPos(stopPos);
		stopMarker.setVisible(true);
		final MapPos destPos = (new EPSG3857()).toWgs84(stopPos.x, stopPos.y);
		gh.setDestination(destPos.y, destPos.x);
		LOGGER.info("Destination set to " + destPos.y + ", " + destPos.x);
	}
	
	@Override
	public void routeResult(final Route route) {
		// not used
	}
	
	@Override
	public void showRoute(final double fromLat, final double fromLon, final double toLat, final double toLon) {
		if (!graphLoaded) {
			Toast.makeText(context, "graph not loaded yet, cannot route", Toast.LENGTH_LONG)
					.show();
			return;
		}
		
		final Projection projection = mapView.getLayers()
												.getBaseLayer()
												.getProjection();
		stopMarker.setMapPos(projection.fromWgs84(toLon, toLat));
		
		final GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon).setAlgorithm("dijkstrabi")
																				.putHint("instructions", true)
																				.putHint("douglas.minprecision", 1);
		
		Toast.makeText(context, "Calculating route...", Toast.LENGTH_SHORT)
				.show();
		
		new GraphCalculatorTask(request).execute();
	}
	
	@Override
	public void onStop() {
		// Stop the map - mandatory to avoid problems with app restart
		mapView.stopMapping();
		super.onStop();
	}
	
	private void setUpLocationManager() {
		// Start the Location Service and binds a listener to it.
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new MyLocationListener();
		
		if (myLocation == null) {
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				Toast.makeText(context, R.string.getting_your_location, Toast.LENGTH_SHORT)
						.show();
			} else {
				Toast.makeText(context, R.string.gps_is_disabled_toast, Toast.LENGTH_LONG)
						.show();
			}
			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			}
		}
	}
	
	private MapView createMapView(final View root) {
		mapView = (MapView) root.findViewById(R.id.mapView);
		// define new configuration holder object
		mapView.setComponents(new Components());
		
		// Define base layer
		final String mapFile = "/storage/external_SD/hungary-gh/hungary.map";
		final JobTheme renderTheme = MapsforgeRasterDataSource.InternalRenderTheme.OSMARENDER;
		final MapsforgeRasterDataSource dataSource = new MapsforgeRasterDataSource(new EPSG3857(), 0, 20, mapFile, renderTheme);
		final RasterLayer mapLayer = new RasterLayer(dataSource, 1044);
		
		mapView.getLayers()
				.setBaseLayer(mapLayer);
		
		// set initial map view camera from database
		final MapFileInfo mapFileInfo = dataSource.getMapDatabase()
													.getMapFileInfo();
		if (mapFileInfo != null) {
			if (mapFileInfo.startPosition != null && mapFileInfo.startZoomLevel != null) {
				// start position is defined
				final MapPos mapCenter = new MapPos(mapFileInfo.startPosition.getLongitude(),
						mapFileInfo.startPosition.getLatitude(), mapFileInfo.startZoomLevel);
				LOGGER.info("center: " + mapCenter);
				mapView.setFocusPoint(mapView.getLayers()
												.getBaseLayer()
												.getProjection()
												.fromWgs84(mapCenter.x, mapCenter.y));
				mapView.setZoom((float) mapCenter.z);
			} else if (mapFileInfo.boundingBox != null) {
				// start position not defined, but boundingbox is defined
				final MapPos boxMin = mapView.getLayers()
												.getBaseLayer()
												.getProjection()
												.fromWgs84(mapFileInfo.boundingBox.getMinLongitude(),
														mapFileInfo.boundingBox.getMinLatitude());
				final MapPos boxMax = mapView.getLayers()
												.getBaseLayer()
												.getProjection()
												.fromWgs84(mapFileInfo.boundingBox.getMaxLongitude(),
														mapFileInfo.boundingBox.getMaxLatitude());
				mapView.setBoundingBox(new Bounds(boxMin.x, boxMin.y, boxMax.x, boxMax.y), true);
			}
		}
		// if no fileinfo, startPosition or boundingBox, then remain to default
		mapView.setFocusPoint(mapView.getLayers()
										.getBaseLayer()
										.getProjection()
										.fromWgs84(19.0575, 47.4730));
		mapView.setZoom(16);
		
		// Activate some mapview options to make it smoother
		mapView.getOptions()
				.setPreloading(false);
		mapView.getOptions()
				.setSeamlessHorizontalPan(true);
		mapView.getOptions()
				.setTileFading(false);
		mapView.getOptions()
				.setKineticPanning(true);
		mapView.getOptions()
				.setDoubleClickZoomIn(true);
		mapView.getOptions()
				.setDualClickZoomOut(true);
		
		// configure texture caching
		mapView.getOptions()
				.setTextureMemoryCacheSize(20 * 1024 * 1024);
		mapView.getOptions()
				.setCompressedMemoryCacheSize(8 * 1024 * 1024);
		
		return mapView;
		
	}
	
	/**
	 * Shows the progress UI and hides the mapview.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			final int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
			
			loadingView.setVisibility(View.VISIBLE);
			loadingView.animate()
						.setDuration(shortAnimTime)
						.alpha(show ? 1 : 0)
						.setListener(new LoadingViewShowerAdapter(show));
			
			mapView.setVisibility(View.VISIBLE);
			mapView.animate()
					.setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new MapViewShowerAdapter(show));
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
			mapView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	private void createParkingDataObjects() {
		geomLayer = new GeometryLayer(new EPSG4326());
		
		pointMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.point);
		lineMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.line);
		
		final StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		final PointStyle pointStyle = PointStyle.builder()
												.setBitmap(pointMarker)
												.setSize(0.1f)
												.setColor(Color.GREEN)
												.build();
		pointStyleSet.setZoomStyle(MIN_ZOOM_FOR_OBJECTS, pointStyle);
		
		// geomLayer.add(new Point(new MapPos(19.055556, 47.481389),
		// new DefaultLabel(
		// "Budapesti Mûszaki és Gazdaságtudományi Egyetem"),
		// pointStyle, null));
		
		new ParkingDataLoaderTask().execute();
		
		mapView.getLayers()
				.addLayer(geomLayer);
	}
	
	private void drawLocation(final Location location) {
		
		// define marker style (image, size, color)
		final Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.olmarker);
		final MarkerStyle markerStyle = MarkerStyle.builder()
													.setBitmap(pointMarker)
													.setSize(0.5f)
													.setColor(Color.WHITE)
													.build();
		// define label what is shown when you click on marker
		final Label markerLabel = new DefaultLabel("Your location", "Here is a marker");
		
		// define location of the marker, it must be converted to base map
		// coordinate system
		final MapPos markerLocation = mapView.getLayers()
												.getBaseProjection()
												.fromWgs84(location.getLongitude(), location.getLatitude());
		
		// create layer and add object to the layer, finally add layer to the
		// map
		if (markerLayer != null) {
			mapView.getLayers()
					.removeLayer(markerLayer);
		}
		markerLayer = new MarkerLayer(mapView.getLayers()
												.getBaseProjection());
		markerLayer.add(new Marker(markerLocation, markerLabel, markerStyle, markerLayer));
		mapView.getLayers()
				.addLayer(markerLayer);
		
		// create location circle
		locationLayer = new GeometryLayer(mapView.getLayers()
													.getBaseProjection());
		circle(location.getLatitude(), location.getLongitude(), location.getAccuracy(), locationLayer);
		mapView.getComponents().layers.addLayer(locationLayer);
	}
	
	// helper to draw a circle to given layer
	private void circle(final double d, final double e, final float circleRadius, final GeometryLayer layer) {
		// number of circle line points
		final int NR_OF_CIRCLE_VERTS = 18;
		final List<MapPos> circleVerts = new ArrayList<MapPos>(NR_OF_CIRCLE_VERTS);
		
		final MapPos circlePos = layer.getProjection()
										.fromWgs84(d, e);
		// width of map to scale circle
		final float projectionScale = (float) layer.getProjection()
													.getBounds()
													.getWidth();
		// convert radius from meters to map units
		final float circleScale = circleRadius / 7500000f * projectionScale;
		
		for (float tsj = 0; tsj <= 360; tsj += 360 / NR_OF_CIRCLE_VERTS) {
			final MapPos mapPos = new MapPos(circleScale * Math.cos(tsj * Const.DEG_TO_RAD) + circlePos.x, circleScale
					* Math.sin(tsj * Const.DEG_TO_RAD) + circlePos.y);
			circleVerts.add(mapPos);
		}
		final LineStyle style = LineStyle.builder()
											.setWidth(0.1f)
											.setColor(Color.argb(192, 255, 255, 0))
											.build();
		final Line circle = new Line(circleVerts, null, style, null);
		layer.add(circle);
	}
	
	private void createRoutingObjects() {
		new GraphLoaderTask().execute();
		
		routeLayer = new GeometryLayer(new EPSG3857());
		mapView.getLayers()
				.addLayer(routeLayer);
		
		final Bitmap olMarker = UnscaledBitmapLoader.decodeResource(getResources(), R.drawable.olmarker);
		final StyleSet<MarkerStyle> startMarkerStyleSet = new StyleSet<MarkerStyle>(MarkerStyle.builder()
																								.setBitmap(olMarker)
																								.setColor(Color.GREEN)
																								.setSize(0.2f)
																								.build());
		startMarker = new Marker(new MapPos(0, 0), new DefaultLabel("Start"), startMarkerStyleSet, null);
		
		final StyleSet<MarkerStyle> stopMarkerStyleSet = new StyleSet<MarkerStyle>(MarkerStyle.builder()
																								.setBitmap(olMarker)
																								.setColor(Color.RED)
																								.setSize(0.2f)
																								.build());
		stopMarker = new Marker(new MapPos(0, 0), new DefaultLabel("Stop"), stopMarkerStyleSet, null);
		
		final MarkerLayer markerLayer = new MarkerLayer(new EPSG3857());
		mapView.getLayers()
				.addLayer(markerLayer);
		
		markerLayer.add(startMarker);
		markerLayer.add(stopMarker);
	}
	
	private Geometry createPolyline(final MapPos startPos, final MapPos stopPos, final GHResponse response) {
		final StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>(LineStyle.builder()
																					.setWidth(0.05f)
																					.setColor(Color.BLUE)
																					.build());
		
		final Projection proj = mapView.getLayers()
										.getBaseLayer()
										.getProjection();
		final int points = response.getPoints()
									.getSize();
		final List<MapPos> geoPoints = new ArrayList<MapPos>(points + 2);
		final PointList tmp = response.getPoints();
		geoPoints.add(startPos);
		for (int i = 0; i < points; i++) {
			geoPoints.add(proj.fromWgs84(tmp.getLongitude(i), tmp.getLatitude(i)));
		}
		geoPoints.add(stopPos);
		
		final String labelText = "" + (int) (response.getDistance() / 100) / 10f + "km, time:" + response.getTime() / 60f + "min";
		
		return new Line(geoPoints, new DefaultLabel("Route", labelText), lineStyleSet, null);
	}
}
