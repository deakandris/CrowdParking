/**
 * 
 */
package hu.bme.tmit.deakandras.crowdparking;

import hu.bme.tmit.deakandras.crowdparking.data.ParkingDataExtractor;
import hu.bme.tmit.deakandras.crowdparking.data.ParkingDataProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.xml.sax.SAXException;

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
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nutiteq.MapView;
import com.nutiteq.components.Bounds;
import com.nutiteq.components.Color;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.datasources.raster.MapsforgeRasterDataSource;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Marker;
import com.nutiteq.geometry.Point;
import com.nutiteq.log.Log;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.EPSG4326;
import com.nutiteq.rasterlayers.RasterLayer;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.MarkerStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.ui.Label;
import com.nutiteq.utils.Const;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;
import com.nutiteq.vectorlayers.MarkerLayer;

public class MapFragment extends Fragment {

	private Context context;
	private MapView mapView;
	private View loadingView;
	private GeometryLayer locationLayer;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location myLocation;
	private StyleSet<PointStyle> pointStyleSet;
	private StyleSet<LineStyle> lineStyleSet;
	private StyleSet<PolygonStyle> polygonStyleSet;
	private GeometryLayer geomLayer;

	private class ParkingDataLoaderTask extends AsyncTask<Void, Void, List<ArrayList<MapPos>>> {

		@Override
		protected List<ArrayList<MapPos>> doInBackground(Void... params) {
			List<ArrayList<MapPos>> roads = new ArrayList<ArrayList<MapPos>>();
			try {
				roads = ParkingDataExtractor.getWaysFromXML();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return roads;
		}

		@Override
		protected void onPreExecute() {
			showProgress(true);
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(List<ArrayList<MapPos>> result) {
			for(ArrayList<MapPos> way : result){
				
				geomLayer.add(new Line(way, null, lineStyleSet, null));
			}
			showProgress(false);
			super.onPostExecute(result);
		}
	}
	
	public MapFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = getActivity();

		// Start the Location Service and binds a listener to it.
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {

			@Override
			public void onLocationChanged(Location location) {
				if (location.getAccuracy() < 500) {
					if (myLocation == null) {
						Toast.makeText(
								context,
								R.string.location_found_jumping_to_your_location,
								Toast.LENGTH_SHORT).show();
					}
					if (myLocation == null
							|| location.getAccuracy() < myLocation
									.getAccuracy()) {
						myLocation = location;
					}

					// jump to location
					mapView.setFocusPoint(mapView
							.getLayers()
							.getBaseLayer()
							.getProjection()
							.fromWgs84(myLocation.getLongitude(),
									myLocation.getLatitude()));

					// draw location
					// Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
					// getResources(), R.drawable.point);
					// PointStyle pointStyle =
					// PointStyle.builder().setBitmap(pointMarker)
					// .setSize(0.1f).setColor(Color.GREEN).build();
					// locationLayer.add(new Point(new MapPos(19.040833,
					// 47.498333),
					// new DefaultLabel("Your Position"), pointStyle, null));

					drawLocation(myLocation);

					if (location.getAccuracy() < 5) {
						locationManager.removeUpdates(this);
					}
				}
			}

			@Override
			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			@Override
			public void onProviderEnabled(String provider) {
				if (provider.equals(LocationManager.GPS_PROVIDER)) {
					locationManager.requestLocationUpdates(
							LocationManager.GPS_PROVIDER, 0, 0,
							locationListener);
					Toast.makeText(context, R.string.getting_your_location,
							Toast.LENGTH_SHORT).show();
				} else if (locationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER, 0, 0,
							locationListener);
				}
			}

			@Override
			public void onProviderDisabled(String provider) {
				if (provider.equals(LocationManager.GPS_PROVIDER)) {
					Toast.makeText(context, R.string.gps_is_disabled_toast,
							Toast.LENGTH_LONG).show();
				}
			}

		};

		if (myLocation == null) {
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				Toast.makeText(context, R.string.getting_your_location,
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context, R.string.gps_is_disabled_toast,
						Toast.LENGTH_LONG).show();
			}
			if (locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 0, 0,
						locationListener);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View root = inflater.inflate(R.layout.fragment_map, container, false);
		loadingView = root.findViewById(R.id.loading_screen);
		
		createMapView(root);
		createTestObjects();

		return root;
	}

