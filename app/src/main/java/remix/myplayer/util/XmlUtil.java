package remix.myplayer.util;

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

import remix.myplayer.model.PlayListItem;
import remix.myplayer.ui.activity.PlayListActivity;

/**
 * Created by taeja on 16-1-26.
 */

/**
 * Xml工具类
 * 保存播放列表 正在播放列表 搜素历史
 */
public class XmlUtil {
    private static final String TAG = "XmlUtil";
    private static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    /**
     * 根据播放列表名字获得相应的歌曲信息列表
     * @param name 播放列表名字
     * @return
     */
    public static Map<String,ArrayList<PlayListItem>> getPlayList(String name)  {
        Map<String,ArrayList<PlayListItem>> map = new HashMap<String,ArrayList<PlayListItem>>();
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<PlayListItem> list = null;
        FileInputStream in = null;
        try {
            in = mContext.openFileInput(name);
            parser.setInput(in,"UTF-8");
            int eventType = parser.getEventType();
            String tag = null;
            PlayListItem item = null;
            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        list = new ArrayList<>();
                        break;
                    case XmlPullParser.START_TAG:
                        if(!parser.getName().equals("playlist") && !parser.getName().equals("song"))
                            tag = parser.getName();
                        if(tag != null && parser.getName().equals("song")) {
                            item = new PlayListItem(parser.getAttributeValue(0),Integer.parseInt(parser.getAttributeValue(1)),
                                    Integer.parseInt(parser.getAttributeValue(2)),parser.getAttributeValue(3));
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

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
                try {
                    if(in != null)
                        in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return map;
    }

    /**
     * 获得正在播放列表
     * @return
     */
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
            while(eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
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

    /**
     * 删除某个播放列表
     * @param name 需要删除的播放列表的名字
     */
    public static void deletePlaylist(String name)  {
        if(name != null && !name.equals("")) {
            Global.mPlaylist.remove(name);
            updatePlaylist();
        }
    }

    /**
     * 添加某个播放列表
     * @param name 需要添加的播放列表名字
     */
    public static void addPlaylist(String name) {
        if(name != null && !name.equals("")) {
            Global.mPlaylist.put(name, new ArrayList<PlayListItem>());
            updatePlaylist();
        }
    }

    /**
     * 删除某个播放列表下的歌曲
     * @param name 播放列表名字
     * @param item 删除歌曲
     */
    public static void deleteSongFromPlayList(String name, PlayListItem item) {
        if(!item.getSongame().equals("") && !name.equals("")) {
            ArrayList<PlayListItem> list = Global.mPlaylist.get(name);
            boolean ret = list.remove(item);
            updatePlaylist();
        }
    }


    /**
     * 某个播放列表下新增歌曲
     * @param name 播放列表名字
     * @param song 歌曲名
     * @param id 歌曲id
     * @param album_id 专辑id
     */
    public static void addSongToPlayList(String name, String song, int id, int album_id,String artist) {
        if(!name.equals("") && !song.equals("")) {
            ArrayList<PlayListItem> list = Global.mPlaylist.get(name);
            list.add(new PlayListItem(song,id,album_id,artist));
            updatePlaylist();
        }
    }


    /**
     * 更新播放列表
     */
    public static void updatePlaylist() {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playlist.xml",Context.MODE_PRIVATE);
            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
            parser.setOutput(fos,"utf-8");
            parser.startDocument("utf-8",true);
            parser.startTag(null,"playlist");
            Iterator it = Global.mPlaylist.keySet().iterator();
            while(it.hasNext()) {
                String key = it.next().toString();
                ArrayList<PlayListItem> list = Global.mPlaylist.get(key);
                parser.startTag(null,key);
                for(int i = 0 ; i < list.size() ;i++)
                {
                    parser.startTag(null,"song");
                    parser.attribute(null,"name",list.get(i).getSongame());
                    parser.attribute(null,"id",String.valueOf(list.get(i).getId()));
                    parser.attribute(null,"albumid",String.valueOf(list.get(i).getAlbumId()));
                    parser.attribute(null,"artist",list.get(i).getArtist());
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


    /**
     * 删除正在播放列表中歌曲
     * @param id 需要删除的歌曲id
     */
    public static void deleteSongFromPlayingList(long id) {
        if(id > 0 && Global.mPlayingList.contains(id)) {
            Global.mPlayingList.remove(id);
            updatePlayingList();
        }
    }

    /**
     * 添加歌曲到正在播放列表
     * @param id 需要添加的歌曲id
     */
    public static void addSongToPlayingList(long id) {
        if(id > 0) {
            Global.mPlayingList.add(id);
            updatePlayingList();
        }
    }

    /**
     * 更新正在播放列表
     */
    public static void updatePlayingList() {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playinglist.xml",Context.MODE_PRIVATE);
//            fos = new FileOutputStream(mPlayingListFile);
            XmlSerializer serializer =  XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(fos,"utf-8");
            serializer.startDocument("utf-8",true);
            serializer.startTag(null,"playinglist");
            for(int i = 0; i < Global.mPlayingList.size(); i++)
            {
                serializer.startTag(null,"song");
                serializer.attribute(null,"id", Global.mPlayingList.get(i).toString());
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

//    //添加到搜索历史
//    public static void addKey(String key){
//        if(!SearchActivity.mSearchHisKeyList.contains(key)){
//            SearchActivity.mSearchHisKeyList.add(key);
//            updateSearchList();
//        }
//    }
//    //删除一个搜索记录
//    public static void deleteKey(String key){
//        if(SearchActivity.mSearchHisKeyList.contains(key)){
//            SearchActivity.mSearchHisKeyList.remove(key);
//            updateSearchList();
//        }
//    }
//    //清空搜索历史
//    public static void removeallKey(){
//        if(SearchActivity.mSearchHisKeyList.size() > 0){
//            SearchActivity.mSearchHisKeyList.clear();
//            updateSearchList();
//        }
//    }
//    //更新搜索历史记录
//    public static void updateSearchList(){
//        XmlSerializer serializer = null;
//        FileOutputStream fos = null;
//        try {
////            fos = new FileOutputStream(mSearchHistoryFile);
//            serializer =  XmlPullParserFactory.newInstance().newSerializer();
//            fos = mContext.openFileOutput("searchhistory.xml",Context.MODE_PRIVATE);
//            serializer.setOutput(fos,"utf-8");
//            serializer.startDocument("utf-8",true);
//            serializer.startTag(null,"searchhistory");
//            for(int i = 0 ; i < SearchActivity.mSearchHisKeyList.size() ; i++){
//                serializer.startTag(null,"key");
//                serializer.text(SearchActivity.mSearchHisKeyList.get(i).toString());
//                serializer.endTag(null,"key");
//                Log.d(TAG,"key[" + i + "]: " + SearchActivity.mSearchHisKeyList.get(i));
//            }
//            serializer.endTag(null,"searchhistory");
//            serializer.endDocument();
//        } catch (XmlPullParserException e) {
//            e.printStackTrace();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e){
//            e.printStackTrace();
//        } finally {
//            if(fos != null)
//                try {
//                    fos.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//        }
//    }

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
