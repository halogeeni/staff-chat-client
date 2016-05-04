package com.ateamdevelopers.staffchatclient;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = "MainActivity";
    private final int MESSAGE_LOADER_ID = 0x01, GROUP_LOADER_ID = 0x02, USER_LOADER_ID = 0x03;

    private int currentUser;
    private int groupSelection;
    private int userSelection;

    private Channel channel;

    private String[] messageProjection = {
            DataContract.MessageEntry._ID,
            DataContract.MessageEntry.COLUMN_NAME_FROM_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_GROUP,
            DataContract.MessageEntry.COLUMN_NAME_BODY,
            DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP
    };

    private String[] userProjection = {
            DataContract.MessageEntry._ID,
            DataContract.MessageEntry.COLUMN_NAME_FROM_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_GROUP,
            DataContract.MessageEntry.COLUMN_NAME_BODY,
            DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP
    };

    private String[] groupProjection = {
            DataContract.MessageEntry._ID,
            DataContract.MessageEntry.COLUMN_NAME_FROM_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_USER,
            DataContract.MessageEntry.COLUMN_NAME_TO_GROUP,
            DataContract.MessageEntry.COLUMN_NAME_BODY,
            DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP
    };

    // REST URLs
    private final String postMessageUrl = "http://10.0.2.2:8080/StaffChat/webresources/messages/add";
    private final String messageUrl = "http://10.0.2.2:8080/StaffChat/webresources/messages";
    private final String userUrl = "http://10.0.2.2:8080/StaffChat/webresources/users";
    private final String groupUrl = "http://10.0.2.2:8080/StaffChat/webresources/groups";

    /*
    // For Joel's computer only, cos why not
    private final String postMessageUrl = "http://192.168.42.68:8080/RESTfulWebApp/webresources/messages/add";
    private final String messageUrl = "http://192.168.42.68:8080/RESTfulWebApp/webresources/messages";
    private final String userUrl = "http://192.168.42.68:8080/RESTfulWebApp/webresources/users";
    private final String groupUrl = "http://192.168.42.68:8080/RESTfulWebApp/webresources/groups";
    */

    private MessageDatabaseHelper mMessageDbHelper;
    private GroupDatabaseHelper mGroupDbHelper;
    private UserDatabaseHelper mUserDbHelper;
    private SimpleCursorAdapter mMessageAdapter, mUserAdapter, mGroupAdapter;
    private DrawerLayout mDrawer;
    private int mLastMessageCount, mLastUserCount, mLastGroupCount;

    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // open broadcast chat by default
        channel = Channel.CHANNEL_BROADCAST;

        // get userid input to loginactivity
        currentUser = getIntent().getIntExtra("userid", -1);

        // default contact selection values
        groupSelection = 0;
        userSelection = 0;
        mLastMessageCount = 0;
        mLastUserCount = 0;
        mLastGroupCount = 0;

        // flush the databases on login
        mMessageDbHelper = new MessageDatabaseHelper(this);
        mGroupDbHelper = new GroupDatabaseHelper(this);
        mUserDbHelper = new UserDatabaseHelper(this);
        mMessageDbHelper.initDb();
        mGroupDbHelper.initDb();
        mUserDbHelper.initDb();

        // get navigation drawer
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        // set action bar title
        getSupportActionBar().setTitle("Broadcast");

        // set cursor adapters
        mMessageAdapter = new SimpleCursorAdapter(this,
                R.layout.message_list_item, null,
                new String[]{DataContract.MessageEntry.COLUMN_NAME_FROM_USER,
                        DataContract.MessageEntry.COLUMN_NAME_BODY,
                        DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP},
                new int[]{R.id.messageUsername, R.id.messageBody, R.id.messageTimestamp}, CursorAdapter.NO_SELECTION);

        mUserAdapter = new SimpleCursorAdapter(this,
                R.layout.user_list_item, null,
                new String[]{DataContract.UserEntry.COLUMN_NAME_USER_NAME},
                new int[]{R.id.userName}, CursorAdapter.NO_SELECTION);

        mGroupAdapter = new SimpleCursorAdapter(this,
                R.layout.group_list_item, null,
                new String[]{DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME},
                new int[]{R.id.groupName}, CursorAdapter.NO_SELECTION);

        // setup a view binder to:
        //  * convert timestamps --> date strings
        //  * userids --> full names
        mMessageAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View aView, Cursor aCursor, int aColumnIndex) {
                switch (aView.getId()) {
                    case R.id.messageUsername:
                        int userId = aCursor.getInt(aColumnIndex);
                        Cursor usernames =
                                getContentResolver().query(UserContentProvider.CONTENT_URI,
                                        new String[]{DataContract.UserEntry.COLUMN_NAME_USERID,
                                                DataContract.UserEntry.COLUMN_NAME_USER_NAME},
                                        DataContract.UserEntry.COLUMN_NAME_USERID + " = ?",
                                        new String[]{String.valueOf(userId)}, null);

                        usernames.moveToPosition(userId);

                        try {
                            if (usernames.getCount() > 0) {
                                ((TextView) aView).setText(usernames.getString(usernames.getColumnIndex(DataContract.UserEntry.COLUMN_NAME_USER_NAME)));
                                return true;
                            } else {
                                return false;
                            }
                        } finally {
                            usernames.close();
                        }

                    case R.id.messageBody:
                        String body = StringEscapeUtils.unescapeHtml4(aCursor.getString(aColumnIndex));
                        ((TextView) aView).setText(body);
                        return true;

                    case R.id.messageTimestamp:
                        try {
                            long timestamp = aCursor.getLong(aColumnIndex);
                            Date d = new Date(timestamp);
                            SimpleDateFormat s = new SimpleDateFormat();
                            ((TextView) aView).setText(s.format(d));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                }
                // fallback - not desired view...
                return false;
            }
        });

        ListView messageListView = (ListView) findViewById(R.id.messageList);
        ListView userListView = (ListView) findViewById(R.id.drawer_users);
        ListView groupListView = (ListView) findViewById(R.id.drawer_groups);
        messageListView.setAdapter(mMessageAdapter);
        userListView.setAdapter(mUserAdapter);
        groupListView.setAdapter(mGroupAdapter);

        // start polling, store message polling task
        mTimer = startMessagePollingTask();
        startUserPollingTask();
        startGroupPollingTask();

        getLoaderManager().initLoader(MESSAGE_LOADER_ID, null, this);
        getLoaderManager().initLoader(GROUP_LOADER_ID, null, this);
        getLoaderManager().initLoader(USER_LOADER_ID, null, this);

        // contact lists' itemClickListeners
        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                userSelection = position;
                channel = Channel.CHANNEL_PRIVATE;
                // cancel active polling task
                mTimer.cancel();
                // clear message db table & counter
                getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
                mLastMessageCount = 0;
                // start polling
                mTimer = startMessagePollingTask();
                // close navigation drawer
                mDrawer.closeDrawers();
                // dynamically change action bar title
                TextView t = (TextView)((LinearLayout) view).getChildAt(0);
                getSupportActionBar().setTitle(t.getText().toString());
                getSupportActionBar().setSubtitle("private chat");
            }
        });

        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "groupList item # " + id + " clicked");
                groupSelection = position;
                Log.d(TAG, "groupSelection now: " + groupSelection);
                channel = Channel.CHANNEL_GROUP;
                mTimer.cancel();
                getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
                mLastMessageCount = 0;
                mTimer = startMessagePollingTask();
                mDrawer.closeDrawers();
                TextView t = (TextView)((LinearLayout) view).getChildAt(0);
                getSupportActionBar().setTitle(t.getText().toString());
                getSupportActionBar().setSubtitle("group chat");
            }
        });
    }

    public void broadcastButtonClicked(View v) {
        channel = Channel.CHANNEL_BROADCAST;
        // cancel active polling task
        mTimer.cancel();
        // clear message db table & counter
        getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
        mLastMessageCount = 0;
        // start new polling
        mTimer = startMessagePollingTask();
        // close navigation drawer
        mDrawer.closeDrawers();
        // hardcoded action bar title
        getSupportActionBar().setTitle("Broadcast");
        getSupportActionBar().setSubtitle("");
    }

    public void sendButtonClicked(View v) {
        new UploadXmlTask().execute(postMessageUrl);
    }

    // message polling timer
    protected Timer startMessagePollingTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new DownloadMessageXmlTask().execute(messageUrl);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 1000); //execute every second
        return timer;
    }

    // 60 sec user polling timer
    protected void startUserPollingTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new DownloadUserXmlTask().execute(userUrl);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 60000); // execute every 60 secs
    }

    // 120 sec group polling timer
    protected void startGroupPollingTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new DownloadGroupXmlTask().execute(groupUrl);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 120000); // execute every 120 secs
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case MESSAGE_LOADER_ID:
                return new CursorLoader(this, MessageContentProvider.CONTENT_URI, messageProjection, null, null, null);
            case GROUP_LOADER_ID:
                return new CursorLoader(this, GroupContentProvider.CONTENT_URI, groupProjection, null, null, null);
            case USER_LOADER_ID:
                return new CursorLoader(this, UserContentProvider.CONTENT_URI, userProjection, null, null, null);
        }

        // should be unreachable...
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case MESSAGE_LOADER_ID:
                mMessageAdapter.swapCursor(data);
                mMessageAdapter.notifyDataSetChanged();
                break;
            case GROUP_LOADER_ID:
                mGroupAdapter.swapCursor(data);
                mGroupAdapter.notifyDataSetChanged();
                break;
            case USER_LOADER_ID:
                mUserAdapter.swapCursor(data);
                mUserAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case MESSAGE_LOADER_ID:
                mMessageAdapter.swapCursor(null);
                mMessageAdapter.notifyDataSetChanged();
                break;
            case GROUP_LOADER_ID:
                mGroupAdapter.swapCursor(null);
                mGroupAdapter.notifyDataSetChanged();
                break;
            case USER_LOADER_ID:
                mUserAdapter.swapCursor(null);
                mUserAdapter.notifyDataSetChanged();
                break;
        }
    }

    private class DownloadMessageXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadMessageXml(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }
    }

    private class DownloadGroupXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadGroupXml(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }
    }

    private class DownloadUserXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadUserXml(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }
    }

    private String downloadMessageXml(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        XmlMessageParser messageParser = new XmlMessageParser();
        String result = "";

        switch (channel) {
            case CHANNEL_BROADCAST:
                urlString += "/broadcast";
                break;
            case CHANNEL_PRIVATE:
                urlString += ("/" + currentUser + "/private/" + userSelection);
                break;
            case CHANNEL_GROUP:
                urlString += ("/group/" + groupSelection);
                break;
        }

        try {
            // connect to the server & get stream
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream();
            stream = new BufferedInputStream(conn.getInputStream());

            List<Message> messageList = messageParser.parse(stream);

            if (messageList == null || messageList.size() == 0) {
                // no messages or something went wrong with the parsing
                getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
            } else if (messageList.size() > mLastMessageCount) {
                int newMessages = messageList.size() - mLastMessageCount;
                // insert new values
                ContentValues values = new ContentValues();
                for (int i = 0; i < newMessages; i++) {
                    Message m = messageList.get(messageList.size() - i - 1);
                    values.put(DataContract.MessageEntry.COLUMN_NAME_FROM_USER, m.getFromUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_USER, m.getToUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_GROUP, m.getToGroupId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_BODY, m.getBody());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP, m.getTimestamp());
                    getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
                }
                // update last poll message counter
                mLastMessageCount = messageList.size();
            } else if (messageList.size() < mLastMessageCount) {
                // something went terribly wrong... lets clear messages and start over
                getContentResolver().delete(MessageContentProvider.CONTENT_URI, null, null);
                mLastMessageCount = 0;
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return result;
    }

    private String downloadUserXml(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        XmlUserParser userParser = new XmlUserParser();
        String result = "";

        try {
            // connect to the server & get stream
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream();
            stream = new BufferedInputStream(conn.getInputStream());

            List<User> userList = userParser.parse(stream);

            if (userList == null || userList.size() == 0) {
                // no users or something went wrong with the parsing
                getContentResolver().delete(UserContentProvider.CONTENT_URI, null, null);
            } else if (userList.size() > mLastUserCount) {
                int newUsers = userList.size() - mLastUserCount;
                // insert new values
                ContentValues values = new ContentValues();
                for (int i = 0; i < newUsers; i++) {
                    User u = userList.get(userList.size() - i - 1);
                    values.put(DataContract.UserEntry.COLUMN_NAME_USERID, u.getId());
                    values.put(DataContract.UserEntry.COLUMN_NAME_USER_NAME, u.getName());
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, values);
                }
                // update last poll user counter
                mLastUserCount = userList.size();
            } else if (userList.size() < mLastUserCount) {
                // something went terribly wrong... lets clear users and start over
                getContentResolver().delete(UserContentProvider.CONTENT_URI, null, null);
                mLastUserCount = 0;
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return result;
    }


    private String downloadGroupXml(String urlString) throws XmlPullParserException, IOException {
        InputStream stream = null;
        XmlGroupParser groupParser = new XmlGroupParser();
        String result = "";

        try {
            // connect to the server & get stream
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream();
            stream = new BufferedInputStream(conn.getInputStream());

            List<Group> groupList = groupParser.parse(stream);

            if (groupList == null || groupList.size() == 0) {
                // no groups or something went wrong with the parsing
                getContentResolver().delete(GroupContentProvider.CONTENT_URI, null, null);
            } else if (groupList.size() > mLastGroupCount) {
                int newGroups = groupList.size() - mLastGroupCount;
                // insert new values
                ContentValues values = new ContentValues();
                for (int i = 0; i < newGroups; i++) {
                    Group g = groupList.get(groupList.size() - i - 1);
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_ID, g.getId());
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME, g.getName());
                    getContentResolver().insert(GroupContentProvider.CONTENT_URI, values);
                }
                // update last poll group counter
                mLastGroupCount = groupList.size();
            } else if (groupList.size() < mLastGroupCount) {
                // something went terribly wrong... lets clear groups and start over
                getContentResolver().delete(GroupContentProvider.CONTENT_URI, null, null);
                mLastGroupCount = 0;
            }
        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return result;
    }


    private class UploadXmlTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... urls) {
            try {
                uploadXml(urls[0]);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void uploadXml(String urlString) throws XmlPullParserException, IOException {
        // disable button on POST
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.sendButton).setEnabled(false);
            }
        });

        EditText messageField = (EditText) findViewById(R.id.writeMessage);
        String messageBody = messageField.getText().toString();
        OutputStream output = null;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            String body = "<message><body><text>" + messageBody + "</text></body>" +
                    "<channel>" + channel.toString() + "</channel><fromUserId>" + currentUser +
                    "</fromUserId><toUserId>" + userSelection + "</toUserId><toGroupId>" +
                    groupSelection + "</toGroupId><messageId>-1</messageId><timestamp>-1</timestamp></message>";

            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setFixedLengthStreamingMode(body.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/xml; charset=\"utf-8\"");

            output = new BufferedOutputStream(conn.getOutputStream());
            output.write(body.getBytes());
            output.flush();
            output.close();

            // clear textfield on successful POST
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // clear edittext and re-enable button
                    ((EditText) findViewById(R.id.writeMessage)).setText("");
                }
            });
        } finally {
            if (output != null) {
                output.close();
            }
            conn.disconnect();
            // disable button on POST
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.sendButton).setEnabled(true);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMessageDbHelper.close();
        mGroupDbHelper.close();
        mUserDbHelper.close();
    }

}
