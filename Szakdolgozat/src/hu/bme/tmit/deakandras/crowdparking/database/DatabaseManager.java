package hu.bme.tmit.deakandras.crowdparking.database;

import hu.bme.tmit.deakandras.crowdparking.activity.data.Road;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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
	 * @param id
	 *            the identifier of the node
	 * @param node
	 *            a {@link MapPos} object where <b>x</b> represents longitude
	 *            and <b>y</b> represents latitude
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insertNode(Long id, MapPos node) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_NODE_ID, id);
		values.put(DatabaseConstants.KEY_NODE_LAT, node.y);
		values.put(DatabaseConstants.KEY_NODE_LON, node.x);
		return database
				.insert(DatabaseConstants.NODES_TABLE_NAME, null, values);
	}

	/**
	 * Insert a single way element to the database with default occupancy (0).
	 * 
	 * @param id
	 *            the identifier of the way
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insertWay(Long id) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_WAY_ID, id);
		values.put(DatabaseConstants.KEY_WAY_OCCUPANCY, 0);
		return database.insert(DatabaseConstants.WAYS_TABLE_NAME, null, values);
	}

	/**
	 * Insert a single way element to the database.
	 * 
	 * @param id
	 *            the identifier of the way
	 * @param occupancy
	 *            the rate of parking slots occupied in percent
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	public long insertWay(Long id, Double occupancy) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_WAY_ID, id);
		values.put(DatabaseConstants.KEY_WAY_OCCUPANCY, occupancy);
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
	public long insertAssignment(Long nodeid, Long wayid) {
		ContentValues values = new ContentValues();
		values.put(DatabaseConstants.KEY_NODE_ID, nodeid);
		values.put(DatabaseConstants.KEY_WAY_ID, wayid);
		return database.insert(DatabaseConstants.WAY_NODE_TABLE_NAME, null,
				values);
	}

	public int insertAll(List<Road> ways) {
		for (Road way : ways) {
			for (MapPos node : way.getNodes()) {
				// TODO
			}
		}
		return 0;
	}

}
