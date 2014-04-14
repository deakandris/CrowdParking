/**
 * 
 */
package hu.bme.tmit.deakandras.crowdparking;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.map.reader.header.MapFileInfo;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nutiteq.MapView;
import com.nutiteq.components.Bounds;
import com.nutiteq.components.Color;
import com.nutiteq.components.Components;
import com.nutiteq.components.MapPos;
import com.nutiteq.datasources.raster.MapsforgeRasterDataSource;
import com.nutiteq.geometry.Line;
import com.nutiteq.geometry.Point;
import com.nutiteq.geometry.Polygon;
import com.nutiteq.log.Log;
import com.nutiteq.projections.EPSG3857;
import com.nutiteq.projections.EPSG4326;
import com.nutiteq.rasterlayers.RasterLayer;
import com.nutiteq.style.LineStyle;
import com.nutiteq.style.PointStyle;
import com.nutiteq.style.PolygonStyle;
import com.nutiteq.style.StyleSet;
import com.nutiteq.ui.DefaultLabel;
import com.nutiteq.utils.UnscaledBitmapLoader;
import com.nutiteq.vectorlayers.GeometryLayer;

public class MapFragment extends Fragment {

	MapView mapView;

	public MapFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View root = inflater.inflate(R.layout.fragment_map, container, false);

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
		// world view

		// Activate some mapview options to make it smoother - optional
		mapView.getOptions().setPreloading(false);
		mapView.getOptions().setSeamlessHorizontalPan(true);
		mapView.getOptions().setTileFading(false);
		mapView.getOptions().setKineticPanning(true);
		mapView.getOptions().setDoubleClickZoomIn(true);
		mapView.getOptions().setDualClickZoomOut(true);

		// configure texture caching - optional, suggested
		mapView.getOptions().setTextureMemoryCacheSize(20 * 1024 * 1024);
		mapView.getOptions().setCompressedMemoryCacheSize(8 * 1024 * 1024);
		return mapView;

	}

	private void createTestObjects() {
		GeometryLayer geomLayer;
		
		geomLayer = new GeometryLayer(new EPSG4326());

		// define minimum zoom for vector style visibility. If 0, then objects
		// are visible with any zoom.
		int minZoom = 0;

		// load bitmaps for vector elements. You can get the images from
		// Hellomap3D project res/drawable
		// these are simple anti-aliased bitmaps which can change colour, should
		// be good for most cases
		Bitmap pointMarker = UnscaledBitmapLoader.decodeResource(
				getResources(), R.drawable.point);
		Bitmap lineMarker = UnscaledBitmapLoader.decodeResource(getResources(),
				R.drawable.line);

		// set styles for all 3 object types: point, line and polygon
		StyleSet<PointStyle> pointStyleSet = new StyleSet<PointStyle>();
		PointStyle pointStyle = PointStyle.builder().setBitmap(pointMarker)
				.setSize(0.1f).setColor(Color.GREEN).build();
		pointStyleSet.setZoomStyle(minZoom, pointStyle);

		// We reuse here pointStyle for Line. This is used for line caps, useful
		// for nicer polylines
		// Also do not forget to set Bitmap for Line. This allows to have fancy
		// styles for lines.
		StyleSet<LineStyle> lineStyleSet = new StyleSet<LineStyle>();
		lineStyleSet.setZoomStyle(minZoom,
				LineStyle.builder().setBitmap(lineMarker).setWidth(0.1f)
						.setColor(Color.GREEN).setPointStyle(pointStyle)
						.build());
		PolygonStyle polygonStyle = PolygonStyle.builder().setColor(Color.BLUE)
				.build();
		StyleSet<PolygonStyle> polygonStyleSet = new StyleSet<PolygonStyle>(
				null);
		polygonStyleSet.setZoomStyle(minZoom, polygonStyle);

		geomLayer.add(new Point(new MapPos(19.040833, 47.498333),
				new DefaultLabel("Budapest"), pointStyle, null));

		// define 2 lines as WGS84 coordinates in an array.
		double[][][] lCoordss = { { { 0, 0 }, { 0, 51 }, { 22, 51 } },
				{ { 2, 2 }, { 2, 50 }, { 24, 50 } } };

		// create two lines with these coordinates
		// if your line is in basemap projection coordinates, no need to use
		// conversion
		for (double[][] lCoords : lCoordss) {
			ArrayList<MapPos> lPoses = new ArrayList<MapPos>();
			for (double[] coord : lCoords) {
				lPoses.add(geomLayer.getProjection().fromWgs84(
						(float) coord[0], (float) coord[1]));
			}
			geomLayer.add(new Line(lPoses, new DefaultLabel("Line"),
					lineStyleSet, null));
		}

		// add polygon with a hole. Inner hole coordinates must be entirely
		// within.
		double[][] pCoordsOuter = { { 0, 0 }, { 0, 51 }, { 22, 51 }, { 0, 0 } }; // outer
																					// ring
		double[][] pCoordsInner = { { 1, 10 }, { 1, 50 }, { 10, 50 }, { 1, 10 } }; // inner
																					// ring

		ArrayList<MapPos> outerPoses = new ArrayList<MapPos>();
		for (double[] coord : pCoordsOuter) {
			outerPoses.add(geomLayer.getProjection().fromWgs84((float) coord[0],
					(float) coord[1]));
		}

		ArrayList<MapPos> innerPoses = new ArrayList<MapPos>();
		for (double[] coord : pCoordsInner) {
			innerPoses.add(geomLayer.getProjection().fromWgs84((float) coord[0],
					(float) coord[1]));
		}
		// we need to create List of holes, as one polygon can have several
		// holes
		// here we have just one. You can have nil there also.
		List<List<MapPos>> holes = new ArrayList<List<MapPos>>();
		holes.add(innerPoses);

		//geomLayer.add(new Polygon(outerPoses, holes,
		//		new DefaultLabel("Polygon"), polygonStyleSet, null));
		
		mapView.getLayers().addLayer(geomLayer);
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
