package it.startup.sendudes.utils.Db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "sendudesDB.db";
    private static final int VERSION = 2;
    private static final String CREATE_DB_TABLE = "CREATE TABLE files (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "size TEXT NOT NULL," +
            "dateTime DATETIME NOT NULL," +
            "sent INTEGER NOT NULL," +
            "path TEXT NOT NULL" +
            ")";
    public DbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DB_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS files");
        onCreate(db);
    }
}
