package remix.myplayer.utils;

import android.content.Context;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
            File file1 = new File("/data/data/remix.myplayer/files/playinglist.xml");
            if(!file1.exists())
                file1.createNewFile();
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
    public static Map<String,ArrayList<PlayListItem>> getPlayList(String name)  {
        Map<String,ArrayList<PlayListItem>> map = new HashMap<String,ArrayList<PlayListItem>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<PlayListItem> list = null;
        String key = null;
        FileInputStream in = null;
        try {
            in = mContext.openFileInput(name);

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


    public static ArrayList<Long> getPlayingList()  {
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<Long> list = null;
        FileInputStream in = null;
        try {
//            in = new FileInputStream(Environment.getExternalStorageDirectory().getCanonicalPath() + "/playinglist.xml");
            in = mContext.openFileInput("playinglist.xml");
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
                        if(parser.getName().equals("playinglist") )
                            tag = parser.getName();
                        if(tag != null && parser.getName().equals("song")) {
                            list.add(Long.parseLong(parser.getAttributeValue(0)));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if(tag != null && parser.getName().equals(tag)) {
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
        finally {
            try {
                if(in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static void deletePlaylist(String name)  {
        if(name != null && !name.equals("")) {
            PlayListActivity.mPlaylist.remove(name);
            updatePlaylistXml();
        }
    }
    public static void addPlaylist(String name)
    {
        if(name != null && !name.equals("")) {
            PlayListActivity.mPlaylist.put(name, new ArrayList<PlayListItem>());
            updatePlaylistXml();
        }
    }
    public static void deleteSong(String name,String song)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            list.remove(song);
            updatePlaylistXml();
        }
    }

    public static void addSong(String name,String song,int id)
    {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            list.add(new PlayListItem(song,id));
            updatePlaylistXml();
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
            updatePlaylistXml();
        }
    }
    public static void updatePlaylistXml()
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


    public static void deleteSong(long id)
    {
        if(id > 0 && DBUtil.mPlayingList.contains(id)) {
            DBUtil.mPlayingList.remove(id);
            updatePlayingListXml();
        }
    }

    public static void addSong(long id)
    {
        if(id > 0) {
            DBUtil.mPlayingList.add(id);
            updatePlayingListXml();
        }
    }

    public static void updatePlayingListXml()
    {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playinglist.xml",Context.MODE_PRIVATE);
            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
            parser.setOutput(fos,"utf-8");
            parser.startDocument("utf-8",true);
            parser.startTag(null,"playinglist");
            for(int i = 0; i < DBUtil.mPlayingList.size(); i++)
            {
                parser.startTag(null,"song");
                parser.attribute(null,"id", DBUtil.mPlayingList.get(i).toString());
                parser.endTag(null,"song");
            }
            parser.endTag(null,"playinglist");
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
