package remix.myplayer.utils;

import android.os.Environment;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by taeja on 16-1-26.
 */
public class XmlUtil {
    public static Map<String,ArrayList<String>> getPlayList()
    {
        Map<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<String> list = null;
        String key = null;
        try {
            FileInputStream in = new FileInputStream(Environment.getExternalStorageDirectory().getCanonicalPath() + "/playlist.xml");
            parser.setInput(in,"UTF-8");
            int eventType = parser.getEventType();
            String tag = null;
            while(eventType != XmlPullParser.END_DOCUMENT)
            {
                switch (eventType)
                {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<>();
                        break;
                    case XmlPullParser.START_TAG:

                        if(!parser.getName().equals("playlist") && !parser.getName().equals("song"))
                            tag = parser.getName();
                        if(tag != null && parser.getName().equals("song"))
                            list.add(parser.getAttributeValue(0));
                        break;
                    case XmlPullParser.END_TAG:
                        if(tag != null && parser.getName().equals(tag)) {
                            map.put(tag,(ArrayList<String>) list.clone());
                            list.clear();
                            tag = null;
                        }
                        break;
                }
                eventType = parser.next();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}
