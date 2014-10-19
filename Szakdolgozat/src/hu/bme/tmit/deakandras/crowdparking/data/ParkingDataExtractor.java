package hu.bme.tmit.deakandras.crowdparking.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	public static List<ArrayList<MapPos>> getWaysFromXML()
			throws ParserConfigurationException, SAXException, IOException {

		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		DefaultHandler handler = new DefaultHandler() {
			ArrayMap<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();
			ArrayList<MapPos> nodeBuffer = new ArrayList<MapPos>();
			List<ArrayList<MapPos>> ways = new ArrayList<ArrayList<MapPos>>();

			@Override
			public void startElement(String uri, String localName,
					String qName, Attributes attributes) throws SAXException {
				if (qName.equalsIgnoreCase("node")) {
					Long id = Long.parseLong(attributes.getValue("id"));
					Float lat = Float.parseFloat(attributes.getValue("lat"));
					Float lon = Float.parseFloat(attributes.getValue("lon"));
					nodes.put(id, new MapPos(lon, lat));
				} else if (qName.equalsIgnoreCase("nd")) {
					Long ref = Long.parseLong(attributes.getValue("ref"));
					nodeBuffer.add(nodes.get(ref));
				}
			}

			@Override
			public void endElement(String uri, String localName, String qName)
					throws SAXException {
				if (qName.equalsIgnoreCase("way")) {
					ways.add(nodeBuffer);
					nodeBuffer = new ArrayList<MapPos>();
				}
			}

			@Override
			public void endDocument() throws SAXException {
				roads = ways;
			}
		};

		saxParser.parse("file://" + Environment.getExternalStorageDirectory()
				+ "/roadmap.xml", handler);
		return roads;
	}
}
