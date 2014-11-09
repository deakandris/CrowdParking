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
	static final String WAY_NODE_TABLE_NAME = "way-node";
	static final String KEY_WAY_OCCUPANCY = "occupancy";

	static final String CREATE_DATABASE = "CREATE TABLE IF NOT EXISTS "
			+ NODES_TABLE_NAME + " (" + KEY_NODE_ID
			+ " INTEGER PRIMARY KEY NOT NULL, " + KEY_NODE_LAT
			+ " REAL NOT NULL, " + KEY_NODE_LON + " REAL NOT NULL); "
			+ "CREATE TABLE IF NOT EXISTS " + WAYS_TABLE_NAME + " ("
			+ KEY_WAY_ID + " INTEGER PRIMARY KEY NOT NULL, "
			+ KEY_WAY_OCCUPANCY + " REAL CHECK(" + KEY_WAY_OCCUPANCY
			+ ">=0 AND " + KEY_WAY_OCCUPANCY + "<=100) DEFAULT 0; "
			+ "CREATER TABLE IF NOT EXISTS " + WAY_NODE_TABLE_NAME + " ("
			+ "FOREIGN KEY(" + KEY_NODE_ID + ") " + "REFERENCES "
			+ NODES_TABLE_NAME + "(" + KEY_NODE_ID + "), " + "FOREIGN KEY("
			+ KEY_WAY_ID + ") " + "REFERENCES " + WAYS_TABLE_NAME + "("
			+ KEY_WAY_ID + ")); ";

	static final String DROP_ALL_TABLES = "DROP TABLE IF EXISTS "
			+ NODES_TABLE_NAME + "; " + "DROP TABLE IF EXISTS "
			+ WAYS_TABLE_NAME + "; " + "DROP TABLE IF EXISTS "
			+ WAY_NODE_TABLE_NAME + "; ";
}
