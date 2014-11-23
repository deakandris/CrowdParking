package hu.bme.tmit.deakandras.crowdparking.data;

import hu.bme.tmit.deakandras.crowdparking.database.DatabaseManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.support.v4.util.ArrayMap;

import com.nutiteq.components.MapPos;

public class ParkingDataLoader {

	private static final Logger logger = Logger
			.getLogger(ParkingDataLoader.class.getName());

	public static List<Way> getWays(final Context context)
			throws ParserConfigurationException, IOException {
		DatabaseManager databaseManager = new DatabaseManager(context);
		databaseManager.open(true);
		List<Way> newWays;
		try {
			newWays = getWaysFromXML(context);
			if (newWays != null && newWays.size() > 0) {
				databaseManager.insertAll(newWays);
				SharedPreferences settings = context.getSharedPreferences(
						Constants.SETTINGS, Context.MODE_PRIVATE);
				Editor editor = settings.edit();
				editor.putLong(Constants.TIMESTAMP, Calendar.getInstance()
						.getTimeInMillis());
				editor.commit();
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<Way> ways = databaseManager.getAll();
		databaseManager.close();
		return ways;
	}

	private static List<Way> getWaysFromXML(final Context context)
			throws ParserConfigurationException, SAXException, IOException {

		final List<Way> roads = new ArrayList<Way>();
		Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
		logger.setLevel(Level.FINE);

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		DefaultHandler handler = new DefaultHandler() {
			ArrayMap<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();
			List<Way> ways = new ArrayList<Way>();
			Way tempway;
			int i = 0;

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				logger.entering("DefaultHandler", "startElement", new Object[] {
						uri, localName, qName, attributes });
				if (qName.equalsIgnoreCase("root")) {
					Long timestamp = Long.parseLong(attributes
							.getValue("timestamp"));
					if (!checkDate(timestamp, context)) {
						throw new SAXException("XML is older than database.");
					}
				} else if (qName.equalsIgnoreCase("node")) {
					Long id = Long.parseLong(attributes.getValue("id"));
					Float lat = Float.parseFloat(attributes.getValue("lat"));
					Float lon = Float.parseFloat(attributes.getValue("lon"));
					nodes.put(id, new MapPos(lon, lat));
					logger.info("Node element found with ID=" + id);
				} else if (qName.equalsIgnoreCase("way")) {
					long id = Long.parseLong(attributes.getValue("id"));
					float occupancy = Float.parseFloat(attributes
							.getValue("occupancy"));
					tempway = new Way(id, new ArrayList<Node>(), occupancy);
				} else if (qName.equalsIgnoreCase("nd")) {

					Long ref = Long.parseLong(attributes.getValue("ref"));
					if (nodes.get(ref) != null) {
						tempway.nodes.add(new Node(ref, nodes.get(ref)));
						logger.info("Node reference found with ID=" + ref);
					} else {
						logger.info("Invalid node reference. ID=" + ref);
					}
				}
				logger.exiting("DefaultHandler", "startElement", new Object[] {
						uri, localName, qName, attributes });
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				logger.entering("DefaultHandler", "endElement", new Object[] {
						uri, localName, qName });
				if (qName.equalsIgnoreCase("way")) {
					if (tempway.nodes.size() > 1) {
						ways.add(tempway);
						tempway = null;
						logger.info("Way element #" + ++i + " added to ways.");
					} else {
						logger.info("Way contains less than 2 nodes, discard way.");
					}
				}
				logger.exiting("DefaultHandler", "endElement", new Object[] {
						uri, localName, qName });
			}

			@Override
			public void endDocument() throws SAXException {
				logger.entering("DefaultHandler", "endDocument");
				roads.addAll(ways);
				logger.info("Ended parsing ways.");
				int i = 0;
				for (Way way : roads) {
					logger.info("#" + ++i + " " + way);
				}
				logger.exiting("DefaultHandler", "endDocument");
			}
		};

		saxParser.parse("file://" + Environment.getExternalStorageDirectory()
				+ "/roadmap.xml", handler);
		return roads;
	}

	private static boolean checkDate(Long timeinmilis, Context context) {
		SharedPreferences settings = context.getSharedPreferences(
				Constants.SETTINGS, Context.MODE_PRIVATE);
		Long databaseTime = settings.getLong(Constants.TIMESTAMP, 0);
		if (timeinmilis > databaseTime) {
			return true;
		} else {
			return false;
		}
	}
}
