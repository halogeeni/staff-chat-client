package com.ateamdevelopers.staffchatclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class UserDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "UserDatabaseHelper";

    private SQLiteDatabase mDb;

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + DataContract.UserEntry.TABLE_NAME + " (" +
                    DataContract.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                    DataContract.UserEntry.COLUMN_NAME_USERID + " INTEGER NOT NULL" + COMMA_SEP +
                    DataContract.UserEntry.COLUMN_NAME_USER_NAME + TEXT_TYPE + ")";

    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + DataContract.UserEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "StaffChat.db";

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void initDb() {
        mDb = getWritableDatabase();
        mDb.execSQL(SQL_DELETE_TABLE);
        mDb.execSQL(SQL_CREATE_TABLE);
        mDb.close();
    }

    public int deleteAll() {
        return mDb.delete(DataContract.UserEntry.TABLE_NAME, null, null);
    }

    public long insertUser(ContentValues values) {
        Log.d(TAG, "in insertUser()");
        mDb = getWritableDatabase();
        return mDb.insert(DataContract.UserEntry.TABLE_NAME, null, values);
    }

    public List getUserList() {
        List<User> users = new ArrayList<>();
        mDb = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                DataContract.UserEntry._ID,
                DataContract.UserEntry.COLUMN_NAME_USERID,
                DataContract.UserEntry.COLUMN_NAME_USER_NAME
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = DataContract.UserEntry.COLUMN_NAME_USERID + " ASC";

        Cursor c = mDb.query(
                DataContract.UserEntry.TABLE_NAME,    // The table to query
                projection,                                 // The columns to return
                null,                                       // The columns for the WHERE clause
                null,                                       // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                sortOrder                                   // The sort order
        );

        try {
            c.moveToFirst();

            int userIdColumn = c.getColumnIndex(DataContract.UserEntry.COLUMN_NAME_USERID);
            int nameColumn = c.getColumnIndex(DataContract.UserEntry.COLUMN_NAME_USER_NAME);

            while(c.getString(userIdColumn) != null) {
                int userId = Integer.parseInt(c.getString(userIdColumn));
                String name = c.getString(nameColumn);

                User u = new User(userId, name);
                users.add(u);

                c.moveToNext();
            }
        } finally {
            c.close();
        }
        return users;
    }

    public Cursor getUserCursor() {
        mDb = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                DataContract.UserEntry._ID,
                DataContract.UserEntry.COLUMN_NAME_USERID,
                DataContract.UserEntry.COLUMN_NAME_USER_NAME
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = DataContract.UserEntry.COLUMN_NAME_USERID + " ASC";

        return mDb.query(
                DataContract.UserEntry.TABLE_NAME,          // The table to query
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
