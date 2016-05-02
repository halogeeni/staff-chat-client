package com.ateamdevelopers.staffchatclient;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

public class GroupContentProvider extends ContentProvider {

    public static final String PROVIDER_NAME = "com.ateamdevelopers.staffchatclient.GroupContentProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME + "/groups");

    // Constant to identify the requested operation
    private static final int GROUPS = 1;

    private static final UriMatcher uriMatcher ;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "groups", GROUPS);
    }

    // content provider does the database operations by this object
    private GroupDatabaseHelper mGroupDb;

    @Override
    public boolean onCreate() {
        mGroupDb = new GroupDatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if(uriMatcher.match(uri) == GROUPS){
            Cursor cursor = mGroupDb.getGroupCursor();
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
            return cursor;
        } else{
            return null;
        }
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            long id = mGroupDb.insertGroup(values);
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
