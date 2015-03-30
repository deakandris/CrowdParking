package hu.bme.tmit.deakandras.crowdparking.data;

import hu.bme.tmit.deakandras.crowdparking.database.DatabaseManager;

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

import com.nutiteq.components.MapPos;

public class ParkingDataLoader {
	
	private static final class ParkingDataHandler extends DefaultHandler {
		private final Context context;
		private final List<Way> roads;
		ArrayMap<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();
		List<Way> ways = new ArrayList<Way>();
		Way tempway;
		int i = 0;
		
		private ParkingDataHandler(Context context, List<Way> roads) {
			this.context = context;
			this.roads = roads;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			LOGGER.entering("DefaultHandler", "startElement", new Object[] { uri, localName, qName, attributes });
			if (qName.equalsIgnoreCase("root")) {
				Long timestamp = Long.parseLong(attributes.getValue("timestamp"));
				if (!checkDate(timestamp, context)) {
					throw new SAXException("XML is older than database.");
				}
			} else if (qName.equalsIgnoreCase("node")) {
				Long id = Long.parseLong(attributes.getValue("id"));
				Float lat = Float.parseFloat(attributes.getValue("lat"));
				Float lon = Float.parseFloat(attributes.getValue("lon"));
				nodes.put(id, new MapPos(lon, lat));
				LOGGER.info("Node element found with ID=" + id);
			} else if (qName.equalsIgnoreCase("way")) {
				long id = Long.parseLong(attributes.getValue("id"));
				float occupancy = Float.parseFloat(attributes.getValue("occupancy"));
				tempway = new Way(id, new ArrayList<Node>(), occupancy);
			} else if (qName.equalsIgnoreCase("nd")) {
				
				Long ref = Long.parseLong(attributes.getValue("ref"));
				if (nodes.get(ref) != null) {
					tempway.nodes.add(new Node(ref, nodes.get(ref)));
					LOGGER.info("Node reference found with ID=" + ref);
				} else {
					LOGGER.info("Invalid node reference. ID=" + ref);
				}
			}
			LOGGER.exiting("DefaultHandler", "startElement", new Object[] { uri, localName, qName, attributes });
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			LOGGER.entering("DefaultHandler", "endElement", new Object[] { uri, localName, qName });
			if (qName.equalsIgnoreCase("way")) {
				if (tempway.nodes.size() > 1) {
					ways.add(tempway);
					tempway = null;
					LOGGER.info("Way element #" + ++i + " added to ways.");
				} else {
					LOGGER.info("Way contains less than 2 nodes, discard way.");
				}
			}
			LOGGER.exiting("DefaultHandler", "endElement", new Object[] { uri, localName, qName });
		}
		
		@Override
		public void endDocument() throws SAXException {
			LOGGER.entering("DefaultHandler", "endDocument");
			roads.addAll(ways);
			LOGGER.info("Ended parsing ways.");
			int i = 0;
			for (Way way : roads) {
				LOGGER.info("#" + ++i + " " + way);
			}
			LOGGER.exiting("DefaultHandler", "endDocument");
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ParkingDataLoader.class.getName());
	
