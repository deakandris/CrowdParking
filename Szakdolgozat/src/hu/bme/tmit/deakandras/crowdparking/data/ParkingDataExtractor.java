package hu.bme.tmit.deakandras.crowdparking.data;

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

import android.os.Environment;
import android.support.v4.util.ArrayMap;

import com.nutiteq.components.MapPos;

public class ParkingDataExtractor {
	private static List<ArrayList<MapPos>> roads = new ArrayList<ArrayList<MapPos>>();

	private static final Logger logger = Logger
			.getLogger(ParkingDataExtractor.class.getName());

	public static List<ArrayList<MapPos>> getWaysFromXML()
			throws ParserConfigurationException, SAXException, IOException {

		Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
		logger.setLevel(Level.FINE);
		
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		DefaultHandler handler = new DefaultHandler() {
			ArrayMap<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();
			ArrayList<MapPos> nodeBuffer = new ArrayList<MapPos>();
			List<ArrayList<MapPos>> ways = new ArrayList<ArrayList<MapPos>>();
			int i = 0;

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				logger.entering("DefaultHandler", "startElement",
						new Object[] { uri, localName, qName, attributes });
				if (qName.equalsIgnoreCase("node")) {
					Long id = Long.parseLong(attributes.getValue("id"));
					Float lat = Float.parseFloat(attributes.getValue("lat"));
					Float lon = Float.parseFloat(attributes.getValue("lon"));
					nodes.put(id, new MapPos(lon, lat));
					logger.info("Node element found with ID=" + id);
				} else if (qName.equalsIgnoreCase("nd")) {
					Long ref = Long.parseLong(attributes.getValue("ref"));
					if (nodes.get(ref) != null){
						nodeBuffer.add(nodes.get(ref));
						logger.info("Node reference found with ID=" + ref);
					} else {
						logger.info("Invalid node reference. ID=" + ref);
					}
				}
				logger.exiting("DefaultHandler", "startElement",
						new Object[] { uri, localName, qName, attributes });
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				logger.entering("DefaultHandler", "endElement",
						new Object[] { uri, localName, qName });
				if (qName.equalsIgnoreCase("way")) {
					if (nodeBuffer.size() > 1){
						ways.add(nodeBuffer);
						nodeBuffer = new ArrayList<MapPos>();
						logger.info("Way element #"+ ++i + " added to ways.");
					} else {
						logger.info("Way contains less than 2 nodes, discard way.");
					}
				}
				logger.exiting("DefaultHandler", "endElement",
						new Object[] { uri, localName, qName });
			}

			@Override
			public void endDocument() throws SAXException {
				logger.entering("DefaultHandler", "endDocument");
				roads = ways;
				logger.info("Ended parsing ways.");
				int i = 0;
				for(ArrayList<MapPos> way : roads){
					logger.info("#"+ ++i + " " + way);
				}
				logger.exiting("DefaultHandler", "endDocument");
			}
		};

		saxParser.parse("file://" + Environment.getExternalStorageDirectory()
				+ "/budapest_roads.osm", handler);
		return roads;
	}
}
