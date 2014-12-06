package hu.bme.tmit.deakandras.crowdparking.data;

import hu.bme.tmit.deakandras.crowdparking.database.DatabaseManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v4.util.ArrayMap;
import android.util.JsonReader;
import android.widget.Toast;

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
//			newWays = getWaysFromServer(context, 47.475929, 19.001738, 500);
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

		saxParser.parse("file:///storage/external_SD/roadmap.xml", handler);
		return roads;
	}

	private static List<Way> getWaysFromServer(final Context context, double lat, double lon, double maxDistance){
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://152.66.244.102:3000/api/query?lat="+lat+"&lon="+lon+"&maxDistance="+maxDistance);
		
		try {
		    HttpResponse response = httpclient.execute(httpget);
		    if(response != null) {
		        String line = "";
		        InputStream is = response.getEntity().getContent();
		        StringBuilder sb = new StringBuilder();
		        BufferedReader br = new BufferedReader(new InputStreamReader(is));
		        try {
		            while ((line = br.readLine()) != null) {
		                sb.append(line);
		            }
		        } catch (Exception e) {
		            Toast.makeText(context, "Stream Exception", Toast.LENGTH_SHORT).show();
		        }

		        logger.info(sb.toString());
		        
		        JsonReader reader = new JsonReader(new InputStreamReader(is));
		        reader.beginArray();
		        while(reader.hasNext()){
		        	long id = -1;
		        	List<Node> nodes = new ArrayList<Node>();
		        	reader.beginObject();
		        	while(reader.hasNext()){
		        		boolean isLineString = false;
		        		String name = reader.nextName();
		        		if (name.equals("_id")){
		        			id = reader.nextLong();
		        		} else if (name.equals("loc")){
		        			reader.beginObject();
		        			while(reader.hasNext()){
		        				name = reader.nextName();
		        				if(name.equals("type") && reader.nextString().equals("LineString")){
		        					isLineString = true;
		        				} else if (name.equals("coordinates") && isLineString){
		        					reader.beginArray();
		        					while(reader.hasNext()){
		        						Double d = null;
		        						reader.beginArray();
		        						while(reader.hasNext()){
		        							if(d == null) {
		        								d = reader.nextDouble();
		        							} else {
//		        								nodes.add(new Node(id, lat, lon))
		        								// TODO
		        							}
		        						}
		        					}
		        				}
		        			}
		        		}
		        	}
		        }
		    } else {
		        Toast.makeText(context, "Unable to complete your request", Toast.LENGTH_LONG).show();
		    }
		} catch (ClientProtocolException e) {
		    Toast.makeText(context, "Caught ClientProtocolException", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
		    Toast.makeText(context, "Caught IOException", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
		    Toast.makeText(context, "Caught Exception", Toast.LENGTH_SHORT).show();
		}
		return null;
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
