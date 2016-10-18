package remix.myplayer.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import remix.myplayer.R;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.ui.activity.ChildHolderActivity;

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
                            LogUtil.d("XmlUtil",item.toString());
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
    public static ArrayList<Integer> getPlayQueue()  {
        XmlPullParser parser = Xml.newPullParser();
        ArrayList<Integer> list = null;
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
                            list.add(Integer.parseInt(parser.getAttributeValue(0)));
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
            Global.mPlayList.remove(name);
            updatePlaylist();
        }
    }

    /**
     * 添加某个播放列表
     * @param context
     * @param name 需要添加的播放列表名字
     */
//    public static void addPlaylist(Context context,String name) {
//        if(name != null && !name.equals("")) {
//            Global.mPlayList.put(name, new ArrayList<PlayListItem>());
//            updatePlaylist();
//            Toast.makeText(context,R.string.add_playlist_success,Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(context,R.string.add_playlist_error,Toast.LENGTH_SHORT).show();
//        }
//    }



    /**
     * 某个播放列表下新增歌曲
     * @param playlistName 播放列表名
     * @param song 歌曲名
     * @param id 歌曲id
     * @param album_id 专辑id
     */
//    public static boolean addSongToPlayList(String playlistName, String song, int id, int album_id,String artist,boolean needUpdate) {
//        if(TextUtils.isEmpty(playlistName) || TextUtils.isEmpty(song) || id < 0 || album_id < 0){
//            return false;
//        }
//        try {
//            boolean isExist = false;
//            for(PlayListItem item : Global.mPlayList.get(playlistName)){
//                if(item.getId() == id){
//                    isExist = true;
//                }
//            }
//            if(isExist){
//                return false;
//            } else {
//                ArrayList<PlayListItem> list = Global.mPlayList.get(playlistName);
//                list.add(new PlayListItem(song,id,album_id,artist));
//                if(needUpdate)
//                    updatePlaylist();
//                return true;
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        return false;
//    }

    /**
     * 某个播放列表下新增多首歌曲
     */
    public static int addSongsToPlayList(String playlistName, ArrayList<PlayListItem> itemList) {
        if (itemList == null)
            return 0;
        int num = 0;
        for(PlayListItem item : itemList){
            if(addSongToPlayList(playlistName,item.getSongame(),item.getId(),item.getAlbumId(),item.getArtist(),false)){
                num++;
            }
        }
        if(num > 0)
            updatePlaylist();
        return num;
    }

    /**
     * 更新播放列表
     */
//    public static void updatePlaylist() {
//        FileOutputStream fos = null;
//        try {
//            fos = mContext.openFileOutput("playlist.xml",Context.MODE_PRIVATE);
//            XmlSerializer parser =  XmlPullParserFactory.newInstance().newSerializer();
//            parser.setOutput(fos,"utf-8");
//            parser.startDocument("utf-8",true);
//            parser.startTag(null,"playlist");
//            Iterator it = Global.mPlayList.keySet().iterator();
//            while(it.hasNext()) {
//                String key = it.next().toString();
//                ArrayList<PlayListItem> list = Global.mPlayList.get(key);
//                parser.startTag(null,key);
//                for(int i = 0 ; i < list.size() ;i++)
//                {
//                    parser.startTag(null,"song");
//                    parser.attribute(null,"name",list.get(i).getSongame());
//                    parser.attribute(null,"id",String.valueOf(list.get(i).getId()));
//                    parser.attribute(null,"albumid",String.valueOf(list.get(i).getAlbumId()));
//                    parser.attribute(null,"artist",list.get(i).getArtist());
//                    parser.endTag(null,"song");
//                }
//                parser.endTag(null,key);
//            }
//            parser.endTag(null,"playlist");
//            parser.endDocument();
//        }
//        catch (XmlPullParserException e){
//            e.printStackTrace();
//        }
//        catch (IOException e){
//            e.printStackTrace();
//        }
//        finally {
//            try {
//                if(fos != null)
//                    fos.close();
//            }
//            catch (IOException e){
//                e.printStackTrace();
//            }
//        }
//    }


    /**
     * 删除正在播放列表中歌曲
     * @param id 需要删除的歌曲id
     */
    public static void deleteSongFromPlayQueue(long id) {
        if(id > 0 && Global.mPlayQueue.contains(id)) {
            Global.mPlayQueue.remove(id);
            updatePlayQueue();
        }
    }

    /**
     * 添加歌曲到正在播放列表
     * @param id 需要添加的歌曲id
     */
    public static void addSongToPlayQueue(int id) {
        if(id > 0 && Global.mPlayQueue != null && !Global.mPlayQueue.contains(id)) {
            Global.mPlayQueue.add(id);
            updatePlayQueue();
        }
    }

    /**
     * 添加多首歌曲到正在播放列表
     * @param idList 需要添加的歌曲id列表
     */
    public static int addSongsToPlayingList(ArrayList<Integer> idList) {
        int num = 0;
        for(Integer id : idList){
            if(id > 0 && Global.mPlayQueue != null) {
                Global.mPlayQueue.add(id);
                num++;
            }
        }
        if(num > 0)
            updatePlayQueue();
        return num;
    }

    /**
     * 更新正在播放列表
     */
    public static void updatePlayQueue() {
        FileOutputStream fos = null;
        try {
            fos = mContext.openFileOutput("playinglist.xml",Context.MODE_PRIVATE);
//            fos = new FileOutputStream(mPlayingListFile);
            XmlSerializer serializer =  XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(fos,"utf-8");
            serializer.startDocument("utf-8",true);
            serializer.startTag(null,"playinglist");
            for(int i = 0; i < Global.mPlayQueue.size(); i++)
            {
                serializer.startTag(null,"song");
                serializer.attribute(null,"id", Global.mPlayQueue.get(i).toString());
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

    /**
     * 根据歌曲id删除某播放列表中歌曲
     * @param playlist 播放列表名
     * @param id 需要删除的歌曲id
     * @return 删除是否成功
     */
//    public static boolean deleteSongInPlayList(String playlist,int id){
//        boolean ret = false;
//        ArrayList<PlayListItem> list = Global.mPlayList.get(playlist);
//        if(list != null){
//            for(PlayListItem item : list){
//                if(item.getId() == id){
//                    ret = list.remove(item);
//                    if(ChildHolderActivity.mInstance != null)
//                        ChildHolderActivity.mInstance.UpdateData();
//                    updatePlaylist();
//                    break;
//                }
//            }
//        }
//       return ret;
//    }
}
