package com.ateamdevelopers.staffchatclient;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XmlGroupParser {

    private static final String ns = null;
    private final String TAG = "XmlGroupParser";


    public List<Group> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readGroups(parser);
        } finally {
            in.close();
        }
    }

    private List<Group> readGroups(XmlPullParser parser) throws XmlPullParserException, IOException {
        Log.d(TAG, "in readGroups");
        List groups = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "groups");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("group")) {
                groups.add(readGroup(parser));
                Log.d(TAG, "read group");
            } else {
                skip(parser);
            }
        }

        Log.d(TAG, "group 0 now: " + groups.get(0).toString());

        return groups;
    }

    private Group readGroup(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "group");

        int groupId = 0;
        String name = "";

        // XML node parse loop
        while (parser.next() != XmlPullParser.END_TAG) {
            Log.d(TAG, "in parsing while-loop");
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tag = parser.getName();

            if (tag.equals("id")) {
                Log.d(TAG, "id tag found");
                groupId = Integer.parseInt(readTagValue(parser, "id"));
            } else if (tag.equals("name")) {
                Log.d(TAG, "name tag found");
                name = readTagValue(parser, "name");
            } else {
                skip(parser);
            }
        }

        return new Group(groupId, name);
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
