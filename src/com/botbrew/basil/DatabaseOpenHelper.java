package com.botbrew.basil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "botbrew";
	public static final String ID = "_id";
	public static final String T_PACKAGECACHE = "packagecache";
	public static final String C_NAME = "name";
	public static final String C_SUMMARY = "summary";
	public static final String C_INSTALLED = "installed";
	public static final String C_UPGRADABLE = "upgradable";
	public static final String T_PACKAGECACHEFTS = "packagecachefts";
	public DatabaseOpenHelper(Context context) {
		super(context,DB_NAME,null,DB_VERSION);
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "+T_PACKAGECACHE+" ("+C_NAME+" TEXT NOT NULL, "+C_SUMMARY+" TEXT NOT NULL, "+C_INSTALLED+" TEXT, "+C_UPGRADABLE+" TEXT);");
		db.execSQL("CREATE UNIQUE INDEX idx_"+T_PACKAGECACHE+"_"+C_NAME+" ON "+T_PACKAGECACHE+" ("+C_NAME+");");
		db.execSQL("CREATE INDEX idx_"+T_PACKAGECACHE+"_"+C_INSTALLED+" ON "+T_PACKAGECACHE+" ("+C_INSTALLED+");");
		db.execSQL("CREATE INDEX idx_"+T_PACKAGECACHE+"_"+C_UPGRADABLE+" ON "+T_PACKAGECACHE+" ("+C_UPGRADABLE+");");
		db.execSQL("CREATE VIRTUAL TABLE "+T_PACKAGECACHEFTS+" USING fts3("+C_NAME+" TEXT NOT NULL, "+C_SUMMARY+" TEXT NOT NULL);");
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(BotBrewApp.TAG,"Upgrading database. Existing contents will be lost. ["+oldVersion+"]->["+newVersion+"]");
		db.execSQL("DROP TABLE IF EXISTS "+T_PACKAGECACHE);
		db.execSQL("DROP TABLE IF EXISTS "+T_PACKAGECACHEFTS);
		onCreate(db);
	}
}
