package hu.bme.tmit.deakandras.crowdparking.database;

import hu.bme.tmit.deakandras.crowdparking.data.Node;
import hu.bme.tmit.deakandras.crowdparking.data.Way;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.ArrayMap;

import com.nutiteq.components.MapPos;

public class DatabaseManager {

	private Context context;
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase database;

	public DatabaseManager(Context context) {
		this.context = context;
	}

	/** Opens the database associated with the application. */
	public void open(boolean writable) {
		databaseHelper = new DatabaseHelper(context);
		if (writable) {
			database = databaseHelper.getWritableDatabase();
		} else {
			database = databaseHelper.getReadableDatabase();
		}
	}

	/** Closes the database. */
	public void close() {
		databaseHelper.close();
	}

	/**
	 * Insert a single node element to the database.
	 * 
	 * @param node
	 *            the {@link Node} object to insert
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insertNode(Node node) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_NODE_ID, node.id);
		values.put(DatabaseConstants.KEY_NODE_LAT, node.lat);
		values.put(DatabaseConstants.KEY_NODE_LON, node.lon);
		return database.insertWithOnConflict(
				DatabaseConstants.NODES_TABLE_NAME, null, values,
				SQLiteDatabase.CONFLICT_IGNORE);
	}

	/**
	 * Insert a single way element to the database. This method does not insert
	 * the referenced nodes.
	 * 
	 * @param way
	 *            the {@link Way} object to insert
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insertWay(Way way) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_WAY_ID, way.id);
		values.put(DatabaseConstants.KEY_WAY_OCCUPANCY, way.occupancy);
		for (Node node : way.nodes) {
			if (insertAssignment(node.id, way.id) == -1) {
				throw new SQLException("Node #" + node.id
						+ " doesn't exists in database");
			}
		}
		return database.insert(DatabaseConstants.WAYS_TABLE_NAME, null, values);
	}

	/**
	 * Insert an assignment of an existing node and way to the database.
	 * 
	 * @param nodeid
	 *            an existing node identifier
	 * @param wayid
	 *            an existing way identifier
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	private long insertAssignment(Long nodeid, Long wayid) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_WAY_NODE_NODEID, nodeid);
		values.put(DatabaseConstants.KEY_WAY_NODE_WAYID, wayid);
		return database.insert(DatabaseConstants.WAY_NODE_TABLE_NAME, null,
				values);
	}

	public void insertAll(List<Way> ways) {
		for (Way way : ways) {
			for (Node node : way.nodes) {
				insertNode(node);
			}
			insertWay(way);
		}
	}

	public List<Way> getAll() {
		List<Way> ways = new ArrayList<Way>();
		Map<Long, MapPos> nodes = new ArrayMap<Long, MapPos>();

		// get every node from the database
		Cursor cursor = database.query(DatabaseConstants.NODES_TABLE_NAME,
				null, null, null, null, null, null);
		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndex(DatabaseConstants.KEY_NODE_ID);
			int latIndex = cursor
					.getColumnIndex(DatabaseConstants.KEY_NODE_LAT);
			int lonIndex = cursor
					.getColumnIndex(DatabaseConstants.KEY_NODE_LON);
			do {
				// and put them in a map
				nodes.put(
						cursor.getLong(idIndex),
						new MapPos(cursor.getFloat(lonIndex), cursor
								.getFloat(latIndex)));
			} while (cursor.moveToNext());
		}

		// get every way from the database
		cursor = database.query(DatabaseConstants.WAYS_TABLE_NAME, null, null,
				null, null, null, null);
		if (cursor.moveToFirst()) {
			int idIndex = cursor.getColumnIndex(DatabaseConstants.KEY_WAY_ID);
			int occupancyIndex = cursor
					.getColumnIndex(DatabaseConstants.KEY_WAY_OCCUPANCY);
			do {
				// create way from the data we know so far
				Way way = new Way(cursor.getLong(idIndex),
						new ArrayList<Node>(), cursor.getFloat(occupancyIndex));
				// and query the database for nodes that are assigned to this
				// way
				Cursor innerCursor = database.query(
						DatabaseConstants.WAY_NODE_TABLE_NAME,
						new String[] { DatabaseConstants.KEY_WAY_NODE_NODEID },
						DatabaseConstants.KEY_WAY_NODE_WAYID + "=" + way.id, null,
						null, null, null);
				if (innerCursor.moveToFirst()) {
					int nodeIndex = innerCursor
							.getColumnIndex(DatabaseConstants.KEY_WAY_NODE_NODEID);
					do {
						// add the nodes with the given id from map
						Long nodeid = innerCursor.getLong(nodeIndex);
						way.nodes.add(new Node(nodeid, nodes.get(nodeid)));
					} while (innerCursor.moveToNext());
				}
				// add the way to the ways
				ways.add(way);
			} while (cursor.moveToNext());
		}
		return ways;
	}
}
