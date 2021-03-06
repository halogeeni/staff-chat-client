package com.ateamdevelopers.staffchatclient;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlUserParser {

    private static final String ns = null;
    private final String TAG = "XmlUserParser";

    public List<User> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readUsers(parser);
        } finally {
            in.close();
        }
    }

    private List<User> readUsers(XmlPullParser parser) throws XmlPullParserException, IOException {
        List users = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "users");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("user")) {
                users.add(readUser(parser));
            } else {
                skip(parser);
            }
        }

        return users;
    }

    private User readUser(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "user");

        int userId = 0;
        String firstname = "", lastname = "", fullname;

        // XML node parse loop
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();

            if (tag.equals("userId")) {
                userId = Integer.parseInt(readTagValue(parser, "userId"));
            } else if (tag.equals("firstname")) {
                firstname = readTagValue(parser, "firstname");
            } else if (tag.equals("lastname")) {
                lastname = readTagValue(parser, "lastname");
            } else {
                skip(parser);
            }
        }

        fullname = firstname + " " + lastname;
        return new User(userId, fullname);
    }

    private String readTagValue(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String value = readText(parser);
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
