package remix.myplayer.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Set;

import remix.myplayer.R;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.db.PlayLists;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.PlayListInfo;
import remix.myplayer.model.PlayListSongInfo;



/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/17 10:24
 */
public class PlayListUtil {
    private static final String TAG = "PlayListUtil";
    private static Context mContext;
    private PlayListUtil(){}
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 新增播放列表
     * @param playListName
     * @return 新增的播放列表id -1:播放列表名字为空 -2:该播放列表已经存在
     */
    public static int addPlayList(String playListName) {
        if(TextUtils.isEmpty(playListName))
            return -1;
        for(PlayListInfo info : Global.PlayList){
            if(info.Name.equals(playListName))
                return -2;
        }
        ContentValues cv = new ContentValues();
        cv.put(PlayLists.PlayListColumns.COUNT, 0);
        cv.put(PlayLists.PlayListColumns.NAME, playListName);
        Uri uri = mContext.getContentResolver().insert(PlayLists.CONTENT_URI, cv);
        return uri != null ? (int) ContentUris.parseId(uri) : -1;
    }

    /**
     * 删除一个播放列表
     * @param id
     * @return
     */
    public static boolean deletePlayList(int id){
        return id > 0 && mContext.getContentResolver().delete(PlayLists.CONTENT_URI, PlayLists.PlayListColumns._ID + "=?",new String[]{id + ""}) > 0;
    }

    /**
     * 删除多个播放列表
     * @param IdList
     * @return
     */
    public static int deleteMultiPlayList(ArrayList<Integer> IdList){
        if(IdList == null || IdList.size() == 0)
            return 0;
        String where = "";
        String[] whereArgs = new String[IdList.size()];
        for(int i = 0 ; i < IdList.size() ;i++){
            whereArgs[i] = IdList.get(i) + "";
            where += (PlayLists.PlayListColumns._ID + "=?");
            if(i != IdList.size() - 1){
                where += " or ";
            }
        }
        return mContext.getContentResolver().delete(PlayLists.CONTENT_URI,where,whereArgs);
    }

    /**
     * 添加一首歌曲
     * @param info
     * @return 新增歌曲的id
     */
    public static int addSong(PlayListSongInfo info){
        if(getIDList(info.PlayListID).contains(info.AudioId))
            return 0;
        ContentValues cv = new ContentValues();
        cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID,info.AudioId);

        cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID,info.PlayListID);
        cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME,info.PlayListName);
        Uri uri = mContext.getContentResolver().insert(PlayListSongs.CONTENT_URI, cv);
        return uri != null ? (int) ContentUris.parseId(uri) : -1;
    }

    /**
     * 添加多首歌曲
     * @param infos
     * @return
     */
    public static int addMultiSongs(ArrayList<PlayListSongInfo> infos){
        if(infos == null || infos.size() == 0 )
            return 0;
        //不重复添加
        ArrayList<Integer> rawIDList = getIDList(infos.get(0).PlayListID);
        for(int i = 0 ; i < rawIDList.size() ;i++){
            for(int j = infos.size() - 1; j >= 0; j--){
                if(rawIDList.get(i) == infos.get(j).AudioId)
                    infos.remove(j);
            }
        }

        ContentValues[] values = new ContentValues[infos.size()];
        for(int i = 0 ; i < infos.size() ;i++){
            ContentValues cv = new ContentValues();
            PlayListSongInfo info = infos.get(i);
            cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME,info.PlayListName);
            cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID,info.PlayListID);
            cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID,info.AudioId);
            values[i] = cv;
        }
        return mContext.getContentResolver().bulkInsert(PlayListSongs.CONTENT_URI,values);
    }

    /**
     * 添加多首歌曲
     * @param IDList
     * @return
     */
    public static int addMultiSongs(ArrayList<Integer> IDList,String playListName){
        return addMultiSongs(IDList,playListName,getPlayListID(playListName));
    }

    /**
     * 添加多首歌曲
     * @param IDList
     * @return
     */
    public static int addMultiSongs(ArrayList<Integer> IDList,String playListName,int playListId){
        if(IDList == null || IDList.size() == 0 )
            return 0;
        //不重复添加
        ArrayList<Integer> rawIDList = getIDList(playListName);
        for(int i = 0 ; i < rawIDList.size() ;i++){
            for(int j = IDList.size() - 1; j >= 0; j--){
                if(rawIDList.get(i).equals(IDList.get(j)))
                    IDList.remove(j);
            }
        }
        ContentValues[] values = new ContentValues[IDList.size()];
        for(int i = 0 ; i < IDList.size() ;i++){
            ContentValues cv = new ContentValues();
            cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME,playListName);
            cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID,playListId);
            cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID,IDList.get(i));
            values[i] = cv;
        }
        return mContext.getContentResolver().bulkInsert(PlayListSongs.CONTENT_URI,values);
    }

    /**
     * 删除一首歌曲
     * @param audioId
     * @param playListId
     * @return
     */
    public static boolean deleteSong(int audioId,int playListId){
        return audioId > 0 &&
                mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,
                        PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?" + " and " + PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?",
                        new String[]{audioId + "",playListId + ""}) > 0;
    }

    /**
     * 删除一首歌曲
     * @param audioId
     * @param playListName
     * @return
     */
    public static boolean deleteSong(int audioId,String playListName){
        return audioId > 0 && !TextUtils.isEmpty(playListName) &&
                mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,
                        PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?" + " and " + PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME + "=?",
                        new String[]{audioId + "",playListName}) > 0;
    }

    /**
     * 删除多首歌曲
     * @param IdList
     * @param playlistId
     * @return
     */
    public static int deleteMultiSongs(ArrayList<Integer> IdList,int playlistId){
        if(IdList == null || IdList.size() == 0)
            return 0;
        String where = "";
        String[] whereArgs = new String[IdList.size() + 1];
        for(int i = 0 ; i < IdList.size() + 1;i++) {
            if (i != IdList.size()) {
                if (i == 0) {
                    where += "(";
                }
                where += (PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?");
                if (i != IdList.size() - 1) {
                    where += " or ";
                }
                whereArgs[i] = IdList.get(i) + "";
            }else {
                where += (") and " + PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?");
                whereArgs[i] = playlistId + "";
            }
        }
        return mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,where,whereArgs);
    }


    /**
     * 根据播放列表id获得对应的名字
     * @param playListId
     * @return
     */
    public static String getPlayListName(int playListId){
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayLists.CONTENT_URI,new String[]{PlayLists.PlayListColumns.NAME},
                    PlayLists.PlayListColumns._ID + "=?",new String[]{playListId + ""},null,null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
                return cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
        } catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 根据播放列表名获得播放列表id
     * @param playListName
     * @return
     */
    public static int getPlayListID(String playListName){
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayLists.CONTENT_URI,new String[]{PlayLists.PlayListColumns._ID},
                    PlayLists.PlayListColumns.NAME + "=?",new String[]{playListName},null,null);
            if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
                return cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 返回系统中除播放队列所有播放列表
     * @return
     */
    public static ArrayList<PlayListInfo> getAllPlayListInfo(){
        ArrayList<PlayListInfo> playList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayLists.CONTENT_URI,null,PlayLists.PlayListColumns.NAME + "!= ?",
                    new String[]{Constants.PLAY_QUEUE},null);
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    PlayListInfo info = new PlayListInfo();
                    info._Id = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
                    info.Count = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.COUNT));
                    info.Name = cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
                    playList.add(info);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return playList;
    }

    /**
     * 根据播放列表名获得歌曲id列表
     * @param playlistName
     * @return
     */
    public static ArrayList<Integer> getIDList(String playlistName){
        ArrayList<Integer> IDList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
                    PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME + "=?",new String[]{playlistName},null,null);
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    IDList.add(cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID)));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return IDList;
    }

    /**
     * 根据播放列表id获得歌曲id列表
     * @param playlistId
     * @return
     */
    public static ArrayList<Integer> getIDList(int playlistId){
        ArrayList<Integer> IDList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
                    PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?",new String[]{playlistId + ""},null,null);
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    IDList.add(cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID)));
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }
        return IDList;
    }

    /**
     *
     * @param cursor
     * @return
     */
    public static PlayListInfo getPlayListInfo(Cursor cursor){
        PlayListInfo info = new PlayListInfo();
        try {
            info.Count = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.COUNT));
            info._Id = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
            info.Name = cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
        } catch (Exception e){
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 根据多个歌曲id返回多个歌曲详细信息
     * @param idList 歌曲id列表
     * @return 对应所有歌曲信息列表
     */
    public static ArrayList<MP3Item> getMP3ListByIds(ArrayList<Integer> idList) {
        if(idList == null || idList.size() == 0)
            return new ArrayList<>();

        ArrayList<MP3Item> mp3list = new ArrayList<>();

        for(Integer id : idList){
            MP3Item temp = MediaStoreUtil.getMP3InfoById(id);
            if(temp != null && temp.getId() == id){
                mp3list.add(temp);
            } else {
                //如果外部删除了某些歌曲 手动添加歌曲信息，保证点击播放列表前后歌曲数目一致
                MP3Item item = new MP3Item();
                item.Title = mContext.getString(R.string.song_lose_effect);
                item.Id = id;
                mp3list.add(item);
            }
        }
        return mp3list;
    }

    /**
     * 获得所有移除歌曲的id
     * @return
     */
    public static String getDeleteID(){
        Set<String> deleteId = SPUtil.getStringSet(mContext,"Setting","DeleteID");
        if(deleteId == null || deleteId.size() == 0)
            return "";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" and ");
        int i = 0;
        for (String id : deleteId) {
            stringBuilder.append(PlayListSongs.PlayListSongColumns.AUDIO_ID + " != ").append(id).append(i != deleteId.size() - 1 ?  " and " : " ");
            i++;
        }
        return stringBuilder.toString();
    }

    /**
     * 是否收藏了该歌曲
     */
    public static final int EXIST = 1;
    public static final int NONEXIST = 2;
    public static final int ERROR = 3;
    public static int isLove(int audioId){
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,null, PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=" + Global.MyLoveID ,null,null);
            if(cursor != null && cursor.getCount() > 0){
                while (cursor.moveToNext()){
                    int id = cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID));
                    if(id == audioId){
                        return EXIST;
                    }
                }
            }
        } catch (Exception e){
            return ERROR;
        } finally {
            if(cursor != null && !cursor.isClosed())
                cursor.close();
        }
        return NONEXIST;
    }
}
