package com.ateamdevelopers.staffchatclient;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    // action bar title - we will eventually migrate to this
    private String mTitle;

    // TODO implement navigation drawer

    // REST URL - should be eventually the root, e.g. http://10.0.2.2:8080/StaffChat/webresources/
    private Uri restEndpointURI;
    private MessageAdapter mAdapter;
    private List<Message> mMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        restEndpointURI = Uri.parse("http://10.0.2.2:8080/StaffChat/webresources/messages/broadcast");
        ListView messageListView = (ListView) findViewById(R.id.messageList);

        mMessages = new ArrayList<>();
        mAdapter = new MessageAdapter(this, mMessages);
        messageListView.setAdapter(mAdapter);

        callAsyncTask();
    }

    // 2 sec polling timer
    protected void callAsyncTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            new AsyncListViewLoader().execute(restEndpointURI);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 2000); //execute every 2000 ms
    }

    private class AsyncListViewLoader extends AsyncTask<Uri, Void, List<Message>> {
        @Override
        protected void onPostExecute(List<Message> result) {
            super.onPostExecute(result);
            // update listview only if new messages have been fetched
            Log.d(TAG, "messages now: " + result.size());
            if(result != null) {
                if(result.size() > mMessages.size()) {
                    Log.d(TAG, "new results available - updating UI");
                    // update UI only if new messages have been fetched
                    mMessages.clear();
                    mMessages.addAll(result);
                    mAdapter.notifyDataSetChanged();
                    Log.d(TAG, "listadapter getCount() now: " + mAdapter.getCount());
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Message> doInBackground(Uri... params) {
            Log.d(TAG, "in doInBackground()");
            List<Message> result = new ArrayList<Message>();
            HttpURLConnection conn;

            try {
                // parse string to url object
                URL u = new URL(params[0].toString());
                // open connection to url
                conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                // get response code from connection in order to avoid unnecessary XML parsing
                // ideally the server should return 304 NOT MODIFIED response in case there is no new content
                // but the backend is not set up that way, at the moment
                int response = conn.getResponseCode();
                if(response == 200) {
                    Log.d(TAG, "server response was 200 OK - parsing data");
                    // OK - connection OK & new content is available
                    XmlMessageParser parser = new XmlMessageParser();
                    return parser.parse(conn.getInputStream());
                }
                conn.disconnect();
            } catch(Throwable t) {
                t.printStackTrace();
            }

            return null;
        }

    }

}
