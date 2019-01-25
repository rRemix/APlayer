//package remix.myplayer.util;
//
//import static remix.myplayer.util.MediaStoreUtil.getSongInfo;
//
//import android.content.ContentUris;
//import android.content.ContentValues;
//import android.content.Context;
//import android.database.Cursor;
//import android.net.Uri;
//import android.provider.MediaStore;
//import android.text.TextUtils;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import remix.myplayer.App;
//import remix.myplayer.Global;
//import remix.myplayer.bean.mp3.PlayList;
//import remix.myplayer.bean.mp3.PlayListSong;
//import remix.myplayer.bean.mp3.Song;
//import remix.myplayer.db.DBManager;
//import remix.myplayer.db.PlayListSongs;
//import remix.myplayer.db.PlayLists;
//import remix.myplayer.helper.SortOrder;
//
//
///**
// * @ClassName
// * @Description
// * @Author Xiaoborui
// * @Date 2016/10/17 10:24
// */
//public class PlayListUtil {
//
//  private static final String TAG = "PlayListUtil";
//  private static Context mContext;
//
//  private PlayListUtil() {
//  }
//
//  static {
//    mContext = App.getContext();
//  }
//
//  /**
//   * 新增播放列表
//   *
//   * @return 新增的播放列表id -1:播放列表名字为空 -2:该播放列表已经存在
//   */
//  public static int addPlayList(String playListName) {
////      if (TextUtils.isEmpty(playListName)) {
////          return -1;
////      }
////    for (PlayList info : ) {
////        if (info.Name.equals(playListName)) {
////            return -2;
////        }
////    }
////    ContentValues cv = new ContentValues();
////    cv.put(PlayLists.PlayListColumns.COUNT, 0);
////    cv.put(PlayLists.PlayListColumns.NAME, playListName);
////    cv.put(PlayLists.PlayListColumns.DATE, System.currentTimeMillis());
////    Uri uri = mContext.getContentResolver().insert(PlayLists.CONTENT_URI, cv);
////    return uri != null ? (int) ContentUris.parseId(uri) : -1;
//    return 1;
//  }
//
//  /**
//   * 删除一个播放列表
//   */
//  public static boolean deletePlayList(int id) {
//    return id > 0 && mContext.getContentResolver()
//        .delete(PlayLists.CONTENT_URI, PlayLists.PlayListColumns._ID + "=?", new String[]{id + ""}) > 0;
//  }
//
////    /**
////     * 删除多个播放列表
////     *
////     * @param IdList
////     * @return
////     */
////    public static int deleteMultiPlayList(List<Integer> IdList) {
////        if (IdList == null || IdList.size() == 0)
////            return 0;
////        StringBuilder where = new StringBuilder();
////        String[] whereArgs = new String[IdList.size()];
////        for (int i = 0; i < IdList.size(); i++) {
////            whereArgs[i] = IdList.get(i) + "";
////            where.append(PlayLists.PlayListColumns._ID + "=?");
////            if (i != IdList.size() - 1) {
////                where.append(" or ");
////            }
////        }
////        return mContext.getContentResolver().deleteSongs(PlayLists.CONTENT_URI, where.toString(), whereArgs);
////    }
//
//  /**
//   * 添加一首歌曲
//   *
//   * @return 新增歌曲的id
//   */
//  public static int addSong(PlayListSong info) {
//      if (getSongIds(info.PlayListID).contains(info.AudioId)) {
//          return 0;
//      }
//    ContentValues cv = new ContentValues();
//    cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID, info.AudioId);
//
//    cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID, info.PlayListID);
//    cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME, info.PlayListName);
//    Uri uri = mContext.getContentResolver().insert(PlayListSongs.CONTENT_URI, cv);
//    return uri != null ? (int) ContentUris.parseId(uri) : -1;
//  }
//
//  /**
//   * 添加多首歌曲
//   */
//  public static int addMultiSongs(List<PlayListSong> infos) {
//      if (infos == null || infos.size() == 0) {
//          return 0;
//      }
//    //不重复添加
//    List<Integer> rawIDList = getSongIds(infos.get(0).PlayListID);
//    for (int i = 0; i < rawIDList.size(); i++) {
//      for (int j = infos.size() - 1; j >= 0; j--) {
//          if (rawIDList.get(i) == infos.get(j).AudioId) {
//              infos.remove(j);
//          }
//      }
//    }
//
//    ContentValues[] values = new ContentValues[infos.size()];
//    for (int i = 0; i < infos.size(); i++) {
//      ContentValues cv = new ContentValues();
//      PlayListSong info = infos.get(i);
//      cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME, info.PlayListName);
//      cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID, info.PlayListID);
//      cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID, info.AudioId);
//      values[i] = cv;
//    }
//    return mContext.getContentResolver().bulkInsert(PlayListSongs.CONTENT_URI, values);
//  }
//
//  /**
//   * 添加多首歌曲
//   */
//  public static int addMultiSongs(List<Integer> IDList, String playListName) {
//    return addMultiSongs(IDList, playListName, getPlayListID(playListName));
//  }
//
//  /**
//   * 添加多首歌曲
//   */
//  public static int addMultiSongs(List<Integer> ids, String playListName, int playListId) {
//      if (ids == null || ids.size() == 0) {
//          return 0;
//      }
//    List<Integer> newList = new ArrayList<>(ids);
//    //不重复添加
//    List<Integer> rawIDList = getSongIds(playListName);
//    for (int i = 0; i < rawIDList.size(); i++) {
//      for (int j = newList.size() - 1; j >= 0; j--) {
//          if (rawIDList.get(i).equals(newList.get(j))) {
//              newList.remove(j);
//          }
//      }
//    }
//    ContentValues[] values = new ContentValues[newList.size()];
//    for (int i = 0; i < newList.size(); i++) {
//      ContentValues cv = new ContentValues();
//      cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME, playListName);
//      cv.put(PlayListSongs.PlayListSongColumns.PLAY_LIST_ID, playListId);
//      cv.put(PlayListSongs.PlayListSongColumns.AUDIO_ID, newList.get(i));
//      values[i] = cv;
//    }
//    return mContext.getContentResolver().bulkInsert(PlayListSongs.CONTENT_URI, values);
//  }
//
//  /**
//   * 删除一首歌曲
//   */
//  public static int deleteSongs(List<Song> songs) {
//    StringBuilder selectionBuilder = new StringBuilder(128);
//    String[] selectionArgs = new String[songs.size()];
//    int i = 0;
//    for (Song song : songs) {
//      selectionBuilder.append(PlayListSongs.PlayListSongColumns.AUDIO_ID + " = ?")
//          .append(i != songs.size() - 1 ? " or " : " ");
//      selectionArgs[i] = song.getId() + "";
//      i++;
//    }
//    return mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,
//        selectionBuilder.toString(), selectionArgs);
//  }
//
//  /**
//   * 删除一首歌曲
//   */
//  public static boolean deleteSong(int audioId, int playListId) {
//    return audioId > 0 &&
//        mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,
//            PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?" + " and "
//                + PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?",
//            new String[]{audioId + "", playListId + ""}) > 0;
//  }
//
//  /**
//   * 删除一首歌曲
//   */
//  public static boolean deleteSong(int audioId, String playListName) {
//    return audioId > 0 && !TextUtils.isEmpty(playListName) &&
//        mContext.getContentResolver().delete(PlayListSongs.CONTENT_URI,
//            PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?" + " and "
//                + PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME + "=?",
//            new String[]{audioId + "", playListName}) > 0;
//  }
//
//  /**
//   * 删除表下所有数据
//   */
//  public static int clearTable(String tablename) {
//    return mContext.getContentResolver()
//        .delete(PlayListSongs.CONTENT_URI, PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME + "=?",
//            new String[]{tablename});
//  }
//
//  /**
//   * 删除多首歌曲
//   */
//  public static int deleteMultiSongs(List<Integer> audioIdList, int playlistId) {
//    try {
//        if (audioIdList == null || audioIdList.size() == 0) {
//            return 0;
//        }
//      StringBuilder where = new StringBuilder();
//      String[] whereArgs = new String[audioIdList.size() + 1];
//      for (int i = 0; i < audioIdList.size() + 1; i++) {
//        if (i != audioIdList.size()) {
//          if (i == 0) {
//            where.append("(");
//          }
//          where.append(PlayListSongs.PlayListSongColumns.AUDIO_ID + "=?");
//          if (i != audioIdList.size() - 1) {
//            where.append(" or ");
//          }
//          whereArgs[i] = audioIdList.get(i) + "";
//        } else {
//          where.append(") and " + PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?");
//          whereArgs[i] = playlistId + "";
//        }
//      }
//      return mContext.getContentResolver()
//          .delete(PlayListSongs.CONTENT_URI, where.toString(), whereArgs);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    return 0;
//  }
//
//
//  /**
//   * 根据播放列表id获得对应的名字
//   */
//  public static String getPlayListName(int playListId) {
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver()
//          .query(PlayLists.CONTENT_URI, new String[]{PlayLists.PlayListColumns.NAME},
//              PlayLists.PlayListColumns._ID + "=?", new String[]{playListId + ""}, null, null);
//        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
//            return cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
//        }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
//    }
//    return "";
//  }
//
//  /**
//   * 根据播放列表名获得播放列表id
//   */
//  public static int getPlayListID(String playListName) {
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver()
//          .query(PlayLists.CONTENT_URI, new String[]{PlayLists.PlayListColumns._ID},
//              PlayLists.PlayListColumns.NAME + "=?", new String[]{playListName}, null, null);
//        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
//            return cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
//        }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
//    }
//    return -1;
//  }
//
//  /**
//   * 返回系统中除播放队列所有播放列表
//   */
//  public static List<PlayList> getAllPlayListInfo() {
//    ArrayList<PlayList> playList = new ArrayList<>();
//    Cursor cursor = null;
//    try {
//      String sortOrder = SPUtil
//          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
//              SortOrder.PlayListSortOrder.PLAYLIST_DATE);
//      LogUtil.d("QueryPlayList", "SortOrder: " + sortOrder);
//      cursor = DBManager.getInstance().openDataBase().rawQuery(
//          "select * from play_list where name != '" + Constants.PLAY_QUEUE + "' order by "
//              + sortOrder, null);
////            cursor = mContext.getContentResolver().query(PlayLists.CONTENT_URI,null,PlayLists.PlayListColumns.NAME + "!= ?",
////                    new String[]{Constants.PLAY_QUEUE},
////                    sortOrder);
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          PlayList info = new PlayList();
//          info._Id = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
//          info.Count = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.COUNT));
//          info.Name = cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
//          info.Date = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.DATE));
//          playList.add(info);
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
//      DBManager.getInstance().closeDataBase();
//    }
//    LogUtil.d("QueryPlayList", playList + "");
//    return playList;
//  }
//
//  public static List<PlayListSong> getPlayListSong(int playlistId) {
//    List<PlayListSong> playListSongs = new ArrayList<>();
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,
//          new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
//          PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?", new String[]{playlistId + ""},
//          null);
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          PlayListSong song = new PlayListSong();
//          song.AudioId = cursor
//              .getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID));
//          playListSongs.add(song);
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
//    }
//    return playListSongs;
//  }
//
//  /**
//   * 根据播放列表名获得歌曲id列表
//   */
//  public static List<Integer> getSongIds(String playlistName) {
//    ArrayList<Integer> IDList = new ArrayList<>();
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,
//          new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
//          PlayListSongs.PlayListSongColumns.PLAY_LIST_NAME + "=?", new String[]{playlistName}, null,
//          null);
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          IDList.add(
//              cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID)));
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//      if (cursor != null && !cursor.isClosed()) {
//        cursor.close();
//      }
//    }
//    return IDList;
//  }
//
//  /**
//   * 根据播放列表id获得歌曲id列表
//   */
//  public static List<Integer> getSongIds(int playlistId) {
//    ArrayList<Integer> IDList = new ArrayList<>();
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI,
//          new String[]{PlayListSongs.PlayListSongColumns.AUDIO_ID},
//          PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=?", new String[]{playlistId + ""},
//          null, null);
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          IDList.add(
//              cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID)));
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//      if (cursor != null && !cursor.isClosed()) {
//        cursor.close();
//      }
//    }
//    return IDList;
//  }
//
//  /**
//   * @param cursor
//   * @return
//   */
//  public static PlayList getPlayListInfo(Cursor cursor) {
//    PlayList info = new PlayList();
//    try {
//      info.Count = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.COUNT));
//      info._Id = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns._ID));
//      info.Name = cursor.getString(cursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
//      info.Date = cursor.getInt(cursor.getColumnIndex(PlayLists.PlayListColumns.COUNT));
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    return info;
//  }
//
//  public static List<Song> getMP3ListWithSort(List<Integer> idList, int playlistId) {
//    return getMP3ListWithSort(idList,playlistId,true);
//  }
//
//    /**
//     * 根据多个歌曲id返回多个歌曲详细信息
//     *
//     * @param idList 歌曲id列表
//     * @param playlistId 播放列表id
//     * @return 对应所有歌曲信息列表
//     */
//  public static List<Song> getMP3ListWithSort(List<Integer> idList, int playlistId,boolean force) {
//      if (idList == null || idList.size() == 0) {
//          return new ArrayList<>();
//      }
//
//    StringBuilder selection = new StringBuilder(127);
//    selection.append(MediaStore.Audio.Media._ID + " in (");
//    StringBuilder sortOrder = new StringBuilder(SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
//        SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
//        SortOrder.PlayListSongSortOrder.SONG_A_Z));
////        //自定义排序 sqlite不支持find_in_set
////        if(sortOrder.toString().equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)){
////            sortOrder.deleteSongs(0,sortOrder.length());
////            sortOrder.append("find_in_set(" + MediaStore.Audio.Media._ID + ",'");
////            for(int i = 0 ; i < idList.size();i++){
////                sortOrder.append(idList.get(i)).append( i == idList.size() - 1 ? "') " : ",");
////            }
////        }
//    boolean isCustom = force || sortOrder.toString()
//        .equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM);
//    if (isCustom) {
//      sortOrder.delete(0, sortOrder.length());
//    }
//    for (int i = 0; i < idList.size(); i++) {
//      selection.append(idList.get(i)).append(i == idList.size() - 1 ? ") " : ",");
//    }
//    Cursor cursor = null;
//    List<Song> songList = new ArrayList<>(idList.size());
//    //处理自定义排序
//    Song[] tempArray = new Song[idList.size()];
//    try {
//      cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//          null,
//          selection.toString(),
//          null,
//          sortOrder.toString());
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          Song temp = getSongInfo(cursor);
//          tempArray[isCustom ? idList.indexOf(temp.getId()) : cursor.getPosition()] = temp;
//        }
//
//        //找到不存在的歌曲
//        for (Song temp : tempArray) {
//            if (temp != null) {
//                songList.add(temp);
//            }
//        }
//
//        //有不存在的歌曲 从播放列表移除
//        if (songList.size() < idList.size()) {
//          List<Integer> deleteId = new ArrayList<>();
//          for (Integer id : idList) {
//            Song deleteSong = new Song(id);
//            if (!songList.contains(deleteSong)) {
//              deleteId.add(id);
//            }
//          }
//          //从播放列表移除
//          PlayListUtil.deleteMultiSongs(deleteId, playlistId);
//        }
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//      if (cursor != null && !cursor.isClosed()) {
//        cursor.close();
//      }
//    }
//
////        for(Integer id : idList){
////            Song temp = MediaStoreUtil.getSongById(id);
////            if(temp != null && temp.getID() == id){
////                songList.add(temp);
////            } else {
////                //如果外部删除了某些歌曲 手动添加歌曲信息，保证点击播放列表前后歌曲数目一致
////                Song item = new Song();
////                item.Title = mContext.getString(R.string.song_lose_effect);
////                item.Id = id;
////                songList.add(item);
////            }
////        }
//    return songList;
//  }
//
//  /**
//   * 获得所有移除歌曲的id
//   */
//  public static String getDeleteID() {
//    Set<String> deleteId = SPUtil
//        .getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG);
//      if (deleteId == null || deleteId.size() == 0) {
//          return "";
//      }
//    StringBuilder stringBuilder = new StringBuilder();
//    stringBuilder.append(" and ");
//    int i = 0;
//    for (String id : deleteId) {
//      stringBuilder.append(PlayListSongs.PlayListSongColumns.AUDIO_ID + " != ").append(id)
//          .append(i != deleteId.size() - 1 ? " and " : " ");
//      i++;
//    }
//    return stringBuilder.toString();
//  }
//
//  /**
//   * 是否收藏了该歌曲
//   */
//  public static final int EXIST = 1;
//  public static final int NONEXIST = 2;
//  public static final int ERROR = 3;
//
//  public static int isLove(int audioId) {
//    Cursor cursor = null;
//    try {
//      cursor = mContext.getContentResolver().query(PlayListSongs.CONTENT_URI, null,
//          PlayListSongs.PlayListSongColumns.PLAY_LIST_ID + "=" + 1, null, null);
//      if (cursor != null && cursor.getCount() > 0) {
//        while (cursor.moveToNext()) {
//          int id = cursor.getInt(cursor.getColumnIndex(PlayListSongs.PlayListSongColumns.AUDIO_ID));
//          if (id == audioId) {
//            return EXIST;
//          }
//        }
//      }
//    } catch (Exception e) {
//      return ERROR;
//    } finally {
//        if (cursor != null && !cursor.isClosed()) {
//            cursor.close();
//        }
//    }
//    return NONEXIST;
//  }
//
//  public static Map<String, List<Integer>> getPlaylistFromMediaStore() {
//    final Map<String, List<Integer>> map = new HashMap<>();
//    Cursor playlistCursor = null;
//    Cursor songCursor = null;
//    try {
//      playlistCursor = mContext.getContentResolver()
//          .query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
//              null, null, null, null);
//        if (playlistCursor == null || playlistCursor.getCount() == 0) {
//            return map;
//        }
//      while (playlistCursor.moveToNext()) {
//        List<Integer> helperList = new ArrayList<>();
//        songCursor = mContext.getContentResolver().query(MediaStore.Audio.Playlists.Members
//                .getContentUri("external", playlistCursor
//                    .getInt(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID))),
//            null, null, null, null);
//        if (songCursor != null) {
//          while (songCursor.moveToNext()) {
//            helperList.add(songCursor
//                .getInt(songCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)));
//          }
//        }
//        map.put(playlistCursor
//                .getString(playlistCursor.getColumnIndex(MediaStore.Audio.PlaylistsColumns.NAME)),
//            helperList);
//        songCursor = null;
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    } finally {
//        if (playlistCursor != null && !playlistCursor.isClosed()) {
//            playlistCursor.close();
//        }
//        if (songCursor != null && !songCursor.isClosed()) {
//            songCursor.close();
//        }
//    }
//    return map;
//  }
//
//  /**
//   * 重命名播放列表
//   */
//  public static boolean rename(int playlistId, String playlistName) {
//    ContentValues values = new ContentValues();
//    values.put(PlayLists.PlayListColumns.NAME, playlistName);
//    try {
//      return mContext.getContentResolver().update(PlayLists.CONTENT_URI,
//          values, PlayLists.PlayListColumns._ID + "=" + playlistId, null) > 0;
//    } catch (Exception e) {
//      return false;
//    }
//
//  }
//}