	private MapView createMapView(View root) {
		mapView = (MapView) root.findViewById(R.id.mapView);
		// define new configuration holder object
		mapView.setComponents(new Components());

		// Define base layer
		String mapFile = Environment.getExternalStorageDirectory()
				+ "/hungary-gh/hungary.map";
		JobTheme renderTheme = MapsforgeRasterDataSource.InternalRenderTheme.OSMARENDER;
		MapsforgeRasterDataSource dataSource = new MapsforgeRasterDataSource(
				new EPSG3857(), 0, 20, mapFile, renderTheme);
		RasterLayer mapLayer = new RasterLayer(dataSource, 1044);

		mapView.getLayers().setBaseLayer(mapLayer);

		// set initial map view camera from database
		MapFileInfo mapFileInfo = dataSource.getMapDatabase().getMapFileInfo();
		if (mapFileInfo != null) {
			if (mapFileInfo.startPosition != null
					&& mapFileInfo.startZoomLevel != null) {
				// start position is defined
				MapPos mapCenter = new MapPos(
						mapFileInfo.startPosition.getLongitude(),
						mapFileInfo.startPosition.getLatitude(),
						mapFileInfo.startZoomLevel);
				Log.debug("center: " + mapCenter);
				mapView.setFocusPoint(mapView.getLayers().getBaseLayer()
						.getProjection().fromWgs84(mapCenter.x, mapCenter.y));
				mapView.setZoom((float) mapCenter.z);
			} else if (mapFileInfo.boundingBox != null) {
				// start position not defined, but boundingbox is defined
				MapPos boxMin = mapView
						.getLayers()
						.getBaseLayer()
						.getProjection()
						.fromWgs84(mapFileInfo.boundingBox.getMinLongitude(),
								mapFileInfo.boundingBox.getMinLatitude());
				MapPos boxMax = mapView
						.getLayers()
						.getBaseLayer()
						.getProjection()
						.fromWgs84(mapFileInfo.boundingBox.getMaxLongitude(),
								mapFileInfo.boundingBox.getMaxLatitude());
				mapView.setBoundingBox(new Bounds(boxMin.x, boxMin.y, boxMax.x,
						boxMax.y), true);
			}
		}
		// if no fileinfo, startPosition or boundingBox, then remain to default
		mapView.setFocusPoint(mapView.getLayers().getBaseLayer()
				.getProjection().fromWgs84(19.0575, 47.4730));
		mapView.setZoom(16);

		// Activate some mapview options to make it smoother
		mapView.getOptions().setPreloading(false);
		mapView.getOptions().setSeamlessHorizontalPan(true);
		mapView.getOptions().setTileFading(false);
		mapView.getOptions().setKineticPanning(true);
		mapView.getOptions().setDoubleClickZoomIn(true);
		mapView.getOptions().setDualClickZoomOut(true);

		// configure texture caching
		mapView.getOptions().setTextureMemoryCacheSize(20 * 1024 * 1024);
		mapView.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);

		return mapView;

	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
		private void showProgress(final boolean show) {
			// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
			// for very easy animations. If available, use these APIs to fade-in
			// the progress spinner.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				int shortAnimTime = getResources().getInteger(
						android.R.integer.config_shortAnimTime);

