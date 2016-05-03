package com.ateamdevelopers.staffchatclient;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlMessageParser {

    private static final String ns = null;
    private final String TAG = "XmlMessageParser";

    public List<Message> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            // so far just broadcast messages...
            return readMessages(parser);
        } finally {
            in.close();
        }
    }

    private List<Message> readMessages(XmlPullParser parser) throws XmlPullParserException, IOException {
        Log.d(TAG, "in readMessages");
        List messages = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "messages");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("message")) {
                messages.add(readSingleMessage(parser));
                Log.d(TAG, "read message");
            } else {
                skip(parser);
            }
        }

        //Log.d(TAG, "message 0 now: " + messages.get(0).toString());

        return messages;
    }

    // TODO read group messages, read user-to-user messages

    // just broadcast messages so far...
    private Message readSingleMessage(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "message");

        long timestamp = 0;
        Integer fromUserId = null;
        String body = "";

        // XML node parse loop
        while (parser.next() != XmlPullParser.END_TAG) {
            Log.d(TAG, "in parsing while-loop");
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();

            Log.d(TAG, "tag now: " + tag);

            if (tag.equals("body")) {
                parser.next();
                body = readText(parser);
            }
        }

        // two whiles! surely you are a wizard
        // - XML parsing black magic

        // XML node parse loop
        while (parser.next() != XmlPullParser.END_TAG) {
            Log.d(TAG, "in parsing while-loop");
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();

            Log.d(TAG, "tag now: " + tag);

            if (tag.equals("fromUserId")) {
                Log.d(TAG, "fromUserId tag found");
                fromUserId = Integer.parseInt(readTagValue(parser, "fromUserId"));
            } else if (tag.equals("timestamp")) {
                Log.d(TAG, "timestamp tag found");
                // timestamp defaults to unix epoch in case of errors
                timestamp = Long.parseLong(readTagValue(parser, "timestamp"));
            } else {
                skip(parser);
            }
        }

        return new Message(fromUserId, body, timestamp);
    }

    private String readTagValue(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String value = readText(parser);
        Log.d(TAG, "value read: " + value);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return value;
    }



    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
