package com.ateamdevelopers.staffchatclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class GroupDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "GroupDatabaseHelper";

    private SQLiteDatabase mDb;

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ", ";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + DataContract.GroupEntry.TABLE_NAME + " (" +
                    DataContract.GroupEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    DataContract.GroupEntry.COLUMN_NAME_GROUP_ID + " INTEGER NOT NULL" + COMMA_SEP +
                    DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME + TEXT_TYPE + ")";

    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + DataContract.GroupEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "StaffChat.db";

    public GroupDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void initDb() {
        mDb = getWritableDatabase();
        mDb.execSQL(SQL_DELETE_TABLE);
        mDb.execSQL(SQL_CREATE_TABLE);
        mDb.close();
    }

    public long insertGroup(ContentValues values) {
        Log.d(TAG, "in insertGroup()");
        mDb = getWritableDatabase();
        return mDb.insert(DataContract.GroupEntry.TABLE_NAME, null, values);
    }

    public int deleteAll() {
        return mDb.delete(DataContract.GroupEntry.TABLE_NAME, null, null);
    }

    public Cursor getGroupCursor() {
        mDb = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                DataContract.GroupEntry._ID,
                DataContract.GroupEntry.COLUMN_NAME_GROUP_ID,
                DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = DataContract.GroupEntry.COLUMN_NAME_GROUP_ID + " ASC";

        return mDb.query(
                DataContract.GroupEntry.TABLE_NAME,    // The table to query
                projection,                                 // The columns to return
                null,                                       // The columns for the WHERE clause
                null,                                       // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                sortOrder                                   // The sort order
        );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "in onCreate()");
        db.execSQL(SQL_CREATE_TABLE);
    }

    // This database is only a cache for online data, so its upgrade policy is
    // to simply to discard the data and start over

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }
}
