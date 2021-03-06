package com.ateamdevelopers.staffchatclient;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class MessageContentProvider extends ContentProvider {
    public static final String PROVIDER_NAME = "com.ateamdevelopers.staffchatclient.MessageContentProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/messages");

    // Constant to identify the requested operation
    private static final int MESSAGES = 1;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "messages", MESSAGES);
    }

    // content provider does the database operations by this object
    private MessageDatabaseHelper mMessageDb;

    // callback method which is invoked when the content provider is starting up
    @Override
    public boolean onCreate() {
        mMessageDb = new MessageDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(uriMatcher.match(uri) == MESSAGES){
            // ignoring sortOrder as we are always sorting per timestamps
            Cursor cursor = mMessageDb.getMessageCursor(projection, selection, selectionArgs);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        } else {
            return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rows = mMessageDb.deleteAll();
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return rows;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            long id = mMessageDb.insertMessage(values);
            getContext().getContentResolver().notifyChange(CONTENT_URI, null);
            return CONTENT_URI;
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

}
