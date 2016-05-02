package com.ateamdevelopers.staffchatclient;

import android.provider.BaseColumns;

public final class MessageContract {
    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "message";

        public static final String COLUMN_NAME_BODY = "body";
        public static final String COLUMN_NAME_FROM_USER = "fromUserId";
        public static final String COLUMN_NAME_TO_USER = "toUserId";
        public static final String COLUMN_NAME_TO_GROUP = "toGroupId";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }
}
