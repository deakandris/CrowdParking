package hu.bme.tmit.deakandras.crowdparking.activity.data;

import java.io.IOException;
import java.util.ArrayList;
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

	public static List<Road> getWays(final Context context)
			throws ParserConfigurationException, SAXException, IOException {
		List<Road> newWays = getWaysFromXML(context);
		return null;
	}

	private static List<Road> getWaysFromXML(final Context context)
			throws ParserConfigurationException, SAXException, IOException {

		final List<Road> roads = new ArrayList<Road>();
		Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
		logger.setLevel(Level.FINE);

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		DefaultHandler handler = new DefaultHandler() {
			ArrayMap<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();
			ArrayList<MapPos> nodeBuffer = new ArrayList<MapPos>();
			List<Road> ways = new ArrayList<Road>();
			float occupancy = 0;
			int i = 0;

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				logger.entering("DefaultHandler", "startElement", new Object[] {
						uri, localName, qName, attributes });
				if (qName.equalsIgnoreCase("root")) {
					Long timestamp = Long.parseLong(attributes
							.getValue("timestamp"));
					if (checkDate(timestamp, context)) {
//						throw new SAXException("XML is older than database.");
					}
				} else if (qName.equalsIgnoreCase("node")) {
					Long id = Long.parseLong(attributes.getValue("id"));
					Float lat = Float.parseFloat(attributes.getValue("lat"));
					Float lon = Float.parseFloat(attributes.getValue("lon"));
					nodes.put(id, new MapPos(lon, lat));
					logger.info("Node element found with ID=" + id);
				} else if (qName.equalsIgnoreCase("way")) {
					occupancy = Float.parseFloat(attributes.getValue("occupancy"));
				} else if (qName.equalsIgnoreCase("nd")) {

					Long ref = Long.parseLong(attributes.getValue("ref"));
					if (nodes.get(ref) != null) {
						nodeBuffer.add(nodes.get(ref));
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
					if (nodeBuffer.size() > 1) {
						ways.add(new Road(nodeBuffer, occupancy));
						nodeBuffer = new ArrayList<MapPos>();
						occupancy = 0;
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
				for (Road way : roads) {
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
