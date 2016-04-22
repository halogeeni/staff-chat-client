package com.ateamdevelopers.staffchatclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
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

        // first check that inputs contain characters
        if(urlInput.length() > 0 && userIdInput.length() > 0) {
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
        } else {
            errorDisplay.setText("Please fill in all values.");
        }
    }
}
