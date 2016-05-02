package com.ateamdevelopers.staffchatclient;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = "MainActivity";
    private final int MESSAGE_LOADER_ID = 0x01;

    private final int currentUser = 0;

    // action bar title - we will eventually migrate to this
    private String mTitle;

    private String[] projection = {
            MessageContract.MessageEntry._ID,
            MessageContract.MessageEntry.COLUMN_NAME_FROM_USER,
            MessageContract.MessageEntry.COLUMN_NAME_TO_USER,
            MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP,
            MessageContract.MessageEntry.COLUMN_NAME_BODY,
            MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP
    };

    // TODO implement navigation drawer

    // REST URL - should be eventually the root, e.g. http://10.0.2.2:8080/StaffChat/webresources/
    private final String url = "http://10.0.2.2:8080/StaffChat/webresources/messages/broadcast";

    private Uri mEndpointURI;
    private MessageDatabaseHelper mDbHelper;
    private SimpleCursorAdapter mAdapter;
    private List<Message> mLastMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // flush the database on login
        mDbHelper = new MessageDatabaseHelper(this);
        mDbHelper.initDb();
        mDbHelper.close();

        mLastMessages = new ArrayList<>();

        mAdapter = new SimpleCursorAdapter(this,
                R.layout.message_list_item, null,
                new String[] { MessageContract.MessageEntry.COLUMN_NAME_FROM_USER,
                        MessageContract.MessageEntry.COLUMN_NAME_BODY,
                        MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP },
                new int[] { R.id.messageUsername , R.id.messageBody, R.id.messageTimestamp }, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

        ListView mListView = (ListView) findViewById(R.id.messageList);
        mListView.setAdapter(mAdapter);
        startPollingTask();
        getLoaderManager().initLoader(MESSAGE_LOADER_ID, null, this);
    }

    // 2 sec polling timer
    protected void startPollingTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new DownloadXmlTask().execute(url);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 2000); //execute every 2000 ms
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, MessageContentProvider.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //mAdapter.notifyDataSetChanged();
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadXml(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }
    }

    private String downloadXml(String urlString) throws XmlPullParserException, IOException {
        Log.d(TAG, "in downloadXml()");

        InputStream stream = null;
        XmlMessageParser messageParser = new XmlMessageParser();
        String result = "";

        try {
            // connect to the server & get stream
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            conn.getInputStream();
            stream = new BufferedInputStream(conn.getInputStream());

            // not the most elegant solution, but works for now
            List<Message> messageList = messageParser.parse(stream);
            Log.d(TAG, "in downloadXml - mLastMessages now: " + mLastMessages);
            if(mLastMessages.isEmpty()) {
                mLastMessages.addAll(messageList);
                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                for (Message m : messageList) {
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_FROM_USER, m.getFromUserId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TO_USER, m.getToUserId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP, m.getToGroupId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_BODY, m.getBody());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP, m.getTimestamp());
                    getContentResolver().insert(MessageContentProvider.CONTENT_URI, values);
                }
            } else if(!messageList.containsAll(mLastMessages)) {
                Log.d(TAG, "fetched message list contained new entries");

                List<Message> tempMessageList = new ArrayList<>(messageList);

                messageList.removeAll(mLastMessages);
                ContentValues values = new ContentValues();
                for (Message m : messageList) {
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_FROM_USER, m.getFromUserId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TO_USER, m.getToUserId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TO_GROUP, m.getToGroupId());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_BODY, m.getBody());
                    values.put(MessageContract.MessageEntry.COLUMN_NAME_TIMESTAMP, m.getTimestamp());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }


}