				loadingView.setVisibility(View.VISIBLE);
				loadingView.animate().setDuration(shortAnimTime)
						.alpha(show ? 1 : 0)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								loadingView.setVisibility(show ? View.VISIBLE
										: View.GONE);
							}
						});

				mapView.setVisibility(View.VISIBLE);
				mapView.animate().setDuration(shortAnimTime)
						.alpha(show ? 0 : 1)
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								mapView.setVisibility(show ? View.VISIBLE
										: View.VISIBLE);
							}
						});
			} else {
				// The ViewPropertyAnimator APIs are not available, so simply show
				// and hide the relevant UI components.
				loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
				mapView.setVisibility(show ? View.GONE : View.VISIBLE);
			}
		}
	
	private void createTestObjects() {
		geomLayer = new GeometryLayer(new EPSG4326());

		// define minimum zoom for vector style visibility. If 0, then objects
		// are visible with any zoom.
		int minZoom = 15;

		// load bitmaps for vector elements. You can get the images from
		// Hellomap3D project res/drawable
		// these are simple anti-aliased bitmaps which can change colour, should
		// be good for most cases
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
				getResources(), R.drawable.point);
		Bitmap lineMarker = UnscaledBitmapLoader.decodeResource(getResources(),
				R.drawable.line);

		// set styles for all 3 object types: point, line and polygon
		pointStyleSet = new StyleSet<PointStyle>();
		PointStyle pointStyle = PointStyle.builder().setBitmap(pointMarker)
				.setSize(0.1f).setColor(Color.GREEN).build();
		pointStyleSet.setZoomStyle(minZoom, pointStyle);

		// We reuse here pointStyle for Line. This is used for line caps, useful
		// for nicer polylines
		// Also do not forget to set Bitmap for Line. This allows to have fancy
		// styles for lines.
		lineStyleSet = new StyleSet<LineStyle>();
		lineStyleSet.setZoomStyle(minZoom,
				LineStyle.builder().setBitmap(lineMarker).setWidth(0.1f)
						.setColor(Color.GREEN).setPointStyle(pointStyle)
						.build());
		PolygonStyle polygonStyle = PolygonStyle.builder().setColor(Color.BLUE)
				.build();
		polygonStyleSet = new StyleSet<PolygonStyle>(
				null);
		polygonStyleSet.setZoomStyle(minZoom, polygonStyle);

		geomLayer.add(new Point(new MapPos(19.055556, 47.481389),
				new DefaultLabel(
						"Budapesti Mûszaki és Gazdaságtudományi Egyetem"),
				pointStyle, null));

		new ParkingDataLoaderTask().execute(null, null, null);
		
		mapView.getLayers().addLayer(geomLayer);
	}

	private void drawLocation(Location location) {

		// define marker style (image, size, color)
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
				getResources(), R.drawable.olmarker);
		MarkerStyle markerStyle = MarkerStyle.builder().setBitmap(pointMarker)
				.setSize(0.5f).setColor(Color.WHITE).build();
		// define label what is shown when you click on marker
		Label markerLabel = new DefaultLabel("Your location",
				"Here is a marker");

		// define location of the marker, it must be converted to base map
		// coordinate system
		MapPos markerLocation = mapView.getLayers().getBaseProjection()
				.fromWgs84(location.getLongitude(), location.getLatitude());

		// create layer and add object to the layer, finally add layer to the
		// map
		MarkerLayer markerLayer = new MarkerLayer(mapView.getLayers()
				.getBaseProjection());
		markerLayer.add(new Marker(markerLocation, markerLabel, markerStyle,
				markerLayer));
		mapView.getLayers().addLayer(markerLayer);

		// create location circle
		locationLayer = new GeometryLayer(mapView.getLayers()
				.getBaseProjection());
		circle(location.getLatitude(), location.getLongitude(),
				location.getAccuracy(), locationLayer);
		mapView.getComponents().layers.addLayer(locationLayer);
	}

	// helper to draw a circle to given layer
	private void circle(double d, double e, float circleRadius,
			GeometryLayer layer) {
		// number of circle line points
		int NR_OF_CIRCLE_VERTS = 18;
		List<MapPos> circleVerts = new ArrayList<MapPos>(NR_OF_CIRCLE_VERTS);

		MapPos circlePos = layer.getProjection().fromWgs84(d, e);
		// width of map to scale circle
		float projectionScale = (float) layer.getProjection().getBounds()
				.getWidth();
		// convert radius from meters to map units
		float circleScale = circleRadius / 7500000f * projectionScale;

		for (float tsj = 0; tsj <= 360; tsj += 360 / NR_OF_CIRCLE_VERTS) {
			MapPos mapPos = new MapPos(circleScale
					* Math.cos(tsj * Const.DEG_TO_RAD) + circlePos.x,
					circleScale * Math.sin(tsj * Const.DEG_TO_RAD)
							+ circlePos.y);
			circleVerts.add(mapPos);
		}
		LineStyle style = LineStyle.builder().setWidth(0.1f)
				.setColor(Color.argb(192, 255, 255, 0)).build();
		Line circle = new Line(circleVerts, null, style, null);
		layer.add(circle);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(1);
	}

	@Override
	public void onStart() {
		super.onStart();
		// Start the map - mandatory
		mapView.startMapping();
	}

	@Override
	public void onStop() {
		// Stop the map - mandatory to avoid problems with app restart
		mapView.stopMapping();
		super.onStop();
	}

}
