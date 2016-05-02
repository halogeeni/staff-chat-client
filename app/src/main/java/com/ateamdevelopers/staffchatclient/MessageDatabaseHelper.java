package com.ateamdevelopers.staffchatclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MessageDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "MessageDatabaseHelper";

    private SQLiteDatabase mDb;

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + MessageContract.MessageEntry.TABLE_NAME + " (" +
                    MessageContract.MessageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    MessageContract.MessageEntry.COLUMN_NAME_FROM_USER + " INTEGER NOT NULL" + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_TO_USER + INT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP + INT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_BODY + TEXT_TYPE + COMMA_SEP +
                    MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP + " INTEGER NOT NULL" + ")";

    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + MessageContract.MessageEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Messages.db";

    public MessageDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void initDb() {
        mDb = getWritableDatabase();
        mDb.execSQL(SQL_DELETE_TABLE);
        mDb.execSQL(SQL_CREATE_TABLE);
        mDb.close();
    }

    public long insertMessage(ContentValues values) {
        Log.d(TAG, "in insertMessage()");
        mDb = getWritableDatabase();
        try {
            return mDb.insert(MessageContract.MessageEntry.TABLE_NAME, null, values);
        } finally {
            mDb.close();
        }
    }

    public List getMessageList() {
        List<Message> messages = new ArrayList<>();

        mDb = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                MessageContract.MessageEntry._ID,
                MessageContract.MessageEntry.COLUMN_NAME_FROM_USER,
                MessageContract.MessageEntry.COLUMN_NAME_TO_USER,
                MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP,
                MessageContract.MessageEntry.COLUMN_NAME_BODY,
                MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP
        };

        // How you want the results sorted in the resulting Cursor
        // TODO should be based on timestamp?
        String sortOrder = MessageContract.MessageEntry._ID + " ASC";

        Cursor c = mDb.query(
                MessageContract.MessageEntry.TABLE_NAME,      // The table to query
                projection,                                 // The columns to return
                null,                                       // The columns for the WHERE clause
                null,                                       // The values for the WHERE clause
                null,                                       // don't group the rows
                null,                                       // don't filter by row groups
                sortOrder                                   // The sort order
        );

        try {
            c.moveToFirst();

            int fromUserIdColumn = c.getColumnIndex(MessageContract.MessageEntry.COLUMN_NAME_FROM_USER);
            int toUserIdColumn = c.getColumnIndex(MessageContract.MessageEntry.COLUMN_NAME_TO_USER);
            int toGroupIdColumn = c.getColumnIndex(MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP);
            int timestampColumn = c.getColumnIndex(MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP);
            int bodyColumn = c.getColumnIndex(MessageContract.MessageEntry.COLUMN_NAME_BODY);

            while(c.getString(fromUserIdColumn) != null) {
                String body = c.getString(bodyColumn);
                int fromUserId = Integer.parseInt(c.getString(fromUserIdColumn));
                int toUserId = Integer.getInteger(c.getString(toUserIdColumn));
                int toGroupId = Integer.getInteger(c.getString(toGroupIdColumn));
                long timestamp = Long.parseLong(c.getString(timestampColumn));

                Message m = new Message(fromUserId, toUserId, body, toGroupId, timestamp);
                messages.add(m);

                c.moveToNext();
            }
        } finally {
            c.close();
            mDb.close();
        }

        return messages;
    }

    public Cursor getMessageCursor() {
        mDb = getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                MessageContract.MessageEntry._ID,
                MessageContract.MessageEntry.COLUMN_NAME_FROM_USER,
                MessageContract.MessageEntry.COLUMN_NAME_TO_USER,
                MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP,
                MessageContract.MessageEntry.COLUMN_NAME_BODY,
                MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP
        };

        // How you want the results sorted in the resulting Cursor
        // TODO should be based on timestamp?
        String sortOrder = MessageContract.MessageEntry._ID + " ASC";

        return mDb.query(
                MessageContract.MessageEntry.TABLE_NAME,      // The table to query
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

    // This database is only a cache for online data, so its upgrade/downgrade policy is
    // to simply to discard the data and start over

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
