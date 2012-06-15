package com.botbrew.basil;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

public class PackageCacheProvider extends ContentProvider {
	private DatabaseOpenHelper mDB;
	private static final String AUTHORITY = "com.botbrew.basil.data.packagecacheprovider";
	private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	public static enum ContentUri {
		NULL("",""),
		CACHE_BASE("cache",ContentResolver.CURSOR_DIR_BASE_TYPE+"/cache"),
		CACHE_ITEM("cache/*",ContentResolver.CURSOR_ITEM_BASE_TYPE+"/cache"),
		CACHE_SUGGEST(SearchManager.SUGGEST_URI_PATH_QUERY,ContentResolver.CURSOR_DIR_BASE_TYPE+"/cache"),
		CACHE_SEARCH(SearchManager.SUGGEST_URI_PATH_QUERY+"/*",ContentResolver.CURSOR_ITEM_BASE_TYPE+"/cache"),
		UPDATE_REFRESH("update/refresh",ContentResolver.CURSOR_DIR_BASE_TYPE+"/cache"),
		UPDATE_RELOAD("update/reload",ContentResolver.CURSOR_DIR_BASE_TYPE+"/cache");
		public final String path;
		public final String type;
		public final Uri uri;
		ContentUri(final String path, final String type) {
			this.path = path;
			this.type = type;
			this.uri = Uri.parse("content://"+AUTHORITY+"/"+path);
			final int i = ordinal();
			if(i > 0) sUriMatcher.addURI(AUTHORITY,path,i);
		}
	}
	private static ContentUri[] sContentUriValues = ContentUri.values();
	@Override
	public boolean onCreate() {
		mDB = new DatabaseOpenHelper(getContext());
		return true;
	}
	@Override
	public String getType(Uri uri) {
		try {
			int match = sUriMatcher.match(uri);
			if(match > 0) return sContentUriValues[match].type;
		} catch(ArrayIndexOutOfBoundsException ex) {}
		throw new IllegalArgumentException("Unknown URI "+uri);
	}
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db;
		Cursor c;
		final int match = sUriMatcher.match(uri);
		if(match > 0) switch(sContentUriValues[match]) {
			case CACHE_BASE:
				db = mDB.getReadableDatabase();
				c = db.query(DatabaseOpenHelper.T_PACKAGECACHE,projection,selection,selectionArgs,null,null,sortOrder);
				if(c != null) c.setNotificationUri(getContext().getContentResolver(),ContentUri.CACHE_BASE.uri);
				return c;
			case CACHE_ITEM:
				db = mDB.getReadableDatabase();
				c = db.query(DatabaseOpenHelper.T_PACKAGECACHE,projection,DatabaseOpenHelper.C_NAME+"=?",new String[] {uri.getLastPathSegment()},null,null,null);
				if(c != null) c.setNotificationUri(getContext().getContentResolver(),ContentUri.CACHE_BASE.uri);
				return c;
			case CACHE_SUGGEST:
				if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs must be provided for the Uri: "+uri);
				db = mDB.getReadableDatabase();
				c = db.query(
					DatabaseOpenHelper.T_PACKAGECACHEFTS,new String[] {
						DatabaseOpenHelper.C_NAME+" AS _id",
						DatabaseOpenHelper.C_NAME+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_1,
						DatabaseOpenHelper.C_SUMMARY+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_2,
					//	DatabaseOpenHelper.C_NAME+" AS "+SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
						DatabaseOpenHelper.C_NAME+" AS "+SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
					},
					DatabaseOpenHelper.T_PACKAGECACHEFTS+" MATCH ?",
					new String[] {selectionArgs[0].toLowerCase().replaceAll("(\\S)-","$1 ")+"*"},
					null,null,sortOrder
				);
				if(c != null) c.setNotificationUri(getContext().getContentResolver(),ContentUri.CACHE_BASE.uri);
				return c;
			case CACHE_SEARCH:
				if(selectionArgs == null) throw new IllegalArgumentException("selectionArgs must be provided for the Uri: "+uri);
				db = mDB.getReadableDatabase();
				c = db.query(
					DatabaseOpenHelper.T_PACKAGECACHEFTS,new String[] {
						DatabaseOpenHelper.C_NAME+" AS _id",
						DatabaseOpenHelper.C_NAME+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_1,
						DatabaseOpenHelper.C_SUMMARY+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_2
					},
					DatabaseOpenHelper.T_PACKAGECACHEFTS+" MATCH ?",
					new String[] {selectionArgs[0].toLowerCase().replaceAll("(\\S)-","$1 ")},
					null,null,sortOrder
				);
				if(c != null) c.setNotificationUri(getContext().getContentResolver(),ContentUri.CACHE_BASE.uri);
				return c;
		}
		throw new IllegalArgumentException("Unsupported URI "+uri);
	}
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		final SQLiteDatabase db = mDB.getWritableDatabase();
		final int match = sUriMatcher.match(uri);
		if(match > 0) switch(sContentUriValues[match]) {
			case UPDATE_RELOAD:
				db.beginTransaction();
				try {
					SQLiteStatement stmt1 = db.compileStatement("DELETE FROM "+DatabaseOpenHelper.T_PACKAGECACHE);
					stmt1.execute();
					SQLiteStatement stmt2 = db.compileStatement("DELETE FROM "+DatabaseOpenHelper.T_PACKAGECACHEFTS);
					stmt2.execute();
					stmt1 = db.compileStatement(
						"INSERT INTO "+DatabaseOpenHelper.T_PACKAGECACHE+" ("
							+DatabaseOpenHelper.C_NAME+","
							+DatabaseOpenHelper.C_SUMMARY+","
							+DatabaseOpenHelper.C_INSTALLED+","
							+DatabaseOpenHelper.C_UPGRADABLE
						+") values "+"(?,?,?,?)"
					);
					stmt2 = db.compileStatement(
						"INSERT INTO "+DatabaseOpenHelper.T_PACKAGECACHEFTS+" ("
							+DatabaseOpenHelper.C_NAME+","
							+DatabaseOpenHelper.C_SUMMARY
						+") values "+"(?,?)"
					);
					for(ContentValues value: values) {
						stmt1.bindString(1,value.getAsString(DatabaseOpenHelper.C_NAME));
						stmt1.bindString(2,value.getAsString(DatabaseOpenHelper.C_SUMMARY));
						stmt1.bindString(3,value.getAsString(DatabaseOpenHelper.C_INSTALLED));
						stmt1.bindString(4,value.getAsString(DatabaseOpenHelper.C_UPGRADABLE));
						stmt1.execute();
						stmt2.bindString(1,value.getAsString(DatabaseOpenHelper.C_NAME));
						stmt2.bindString(2,value.getAsString(DatabaseOpenHelper.C_SUMMARY));
						stmt2.execute();
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
					getContext().getContentResolver().notifyChange(ContentUri.CACHE_BASE.uri,null);
				}
				return values.length;
			case UPDATE_REFRESH:
				db.beginTransaction();
				try {
					SQLiteStatement stmt = db.compileStatement(
						"UPDATE "+DatabaseOpenHelper.T_PACKAGECACHE+" SET "
							+DatabaseOpenHelper.C_INSTALLED+"='',"
							+DatabaseOpenHelper.C_UPGRADABLE+"=''"
					);
					stmt.execute();
					stmt = db.compileStatement(
						"UPDATE "+DatabaseOpenHelper.T_PACKAGECACHE+" SET "
							+DatabaseOpenHelper.C_INSTALLED+"=?,"
							+DatabaseOpenHelper.C_UPGRADABLE+"=?"
						+" WHERE "+DatabaseOpenHelper.C_NAME+"=?"
					);
					for(ContentValues value: values) {
						stmt.bindString(1,value.getAsString(DatabaseOpenHelper.C_INSTALLED));
						stmt.bindString(2,value.getAsString(DatabaseOpenHelper.C_UPGRADABLE));
						stmt.bindString(3,value.getAsString(DatabaseOpenHelper.C_NAME));
						stmt.execute();
					}
					db.setTransactionSuccessful();
				} finally {
					db.endTransaction();
					getContext().getContentResolver().notifyChange(ContentUri.CACHE_BASE.uri,null);
				}
				return values.length;
		}
		throw new IllegalArgumentException("Unsupported URI "+uri);
	}
}
