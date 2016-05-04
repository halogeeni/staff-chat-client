package com.ateamdevelopers.staffchatclient;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class UserContentProvider extends ContentProvider {
    public static final String PROVIDER_NAME = "com.ateamdevelopers.staffchatclient.UserContentProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/users");
    private static final int USERS = 1;

    private static final UriMatcher uriMatcher ;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "users", USERS);
    }

    private UserDatabaseHelper mUserDb;


    @Override
    public boolean onCreate() {
        mUserDb = new UserDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(uriMatcher.match(uri) == USERS) {
            Cursor cursor = mUserDb.getUserCursor();
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        } else {
            return null;
        }
    }
/*
    public Cursor customQuery(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(uriMatcher.match(uri) == USERS) {
            Cursor cursor = mUserDb.getCustomUserCursor(projection, selection, selectionArgs, sortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        } else {
            return null;
        }
    }
*/
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            long id = mUserDb.insertUser(values);
            Uri returnUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(returnUri, null);
            return returnUri;
        } catch(Exception e) {
            return null;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