	public static List<Way> getWays(final Context context, double lat, double lon, double radius)
			throws ParserConfigurationException, IOException {
		DatabaseManager databaseManager = new DatabaseManager(context);
		databaseManager.open(true);
		List<Way> newWays = null;
		try {
			newWays = getWaysFromXML(context);
			// if (checkDate(Calendar.getInstance().getTimeInMillis() - 600,
			// context)) {
			// newWays = getWaysFromServer(context, lat, lon, radius);
			// }
			if (newWays != null && newWays.size() > 0) {
				databaseManager.insertAll(newWays);
				SharedPreferences settings = context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
				Editor editor = settings.edit();
				editor.putLong(Constants.TIMESTAMP, Calendar.getInstance()
															.getTimeInMillis());
				editor.commit();
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
		List<Way> ways = databaseManager.getAll();
		databaseManager.close();
		return ways;
	}
	
	private static List<Way> getWaysFromXML(final Context context) throws ParserConfigurationException, SAXException, IOException {
		
		final List<Way> roads = new ArrayList<Way>();
		Logger.getLogger("")
				.getHandlers()[0].setLevel(Level.ALL);
		LOGGER.setLevel(Level.FINE);
		
		SAXParser saxParser = SAXParserFactory.newInstance()
												.newSAXParser();
		DefaultHandler handler = new ParkingDataHandler(context, roads);
		
		saxParser.parse("file:///storage/external_SD/roadmap.xml", handler);
		return roads;
	}
	
	private static List<Way> getWaysFromServer(final Context context, double lat, double lon, double maxDistance)
			throws IOException {
		List<Way> result = new ArrayList<Way>();
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet("http://152.66.244.102:3000/api/query?lat=" + lat + "&lon=" + lon + "&maxDistance="
				+ maxDistance);
		
		HttpResponse response = httpclient.execute(httpget);
		if (response != null) {
			InputStream is = response.getEntity()
										.getContent();
			// String line = "";
			// StringBuilder sb = new StringBuilder();
			// BufferedReader br = new BufferedReader(
			// new InputStreamReader(is));
			// try {
			// while ((line = br.readLine()) != null) {
			// sb.append(line);
			// }
			// } catch (Exception e) {
			// Toast.makeText(context, "Stream Exception",
			// Toast.LENGTH_SHORT).show();
			// }
			//
			// LOGGER.info(sb.toString());
			
			JsonReader reader = new JsonReader(new InputStreamReader(is));
			long nodeid = 0;
			reader.beginArray();
			while (reader.hasNext()) {
				long id = 0;
				ArrayList<Node> nodes = new ArrayList<Node>();
				float occupancy = 0;
				reader.beginObject();
				while (reader.hasNext()) {
					boolean isLineString = false;
					String name = reader.nextName();
					if (name.equals("id")) {
						String stringid = reader.nextString();
						int index = stringid.indexOf("_");
						if (index == -1) {
							id = Long.parseLong(stringid);
						} else {
							id = Long.parseLong(stringid.substring(0, index) + stringid.substring(index + 1));
						}
					} else if (name.equals("loc")) {
						reader.beginObject();
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("type")) {
								isLineString = reader.nextString()
														.equals("LineString");
							} else if (name.equals("coordinates") && isLineString) {
								reader.beginArray();
								while (reader.hasNext()) {
									Float d = null;
									reader.beginArray();
									while (reader.hasNext()) {
										if (d == null) {
											d = (float) reader.nextDouble();
										} else {
											nodes.add(new Node(++nodeid, d, (float) reader.nextDouble()));
											d = null;
										}
									}
									reader.endArray();
								}
								reader.endArray();
							}
						}
						reader.endObject();
					} else if (name.equals("slots")) {
						reader.beginObject();
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("left")) {
								occupancy = reader.nextInt();
							} else if (name.equals("right")) {
								occupancy += reader.nextInt();
							}
						}
						reader.endObject();
					} else if (name.equals("load")) {
						reader.beginObject();
						float load = 0;
						while (reader.hasNext()) {
							name = reader.nextName();
							if (name.equals("left")) {
								load = reader.nextInt();
							} else if (name.equals("right")) {
								load += reader.nextInt();
								occupancy = load / occupancy * 100;
								if (occupancy > 100) {
									occupancy = 100;
								}
							}
						}
						reader.endObject();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
				if (id > 0 && nodes.size() > 1) {
					result.add(new Way(id, nodes, occupancy));
				}
			}
			reader.close();
		}
		return result;
	}
	
	private static boolean checkDate(Long timeinmilis, Context context) {
		SharedPreferences settings = context.getSharedPreferences(Constants.SETTINGS, Context.MODE_PRIVATE);
		Long databaseTime = settings.getLong(Constants.TIMESTAMP, 0);
		if (timeinmilis > databaseTime) {
			return true;
		} else {
			return false;
		}
	}
}
