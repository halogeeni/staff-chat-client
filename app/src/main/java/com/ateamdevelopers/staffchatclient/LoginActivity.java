package com.ateamdevelopers.staffchatclient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class LoginActivity extends Activity {

    private final String userUrl = "http://10.0.2.2:8080/StaffChat/webresources/users/";
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
    }

    private class DownloadUserXmlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            Log.d(TAG, "doInBackground");
            try {
                return downloadUserXml(urls[0]);
            } catch (IOException e) {
                return getResources().getString(R.string.connection_error);
            } catch (XmlPullParserException e) {
                return getResources().getString(R.string.xml_error);
            }
        }

    }

    private String downloadUserXml(String urlString) throws XmlPullParserException, IOException {
        Log.d(TAG,"Starting httpurlconnection with url: "+urlString);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        String code = String.valueOf(conn.getResponseCode());
        Log.d(TAG,"Response code: "+code);
        return code;

    }

    public void processLogin(View view) {
        // clear possible errors displayed
        TextView errorDisplay = (TextView) findViewById(R.id.login_error);
        errorDisplay.setText("");

        EditText urlInputEditText = (EditText) findViewById(R.id.url_input);
        EditText userInputEditText = (EditText) findViewById(R.id.user_id);

        // get user input strings
        String urlInput = urlInputEditText.getText().toString();
        String userIdInput = userInputEditText.getText().toString();
        String responseCode="";

        // first check that inputs contain characters
        if(urlInput.length() > 0 && userIdInput.length() > 0) {


            try {
                responseCode= new DownloadUserXmlTask().execute(userUrl+userIdInput).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            if(responseCode.equals("200")) {
                Log.d(TAG,"User was found. Continuing into MainActivity");
                // parse user inputs
                try {
                    int userid = Integer.parseInt(userIdInput);
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    i.putExtra("url", urlInput);
                    i.putExtra("userid", userid);
                    startActivity(i);
                    // TODO validate and pass uri and userid to MainActivity
                } catch (NumberFormatException e) {
                    errorDisplay.setText("User ID must be an integer value");
                }
            }else{
                errorDisplay.setText("No user by ID "+userIdInput+ " was found.");
            }
        } else {
            errorDisplay.setText("Please fill in all values.");
        }
    }
}
