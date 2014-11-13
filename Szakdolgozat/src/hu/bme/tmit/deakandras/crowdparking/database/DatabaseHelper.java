package hu.bme.tmit.deakandras.crowdparking.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, DatabaseConstants.DATABASE_NAME, null,
				DatabaseConstants.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DatabaseConstants.CREATE_TABLE_NODES);
		db.execSQL(DatabaseConstants.CREATE_TABLE_WAYS);
		db.execSQL(DatabaseConstants.CREATE_TABLE_WAY_NODE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(DatabaseConstants.DROP_TABLE_NODES);
		db.execSQL(DatabaseConstants.DROP_TABLE_WAYS);
		db.execSQL(DatabaseConstants.DROP_TABLE_WAY_NODE);
		onCreate(db);

	}

}
