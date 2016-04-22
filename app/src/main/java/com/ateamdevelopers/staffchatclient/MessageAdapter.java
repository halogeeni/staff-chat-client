package com.ateamdevelopers.staffchatclient;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;

public class MessageAdapter extends BaseAdapter implements ListAdapter {
    private final String TAG = "MessageAdapter";

    private List<Message> mMessages;
    private Context mContext;

    public MessageAdapter(Context context, List<Message> messages) {
        this.mMessages = messages;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return mMessages.size();
    }

    @Override
    public Message getItem(int position) {
        if(mMessages != null) {
            return mMessages.get(position);
        }
        // fallback
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "in getView()");
        View v = convertView;
        ViewHolder holder;

        if (v == null) {
            Log.d(TAG, "view is NULL");
            // instantiate inflater and holder objects
            LayoutInflater inflater = LayoutInflater.from(mContext);
            v = inflater.inflate(R.layout.message_list_item, parent, false);
            holder = new ViewHolder();
            // find desired message textviews
            holder.username = (TextView) v.findViewById(R.id.messageUsername);
            holder.body = (TextView) v.findViewById(R.id.messageBody);
            holder.timestamp = (TextView) v.findViewById(R.id.messageTimestamp);
            v.setTag(holder);
        }

        Message m = mMessages.get(position);

        if(m != null) {
            holder = (ViewHolder) v.getTag();
            // so far only the user id is shown
            holder.username.setText(m.getFromUserId().toString());
            // text child node xml parsing not done yet, so hardcoded value here
            holder.body.setText("test string");
            //body.setText(m.getBody());
            // timestamp to date string conversion
            holder.timestamp.setText(getDateString(m.getTimestamp()));
        }

        return v;
    }

    public String getDateString(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return calendar.getTime().toString();
    }

    protected class ViewHolder {
        private TextView username;
        private TextView body;
        private TextView timestamp;
    }

}


