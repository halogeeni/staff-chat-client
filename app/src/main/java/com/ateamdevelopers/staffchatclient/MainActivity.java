package com.ateamdevelopers.staffchatclient;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = "MainActivity";
    private final int MESSAGE_LOADER_ID = 0x01, GROUP_LOADER_ID = 0x02, USER_LOADER_ID = 0x03;

    private final int currentUser = 0;
    private int groupSelection;
    private int userSelection;

    private Channel channel;

    // action bar title - we will eventually migrate to this
    private String mTitle;

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

    // REST URL - should be eventually the root, e.g. http://10.0.2.2:8080/StaffChat/webresources/
    private final String postMessageUrl = "http://10.0.2.2:8080/StaffChat/webresources/messages/add";
    private final String messageUrl = "http://10.0.2.2:8080/StaffChat/webresources/messages";
    private final String userUrl = "http://10.0.2.2:8080/StaffChat/webresources/users";
    private final String groupUrl = "http://10.0.2.2:8080/StaffChat/webresources/groups";

    // For Joel's computer only, cos why not
    /*private final String postMessageUrl = "http://192.168.43.169:8080/RESTfulWebApp/webresources/messages/add";
    private final String messageUrl = "http://192.168.43.169:8080/RESTfulWebApp/webresources/messages/broadcast";
    private final String userUrl = "http://192.168.43.169:8080/RESTfulWebApp/webresources/users";
    private final String groupUrl = "http://192.168.43.169:8080/RESTfulWebApp/webresources/groups";*/

    //private Uri mEndpointURI;
    private MessageDatabaseHelper mMessageDbHelper;
    private GroupDatabaseHelper mGroupDbHelper;
    private UserDatabaseHelper mUserDbHelper;
    private SimpleCursorAdapter mMessageAdapter, mUserAdapter, mGroupAdapter;
    private List<Message> mLastMessages;
    private List<User> mLastUsers;
    private List<Group> mLastGroups;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // open broadcast chat by default
        channel = Channel.CHANNEL_BROADCAST;

        // default contact selection values
        groupSelection = 0;
        userSelection = 0;

        // flush the databases on login
        mMessageDbHelper = new MessageDatabaseHelper(this);
        mMessageDbHelper.initDb();

        mGroupDbHelper = new GroupDatabaseHelper(this);
        mGroupDbHelper.initDb();

        mUserDbHelper = new UserDatabaseHelper(this);
        mUserDbHelper.initDb();

        mLastMessages = new ArrayList<>();
        mLastGroups = new ArrayList<>();
        mLastUsers = new ArrayList<>();

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

        ListView messageListView = (ListView) findViewById(R.id.messageList);
        ListView userListView = (ListView) findViewById(R.id.drawer_users);
        ListView groupListView = (ListView) findViewById(R.id.drawer_groups);

        messageListView.setAdapter(mMessageAdapter);
        userListView.setAdapter(mUserAdapter);
        groupListView.setAdapter(mGroupAdapter);

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
                Log.d(TAG, "userList item # " + id + " clicked");
                userSelection = position;
                channel = Channel.CHANNEL_PRIVATE;
                mTimer.cancel();
                mMessageDbHelper.initDb();
                mLastMessages.clear();
                mTimer = startMessagePollingTask();
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
                mMessageDbHelper.initDb();
                mLastMessages.clear();
                mTimer = startMessagePollingTask();
            }
        });
    }

    public void sendButtonClicked(View v) {
        try {
            new UploadXmlTask().execute(postMessageUrl);
        } catch (Exception e) {
            // TODO Auto-generated catch block
        }
    }

    // 2 sec message polling timer
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
        timer.schedule(doAsynchronousTask, 0, 1000); //execute every 2 secs
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
        Log.d(TAG, "in downloadMessageXml()");

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
                urlString +=("/group/" + groupSelection);
                break;
        }

        try {
            // connect to the server & get stream
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream();
            stream = new BufferedInputStream(conn.getInputStream());

            // not the most elegant solution, but works for now
            List<Message> messageList = messageParser.parse(stream);
            Log.d(TAG, "in downloadMessageXml - mLastMessages now: " + mLastMessages);
            if (mLastMessages.isEmpty()) {
                mLastMessages.addAll(messageList);
                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                for (Message m : messageList) {
                    values.put(DataContract.MessageEntry.COLUMN_NAME_FROM_USER, m.getFromUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_USER, m.getToUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_GROUP, m.getToGroupId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_BODY, m.getBody());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP, m.getTimestamp());
                    getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
                }
            } else if (messageList.size() > mLastMessages.size()) {
                Log.d(TAG, "fetched message list contained new entries");

                List<Message> tempMessageList = new ArrayList<>(messageList);

                messageList.removeAll(mLastMessages);
                ContentValues values = new ContentValues();

                for (Message m : messageList) {
                    values.put(DataContract.MessageEntry.COLUMN_NAME_FROM_USER, m.getFromUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_USER, m.getToUserId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TO_GROUP, m.getToGroupId());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_BODY, m.getBody());
                    values.put(DataContract.MessageEntry.COLUMN_NAME_TIMESTAMP, m.getTimestamp());
                    getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
                }

                mLastMessages.clear();
                mLastMessages.addAll(tempMessageList);
            }

        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return result;
    }

    private String downloadUserXml(String urlString) throws XmlPullParserException, IOException {
        Log.d(TAG, "in downloadUserXml()");

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

            // not the most elegant solution, but works for now
            List<User> userList = userParser.parse(stream);
            Log.d(TAG, "in downloadUserXml - mLastUsers now: " + mLastUsers);
            if (mLastUsers.isEmpty()) {
                mLastUsers.addAll(userList);
                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                for (User u : userList) {
                    values.put(DataContract.UserEntry.COLUMN_NAME_USERID, u.getId());
                    values.put(DataContract.UserEntry.COLUMN_NAME_USER_NAME, u.getName());
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, values);
                }
            } else if (userList.size() > mLastUsers.size()) {
                Log.d(TAG, "fetched user list contained new entries");

                List<User> tempUserList = new ArrayList<>(userList);

                userList.removeAll(mLastUsers);
                ContentValues values = new ContentValues();

                for (User u : userList) {
                    values.put(DataContract.UserEntry.COLUMN_NAME_USERID, u.getId());
                    values.put(DataContract.UserEntry.COLUMN_NAME_USER_NAME, u.getName());
                    getContentResolver().insert(UserContentProvider.CONTENT_URI, values);
                }

                mLastUsers.clear();
                mLastUsers.addAll(tempUserList);
            }

        } finally {
            if (stream != null) {
                stream.close();
            }
        }

        return result;
    }


    private String downloadGroupXml(String urlString) throws XmlPullParserException, IOException {
        Log.d(TAG, "in downloadGroupXml()");

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

            // not the most elegant solution, but works for now
            List<Group> groupList = groupParser.parse(stream);
            Log.d(TAG, "in downloadGroupXml - mLastGroups now: " + mLastGroups);
            if (mLastGroups.isEmpty()) {
                mLastGroups.addAll(groupList);
                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                for (Group g : groupList) {
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_ID, g.getId());
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME, g.getName());
                    getContentResolver().insert(GroupContentProvider.CONTENT_URI, values);
                }
            } else if (groupList.size() > mLastGroups.size()) {
                Log.d(TAG, "fetched group list contained new entries");

                List<Group> tempGroupList = new ArrayList<>(groupList);

                groupList.removeAll(mLastGroups);
                ContentValues values = new ContentValues();

                for (Group g : groupList) {
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_ID, g.getId());
                    values.put(DataContract.GroupEntry.COLUMN_NAME_GROUP_NAME, g.getName());
                    getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
                }

                mLastGroups.clear();
                mLastGroups.addAll(tempGroupList);
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

        Log.d(TAG, "in uploadXml()");

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

        Log.d(TAG, "in uploadXml() - fetched messagebody: " + messageBody);

        try {
            Log.d(TAG, "in uploadXml() - POST setup");

            String body = "<message><body><text>" + messageBody + "</text></body>" +
                    "<channel>" + channel.toString() + "</channel><fromUserId>" + currentUser +
                    "</fromUserId><toUserId>" + userSelection + "</toUserId><toGroupId>" +
                    groupSelection + "</toGroupId><messageId>-1</messageId><timestamp>-1</timestamp></message>";

            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setFixedLengthStreamingMode(body.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/xml; charset=\"utf-8\"");

            Log.d(TAG, "in uploadXml() - sending XML data to server:\n"+body);
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
            Log.d(TAG, "in uploadXml() - disconnecting");
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
