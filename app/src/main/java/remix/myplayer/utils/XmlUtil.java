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
import remix.myplayer.activities.SearchActivity;
import remix.myplayer.infos.PlayListItem;

/**
 * Created by taeja on 16-1-26.
 */
public class XmlUtil {
    private static final String TAG = "XmlUtil";
    private static Context mContext;
    private static File mPlayListFile;
    private static File mPlayingListFile;
    private static File mSearchHistoryFile;
    public static void setContext(Context context) {
        mContext = context;
//        try {
//            File mDir = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName());
//            if(!mDir.exists())
//                mDir.mkdir();
//            mPlayListFile = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/playlist.xml");
//            if(!mPlayListFile.exists())
//                mPlayListFile.createNewFile();
//            mPlayingListFile = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/playinglist.xml");
//            if(!mPlayingListFile.exists())
//                mPlayingListFile.createNewFile();
//            mSearchHistoryFile = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + mContext.getPackageName() + "/searchhistory.xml");
//            if(!mSearchHistoryFile.exists())
//                mSearchHistoryFile.createNewFile();
//
//        }
//        catch (IOException e){
//            e.printStackTrace();
//            ErrUtil.writeError(TAG + "---CreateListFile---" + e.toString());
//        }
    }


    public static Map<String,ArrayList<PlayListItem>> getPlayList(String name)  {
        Map<String,ArrayList<PlayListItem>> map = new HashMap<String,ArrayList<PlayListItem>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<PlayListItem> list = null;
        String key = null;
        FileInputStream in = null;
        try {
            in = mContext.openFileInput(name);
//            in = new FileInputStream(mPlayListFile);
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
                            item = new PlayListItem(parser.getAttributeValue(0),Integer.parseInt(parser.getAttributeValue(1)),
                                    Integer.parseInt(parser.getAttributeValue(2)));
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
            in = mContext.openFileInput("playinglist.xml");
//            in = new FileInputStream(mPlayingListFile);
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
            PlayListActivity.getPlayList().remove(name);
            updatePlaylist();
        }
    }

    public static void addPlaylist(String name) {
        if(name != null && !name.equals("")) {
            PlayListActivity.getPlayList().put(name, new ArrayList<PlayListItem>());
            updatePlaylist();
        }
    }
    public static void deleteSong(String name,PlayListItem item) {
        if(!item.getSongame().equals("") && !name.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(name);
            boolean ret = list.remove(item);
            updatePlaylist();
        }
    }

    public static void addSong(String name,String song,int id,int album_id) {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(name);
            list.add(new PlayListItem(song,id,album_id));
            updatePlaylist();
        }
    }
    public static void updateSong(String name,String _new,String _old,int id)
    {
        if(!name.equals("") && !_new.equals("")) {
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(name);
            for (int i = 0; i < list.size(); i++) {
                PlayListItem tmp = list.get(i);
                if (tmp.getSongame().equals(_old)){
                    tmp.setSongName(_new);
                    tmp.setId(id);
                }
            }
            updatePlaylist();
        }
    }
    public static void updatePlaylist()
    {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playlist.xml",Context.MODE_PRIVATE);
//            fos = new FileOutputStream(mPlayListFile);
            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
            parser.setOutput(fos,"utf-8");
            parser.startDocument("utf-8",true);
            parser.startTag(null,"playlist");
            Iterator it = PlayListActivity.getPlayList().keySet().iterator();
            while(it.hasNext())
            {
                String key = it.next().toString();
                ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(key);
                parser.startTag(null,key);
                for(int i = 0 ; i < list.size() ;i++)
                {
                    parser.startTag(null,"song");
                    parser.attribute(null,"name",list.get(i).getSongame());
                    parser.attribute(null,"id",String.valueOf(list.get(i).getId()));
                    parser.attribute(null,"albumid",String.valueOf(list.get(i).getAlbumId()));
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
            updatePlayingList();
        }
    }

    public static void addSong(long id)
    {
        if(id > 0) {
            DBUtil.mPlayingList.add(id);
            updatePlayingList();
        }
    }

    public static void updatePlayingList()
    {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playinglist.xml",Context.MODE_PRIVATE);
//            fos = new FileOutputStream(mPlayingListFile);
            XmlSerializer serializer =  XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(fos,"utf-8");
            serializer.startDocument("utf-8",true);
            serializer.startTag(null,"playinglist");
            for(int i = 0; i < DBUtil.mPlayingList.size(); i++)
            {
                serializer.startTag(null,"song");
                serializer.attribute(null,"id", DBUtil.mPlayingList.get(i).toString());
                serializer.endTag(null,"song");
            }
            serializer.endTag(null,"playinglist");
            serializer.endDocument();
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

    //添加到搜索历史
    public static void addKey(String key){
        if(!SearchActivity.mSearchHisKeyList.contains(key)){
            SearchActivity.mSearchHisKeyList.add(key);
            updateSearchList();
        }
    }
    //删除一个搜索记录
    public static void deleteKey(String key){
        if(SearchActivity.mSearchHisKeyList.contains(key)){
            SearchActivity.mSearchHisKeyList.remove(key);
            updateSearchList();
        }
    }
    //清空搜索历史
    public static void removeallKey(){
        if(SearchActivity.mSearchHisKeyList.size() > 0){
            SearchActivity.mSearchHisKeyList.clear();
            updateSearchList();
        }
    }
    //更新搜索历史记录
    public static void updateSearchList(){
        XmlSerializer serializer = null;
        FileOutputStream fos = null;
        try {
//            fos = new FileOutputStream(mSearchHistoryFile);
            serializer =  XmlPullParserFactory.newInstance().newSerializer();
            fos = mContext.openFileOutput("searchhistory.xml",Context.MODE_PRIVATE);
            serializer.setOutput(fos,"utf-8");
            serializer.startDocument("utf-8",true);
            serializer.startTag(null,"searchhistory");
            for(int i = 0 ; i < SearchActivity.mSearchHisKeyList.size() ; i++){
                serializer.startTag(null,"key");
                serializer.text(SearchActivity.mSearchHisKeyList.get(i).toString());
                serializer.endTag(null,"key");
                Log.d(TAG,"key[" + i + "]: " + SearchActivity.mSearchHisKeyList.get(i));
            }
            serializer.endTag(null,"searchhistory");
            serializer.endDocument();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    //获得搜索历史记录
    public static ArrayList<String> getSearchHisList()  {
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<String> list = new ArrayList<>();
        FileInputStream in = null;
        try {
//            in = new FileInputStream(mSearchHistoryFile);
            in = mContext.openFileInput("searchhistory.xml");
//
            parser.setInput(in,"utf-8");
            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if(parser.getName().equals("key")) {
                            list.add(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.END_DOCUMENT:
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
        } finally {
            if(in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return list;
    }
}
