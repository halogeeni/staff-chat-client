package com.ateamdevelopers.staffchatclient;

import android.provider.BaseColumns;

public final class DataContract {

    public static abstract class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME_USERID = "userId";
        public static final String COLUMN_NAME_FIRSTNAME = "firstname";
        public static final String COLUMN_NAME_LASTNAME = "lastname";
    }

    public static abstract class GroupEntry implements BaseColumns {
        public static final String TABLE_NAME = "rooms";
        public static final String COLUMN_NAME_GROUP_ID = "groupId";
        public static final String COLUMN_NAME_GROUP_NAME = "name";
    }

    public static abstract class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "messages";
        public static final String COLUMN_NAME_BODY = "body";
        public static final String COLUMN_NAME_FROM_USER = "fromUserId";
        public static final String COLUMN_NAME_TO_USER = "toUserId";
        public static final String COLUMN_NAME_TO_GROUP = "toGroupId";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }

}
