package it.startup.sendudes.utils.Db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.security.Key;

public class FilesDbAdapter {
    private static final String LOG_TAG = FilesDbAdapter.class.getSimpleName();

    private Context context;
    private SQLiteDatabase database;
    private DbHelper dbhelper;

    private static final String DATABASE_TABLE = "files";

    private static final String KEY_FILE_ID = "_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_SIZE= "size";
    private static final String KEY_DATE_TIME= "dateTime";
    private static final String KEY_SENT= "sent";
    private static final String KEY_PATH = "path";

    public FilesDbAdapter(Context con){
        this.context = con;
    }
    public FilesDbAdapter open() throws SQLException {
        dbhelper = new DbHelper(context);
        database = dbhelper.getWritableDatabase();
        return this;
    }
    private ContentValues createFileValues(String name, String size, String dateTime, int sent, String path){
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_SIZE, size);
        values.put(KEY_DATE_TIME, dateTime);
        values.put(KEY_SENT, sent);
        values.put(KEY_PATH, path);

        return values;
    }
    public long createFileRow(String name, String size, String dateTime, int sent, String path){
        ContentValues initVals = createFileValues(name, size, dateTime, sent, path);
        return database.insertOrThrow(DATABASE_TABLE, null, initVals);
        // returns ID of row created or returns -1 if error occurs
    }
    public Cursor fetchAllFiles(){
        return database.query(DATABASE_TABLE, new String[] {
            KEY_FILE_ID,
            KEY_NAME,
            KEY_SIZE,
            KEY_DATE_TIME,
            KEY_SENT,
            KEY_PATH
        }, null, null, null, null, null);
    }
    public void close() {
        dbhelper.close();
    }
}
