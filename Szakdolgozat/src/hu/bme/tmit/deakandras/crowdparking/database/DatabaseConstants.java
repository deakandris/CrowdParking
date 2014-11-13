package hu.bme.tmit.deakandras.crowdparking.database;

public class DatabaseConstants {

	static final String DATABASE_NAME = "database.db";
	static final int DATABASE_VERSION = 1;
	static final String NODES_TABLE_NAME = "nodes";
	static final String KEY_NODE_ID = "id";
	static final String KEY_NODE_LAT = "lat";
	static final String KEY_NODE_LON = "lon";
	static final String WAYS_TABLE_NAME = "ways";
	static final String KEY_WAY_ID = "id";
	static final String WAY_NODE_TABLE_NAME = "way_node";
	static final String KEY_WAY_OCCUPANCY = "occupancy";
	static final String KEY_WAY_NODE_NODEID = "nodeid";
	static final String KEY_WAY_NODE_WAYID = "wayid";

	static final String CREATE_TABLE_WAY_NODE = "CREATE TABLE IF NOT EXISTS "
			+ WAY_NODE_TABLE_NAME + " (" + KEY_WAY_NODE_NODEID
			+ " INTEGER NOT NULL, " + KEY_WAY_NODE_WAYID
			+ " INTEGER NOT NULL, " + "FOREIGN KEY(" + KEY_WAY_NODE_NODEID
			+ ") " + "REFERENCES " + NODES_TABLE_NAME + "(" + KEY_NODE_ID
			+ "), " + "FOREIGN KEY(" + KEY_WAY_NODE_WAYID + ") "
			+ "REFERENCES " + WAYS_TABLE_NAME + "(" + KEY_WAY_ID + "));";

	static final String CREATE_TABLE_NODES = "CREATE TABLE IF NOT EXISTS "
			+ NODES_TABLE_NAME + " (" + KEY_NODE_ID
			+ " INTEGER PRIMARY KEY NOT NULL, " + KEY_NODE_LAT
			+ " REAL NOT NULL, " + KEY_NODE_LON + " REAL NOT NULL);";

	static final String CREATE_TABLE_WAYS = "CREATE TABLE IF NOT EXISTS "
			+ WAYS_TABLE_NAME + " (" + KEY_WAY_ID
			+ " INTEGER PRIMARY KEY NOT NULL, " + KEY_WAY_OCCUPANCY
			+ " REAL CHECK(" + KEY_WAY_OCCUPANCY + ">=0 AND "
			+ KEY_WAY_OCCUPANCY + "<=100) DEFAULT 0);";

	static final String DROP_TABLE_WAY_NODE = "DROP TABLE IF EXISTS "
			+ WAY_NODE_TABLE_NAME + ";";
	
	static final String DROP_TABLE_NODES = "DROP TABLE IF EXISTS "
			+ NODES_TABLE_NAME + ";";
	
	static final String DROP_TABLE_WAYS = "DROP TABLE IF EXISTS "
			+ WAYS_TABLE_NAME + ";";
}
