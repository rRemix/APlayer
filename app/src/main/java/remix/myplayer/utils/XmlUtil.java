package remix.myplayer.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
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
    static {
        try {
            File file = new File("/data/data/remix.myplayer/files/playlist.xml");
            if(!file.exists())
                file.createNewFile();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }
    private static Context mContext;
    public static void setContext(Context context)
    {
        mContext = context;
    }
    public static Map<String,ArrayList<PlayListItem>> getPlayList()  {
        Map<String,ArrayList<PlayListItem>> map = new HashMap<String,ArrayList<PlayListItem>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<PlayListItem> list = null;
        String key = null;
        FileInputStream in = null;
        try {
            in = mContext.openFileInput("playlist.xml");

            parser.setInput(in,"UTF-8");
            int eventType = parser.getEventType();
            String tag = null;
            PlayListItem item = null;
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
                        if(tag != null && parser.getName().equals("song")) {
                            item = new PlayListItem(parser.getAttributeValue(0),Integer.parseInt(parser.getAttributeValue(1)));
                            Log.d("XmlUtil",item.toString());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(tag != null && parser.getName().equals(tag)) {
                            map.put(tag,(ArrayList<PlayListItem>) list.clone());
                            list.clear();
                            tag = null;
                        }
                        if(tag != null && parser.getName().equals("song")) {
                            list.add(item);
                            item = null;
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
        finally {
                try {
                    if(in != null)
                        in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            PlayListActivity.mPlaylist.put(name, new ArrayList<PlayListItem>());
            updateXml();
        }
    }
    public static void deleteSong(String name,String song)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            list.remove(song);
            updateXml();
        }
    }

    public static void addSong(String name,String song,int id)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            list.add(new PlayListItem(song,id));
            updateXml();
        }
    }
    public static void updateSong(String name,String _new,String _old,int id)
    {
        if(!name.equals("") && !_new.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            for (int i = 0; i < list.size(); i++) {
                PlayListItem tmp = list.get(i);
                if (tmp.getmSongame().equals(_old)){
                    tmp.setSongName(_new);
                    tmp.setId(id);
                }
            }
            updateXml();
        }
    }
    public static void updateXml()
    {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playlist.xml",Context.MODE_PRIVATE);
            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
            parser.setOutput(fos,"utf-8");
            parser.startDocument("utf-8",true);
            parser.startTag(null,"playlist");
            Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
            while(it.hasNext())
            {
                String key = it.next().toString();
                ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(key);
                parser.startTag(null,key);
                for(int i = 0 ; i < list.size() ;i++)
                {
                    parser.startTag(null,"song");
                    parser.attribute(null,"name",list.get(i).getmSongame());
                    parser.attribute(null,"id",String.valueOf(list.get(i).getId()));
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
        finally {
            try {
                if(fos != null)
                    fos.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
