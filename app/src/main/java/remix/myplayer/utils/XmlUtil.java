package remix.myplayer.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import remix.myplayer.activities.MainActivity;
import remix.myplayer.activities.PlayListActivity;

/**
 * Created by taeja on 16-1-26.
 */
public class XmlUtil {
    private static Context mContext;
    public static void setContext(Context context)
    {
        mContext = context;
    }
    public static Map<String,ArrayList<String>> getPlayList()
    {
        Map<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<String> list = null;
        String key = null;
        try {
            FileInputStream in = mContext.openFileInput("playlist.xml");
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
    public static void deletePlaylist(String name)  {
        if(name != null && !name.equals("")) {
            PlayListActivity.mPlaylist.remove(name);
            updateXml();
        }
    }
    public static void addPlaylist(String name)
    {
        if(name != null && !name.equals("")) {
            PlayListActivity.mPlaylist.put(name, new ArrayList<String>());
            updateXml();
        }
    }
    public static void deleteSong(String name,String song)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<String> list = PlayListActivity.mPlaylist.get(name);
            list.remove(song);
            updateXml();
        }
    }

    public static void addSong(String name,String song)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<String> list = PlayListActivity.mPlaylist.get(name);
            list.add(song);
            updateXml();
        }
    }
    public static void updateSong(String name,String _new,String _old)
    {
        if(!name.equals("") && !_new.equals("")) {
            ArrayList<String> list = PlayListActivity.mPlaylist.get(name);
            for (int i = 0; i < list.size(); i++) {
                String tmp = list.get(i);
                if (tmp.equals(_old))
                    tmp = _new;
            }
            updateXml();
        }
    }
    public static void updateXml()
    {
        try {
            FileOutputStream fos = mContext.openFileOutput("playlist.xml",Context.MODE_PRIVATE);
            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
            parser.setOutput(fos,"utf-8");
            parser.startDocument("utf-8",true);
            parser.startTag(null,"playlist");
            Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
            while(it.hasNext())
            {
                String key = it.next().toString();
                ArrayList<String> list = PlayListActivity.mPlaylist.get(key);
                parser.startTag(null,key);
                for(int i = 0 ; i < list.size() ;i++)
                {
                    parser.startTag(null,"song");
                    parser.attribute(null,"name",list.get(i));
                    parser.endTag(null,"song");
                }
                parser.endTag(null,key);
            }
            parser.endTag(null,"playlist");
            parser.endDocument();
        }
        catch (XmlPullParserException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
