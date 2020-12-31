package remix.myplayer.util;

import static remix.myplayer.util.Util.TYPE_ALBUM;
import static remix.myplayer.util.Util.TYPE_ARTIST;
import static remix.myplayer.util.Util.TYPE_DISPLAYNAME;
import static remix.myplayer.util.Util.TYPE_SONG;
import static remix.myplayer.util.Util.hasStoragePermissions;
import static remix.myplayer.util.Util.processInfo;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.text.TextUtils;
import com.facebook.common.util.ByteConstants;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.bean.mp3.Folder;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import timber.log.Timber;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 数据库工具类
 */
public class MediaStoreUtil {

  private static final String TAG = "MediaStoreUtil";
  @SuppressLint("StaticFieldLeak")
  private static Context mContext;

  //扫描文件默认大小设置
  public static int SCAN_SIZE;

  static {
    mContext = App.getContext();

    SCAN_SIZE = SPUtil
        .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE,
            ByteConstants.MB);
  }

  private MediaStoreUtil() {
  }

  private static final String[] BASE_PROJECTION = new String[]{
      BaseColumns._ID,
      AudioColumns.TITLE,
      AudioColumns.TITLE_KEY,
      AudioColumns.DISPLAY_NAME,
      AudioColumns.TRACK,
      AudioColumns.SIZE,
      AudioColumns.YEAR,
      AudioColumns.DURATION,
      AudioColumns.DATE_ADDED,
      AudioColumns.DATA,
      AudioColumns.ALBUM_ID,
      AudioColumns.ALBUM,
      AudioColumns.ARTIST_ID,
      AudioColumns.ARTIST,
  };

  public static List<Artist> getAllArtist() {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }

    final Map<Long, List<Artist>> artistMaps = new LinkedHashMap<>();
    final List<Artist> artists = new ArrayList<>();
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[]{MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST},
            getBaseSelection(),
            getBaseSelectionArgs(),
            SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
                SortOrder.ArtistSortOrder.ARTIST_A_Z))) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          try {
            long artistId = cursor.getLong(0);
            if (artistMaps.get(artistId) == null) {
              artistMaps.put(artistId, new ArrayList<>());
            }
            artistMaps.get(artistId).add(new Artist(artistId, cursor.getString(1), 0));
          } catch (Exception ignored) {
          }
        }

        for (Entry<Long, List<Artist>> entry : artistMaps.entrySet()) {
          try {
            final Artist artist = entry.getValue().get(0);
            artist.setCount(entry.getValue().size());
            artists.add(artist);
          } catch (Exception e) {
            Timber.v("addArtist failed: " + e);
          }
        }
      }
    } catch (Exception e) {
      Timber.v("getAllArtist failed: " + e);
    }

    return artists;
  }

  public static List<Album> getAllAlbum() {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }
    final Map<Long, List<Album>> albumMaps = new LinkedHashMap<>();
    final List<Album> albums = new ArrayList<>();
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[]{MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST},
            getBaseSelection(),
            getBaseSelectionArgs(),
            SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
                SortOrder.AlbumSortOrder.ALBUM_A_Z))) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          try {
            long albumId = cursor.getLong(0);
            if (albumMaps.get(albumId) == null) {
              albumMaps.put(albumId, new ArrayList<>());
            }
            albumMaps.get(albumId).add(new Album(albumId,
                cursor.getString(1),
                cursor.getLong(2),
                cursor.getString(3),
                0));
          } catch (Exception ignored) {
          }
        }

        for (Entry<Long, List<Album>> entry : albumMaps.entrySet()) {
          try {
            final Album album = entry.getValue().get(0);
            album.setCount(entry.getValue().size());
            albums.add(album);
          } catch (Exception e) {
            Timber.v("addAlbum failed: " + e);
          }
        }

      }
    } catch (Exception e) {
      Timber.v("getAllAlbum failed: " + e);
    }
    return albums;
  }


  public static List<Song> getAllSong() {
    return getSongs(null,
        null,
        SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
            SortOrder.SongSortOrder.SONG_A_Z));

  }

  public static List<Song> getLastAddedSong() {
    Calendar today = Calendar.getInstance();
    today.setTime(new Date());
    return getSongs(MediaStore.Audio.Media.DATE_ADDED + " >= ?",
        new String[]{String.valueOf((today.getTimeInMillis() / 1000 - (3600 * 24 * 7)))},
        MediaStore.Audio.Media.DATE_ADDED);
  }

  /**
   * 获得所有歌曲id
   */
  public static List<Integer> getAllSongsId() {
    return getSongIds(
        null,
        null,
        SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
            SortOrder.SongSortOrder.SONG_A_Z));
  }

  public static List<Folder> getAllFolder() {
    List<Folder> folders = new ArrayList<>();
    if (!hasStoragePermissions()) {
      return folders;
    }
    Map<Integer, List<String>> folderMap = new LinkedHashMap<>();

    final String baseSelection = getBaseSelection();
    final String selection =
        !TextUtils.isEmpty(baseSelection) ? baseSelection + " and media_type = 2"
            : "media_type = 2";
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Files.getContentUri("external"),
            null, selection, getBaseSelectionArgs(), null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          final String data = cursor
              .getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
          final int parentId = cursor
              .getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));
          final String parentPath = data.substring(0, data.lastIndexOf("/"));

          if (!folderMap.containsKey(parentId)) {
            folderMap.put(parentId, new ArrayList<>(Collections.singletonList(parentPath)));
          } else {
            folderMap.get(parentId).add(parentPath);
          }
        }

        //转换
        for (Map.Entry<Integer, List<String>> entry : folderMap.entrySet()) {
          final String parentPath = entry.getValue().get(0);
          Folder folder = new Folder(
              parentPath.substring(parentPath.lastIndexOf("/") + 1),
              folderMap.get(entry.getKey()).size(),
              parentPath,
              entry.getKey());
          folders.add(folder);
        }

      }
    } catch (Exception e) {
      Timber.v(e);
    }

    return folders;
  }


  /**
   * 根据歌手或者专辑id获取所有歌曲
   *
   * @param id 歌手id 专辑id
   * @param type 1:专辑  2:歌手
   * @return 对应所有歌曲的id
   */
  public static List<Song> getSongsByArtistIdOrAlbumId(long id, int type) {
    String selection = null;
    String sortOrder = null;
    String[] selectionValues = null;
    if (type == Constants.ALBUM) {
      selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
      selectionValues = new String[]{id + ""};
      sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    }
    if (type == Constants.ARTIST) {
      selection = MediaStore.Audio.Media.ARTIST_ID + "=?";
      selectionValues = new String[]{id + ""};
      sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    }
    return getSongs(selection, selectionValues, sortOrder);
  }

  /**
   * 根据记录集获得歌曲详细
   *
   * @param cursor 记录集
   * @return 拼装后的歌曲信息
   */
  @WorkerThread
  public static Song getSongInfo(Cursor cursor) {
    if (cursor == null || cursor.getColumnCount() <= 0) {
      return Song.Companion.getEMPTY_SONG();
    }

    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
    final String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
    final int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

    Song song = new Song(
        id,
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
            TYPE_DISPLAYNAME),
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
            TYPE_SONG),
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
            TYPE_ALBUM),
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
            TYPE_ARTIST),
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
        duration,
        Util.getTime(duration),
        data,
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)),
        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)),
        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY)),
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)));
    return song;
  }

  public static List<Integer> getSongIdsByParentId(long parentId) {
    List<Integer> ids = new ArrayList<>();
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Files.getContentUri("external"),
            new String[]{"_id"}, "parent = " + parentId, null, null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          ids.add(cursor.getInt(0));
        }
      }
    }
    return ids;
  }

  /**
   * 根据文件夹名字
   */
  public static List<Song> getSongsByParentId(long parentId) {
    List<Integer> ids = getSongIdsByParentId(parentId);

    if (ids.size() == 0) {
      return new ArrayList<>();
    }
    StringBuilder selection = new StringBuilder(127);
    selection.append(MediaStore.Audio.Media._ID + " in (");
    for (int i = 0; i < ids.size(); i++) {
      selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
    }
    return getSongs(selection.toString(), null, SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
        SortOrder.ChildHolderSongSortOrder.SONG_A_Z));
  }


  /**
   * 根据专辑id查询歌曲详细信息
   *
   * @param albumId 歌曲id
   * @return 对应歌曲信息
   */
  public static Song getSongByAlbumId(long albumId) {
    return getSong(MediaStore.Audio.Media.ALBUM_ID + "=?", new String[]{albumId + ""});
  }

  /**
   * 根据歌曲id查询歌曲详细信息
   *
   * @param id 歌曲id
   * @return 对应歌曲信息
   */
  public static Song getSongById(int id) {
    return getSong(MediaStore.Audio.Media._ID + "=?", new String[]{id + ""});
  }


  public static List<Song> getSongsByIds(List<Integer> ids) {
    List<Song> songs = new ArrayList<>();
    if (ids == null || ids.isEmpty()) {
      return songs;
    }
    StringBuilder selection = new StringBuilder(127);
    selection.append(MediaStore.Audio.Media._ID + " in (");
    for (int i = 0; i < ids.size(); i++) {
      selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
    }

    return getSongs(selection.toString(), null);
  }


  private static void insertAlbumArt(@NonNull Context context, long albumId, String path) {
    ContentResolver contentResolver = context.getContentResolver();

    Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
    contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

    ContentValues values = new ContentValues();
    values.put("album_id", albumId);
    values.put("_data", path);

    contentResolver.insert(artworkUri, values);
  }

  private static void deleteAlbumArt(@NonNull Context context, int albumId) {
    ContentResolver contentResolver = context.getContentResolver();
    Uri localUri = Uri.parse("content://media/external/audio/albumart");
    contentResolver.delete(ContentUris.withAppendedId(localUri, albumId), null, null);
  }

  @WorkerThread
  public static void saveArtwork(Context context, long albumId, File artFile)
      throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException, CannotWriteException {
    Song song = MediaStoreUtil.getSongByAlbumId(albumId);
    if (song == null) {
      return;
    }
    AudioFile audioFile = AudioFileIO.read(new File(song.getUrl()));
    Tag tag = audioFile.getTagOrCreateAndSetDefault();
    Artwork artwork = ArtworkFactory.createArtworkFromFile(artFile);
    tag.deleteArtworkField();
    tag.setField(artwork);
    audioFile.commit();
    insertAlbumArt(context, albumId, artFile.getAbsolutePath());
  }

  /**
   * 删除歌曲
   *
   * @param data 删除参数 包括歌曲id、专辑id、艺术家id、播放列表id、parentId
   * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹、播放列表
   * @return 是否歌曲数量
   */
  @Deprecated
  public static int delete(int data, int type, boolean deleteSource) {
    String where = null;
    String[] arg = null;

    //拼接参数
    switch (type) {
      case Constants.SONG:
        where = MediaStore.Audio.Media._ID + "=?";
        arg = new String[]{data + ""};
        break;
      case Constants.ALBUM:
      case Constants.ARTIST:
        if (type == Constants.ALBUM) {
          where = MediaStore.Audio.Media.ALBUM_ID + "=?";
          arg = new String[]{data + ""};
        } else {
          where = MediaStore.Audio.Media.ARTIST_ID + "=?";
          arg = new String[]{data + ""};
        }
        break;
      case Constants.FOLDER:
        List<Integer> ids = getSongIdsByParentId(data);
        StringBuilder selection = new StringBuilder(127);
//                for(int i = 0 ; i < ids.size();i++){
//                    selection.append(MediaStore.Audio.Media._ID).append(" = ").append(ids.get(i)).append(i != ids.size() - 1 ? " or " : " ");
//                }
        selection.append(MediaStore.Audio.Media._ID + " in (");
        for (int i = 0; i < ids.size(); i++) {
          selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
        }
        where = selection.toString();
        arg = null;
        break;
    }

    return delete(getSongs(where, arg), deleteSource);
  }

  /**
   * 删除指定歌曲
   */
  @WorkerThread
  public static int delete(List<Song> songs, boolean deleteSource) {
    //保存是否删除源文件
    SPUtil.putValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE,
        deleteSource);
    if (songs == null || songs.size() == 0) {
      return 0;
    }

    //删除之前保存的所有移除歌曲id
    Set<String> deleteId = new HashSet<>(
        SPUtil.getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG));
    //保存到sp
    for (Song temp : songs) {
      if (temp != null) {
        deleteId.add(temp.getId() + "");
      }
    }
    SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG,
        deleteId);
    //从播放队列和全部歌曲移除
    MusicServiceRemote.deleteFromService(songs);

    DatabaseRepository.getInstance().deleteFromAllPlayList(songs).subscribe();

    //删除源文件
    if (deleteSource) {
      deleteSource(songs);
    }

    //刷新界面
    mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);

    return songs.size();
  }


  /**
   * 删除源文件
   */
  public static void deleteSource(List<Song> songs) {
    if (songs == null || songs.size() == 0) {
      return;
    }
    for (Song song : songs) {
      mContext.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          MediaStore.Audio.Media._ID + "=?", new String[]{song.getId() + ""});
      Util.deleteFileSafely(new File(song.getUrl()));
    }
  }

  /**
   * 过滤移出的歌曲以及铃声等
   */
  public static String getBaseSelection() {
    Set<String> deleteIds = SPUtil
        .getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG);
    Set<String> blacklist = SPUtil
        .getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SETTING_KEY.BLACKLIST);

    String baseSelection = " _data != '' AND " + Media.SIZE + " > " + SCAN_SIZE;
    if (deleteIds.isEmpty() && blacklist.isEmpty()) {
      return baseSelection;
    }

    final StringBuilder builder = new StringBuilder(baseSelection);
    int i = 0;
    if (!deleteIds.isEmpty()) {
      builder.append(" AND ");
      for (String id : deleteIds) {
        if (i == 0) {
          builder.append(MediaStore.Audio.Media._ID).append(" not in (");
        }
        builder.append(id);
        builder.append(i != deleteIds.size() - 1 ? "," : ")");
        i++;
      }

    }

    if (!blacklist.isEmpty()) {
      builder.append(" AND ");
      i = 0;
      for (String path : blacklist) {
        builder.append(Media.DATA + " NOT LIKE ").append(" ? ");
        builder.append(i != blacklist.size() - 1 ? " AND " : "");
        i++;
      }
    }

    return builder.toString();
  }

  public static String[] getBaseSelectionArgs() {
    Set<String> blacklist = SPUtil
        .getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SETTING_KEY.BLACKLIST);

    String[] selectionArgs = new String[blacklist.size()];
    Iterator<String> iterator = blacklist.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      selectionArgs[i] = iterator.next() + "%";
      i++;
    }
    return selectionArgs;
  }

  /**
   * 根据路径获得歌曲id
   */
  public static int getSongIdByUrl(String url) {
    return getSongId(MediaStore.Audio.Media.DATA + " = ?", new String[]{url});
  }

  public static Song getSongByUrl(String url) {
    return getSong(MediaStore.Audio.Media.DATA + " = ?", new String[]{url});
  }

  /**
   * 设置铃声
   */
  public static void setRing(Context context, int audioId) {
    try {
      ContentValues cv = new ContentValues();
      cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
      cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
      cv.put(MediaStore.Audio.Media.IS_ALARM, false);
      cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
      // 把需要设为铃声的歌曲更新铃声库
      if (mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv,
          MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) > 0) {
        Uri newUri = ContentUris
            .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
        ToastUtil.show(context, R.string.set_ringtone_success);
      } else {
        ToastUtil.show(context, R.string.set_ringtone_error);
      }
    } catch (Exception e) {
      //没有权限
      if (e instanceof SecurityException) {
        ToastUtil.show(context, R.string.please_give_write_settings_permission);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (!Settings.System.canWrite(mContext)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + mContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Util.isIntentAvailable(mContext, intent)) {
              mContext.startActivity(intent);
            }
          }
        }
      }

    }
  }

  @Nullable
  public static Cursor makeSongCursor(@Nullable String selection, String[] selectionValues,
      final String sortOrder) {

    if (selection != null && !selection.trim().equals("")) {
      selection = selection + " AND " + getBaseSelection();
    } else {
      selection = getBaseSelection();
    }

    if (selectionValues == null) {
      selectionValues = new String[0];
    }

    String[] baseSelectionArgs = getBaseSelectionArgs();
    String[] newSelectionValues = new String[selectionValues.length + baseSelectionArgs.length];
    System.arraycopy(selectionValues, 0, newSelectionValues, 0, selectionValues.length);
    if (newSelectionValues.length - selectionValues.length >= 0) {
      System.arraycopy(baseSelectionArgs, 0,
          newSelectionValues, selectionValues.length,
          newSelectionValues.length - selectionValues.length);
    }

    try {
      return mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          BASE_PROJECTION, selection, newSelectionValues, sortOrder);
    } catch (SecurityException e) {
      return null;
    }
  }

  public static int getSongId(@Nullable String selection, String[] selectionValues) {
    List<Song> songs = getSongs(selection, selectionValues, null);
    return songs != null && songs.size() > 0 ? songs.get(0).getId() : -1;
  }


  public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues) {
    return getSongIds(selection, selectionValues, null);
  }

  public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues,
      String sortOrder) {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }

    List<Integer> ids = new ArrayList<>();
    try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
      if (cursor != null && cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
        }
      }
    } catch (Exception ignore) {

    }
    return ids;
  }

  public static Song getSong(@Nullable String selection, String[] selectionValues) {
    List<Song> songs = getSongs(selection, selectionValues, null);
    return songs != null && songs.size() > 0 ? songs.get(0) : Song.getEMPTY_SONG();
  }

  public static List<Song> getSongs(@Nullable String selection, String[] selectionValues,
      final String sortOrder) {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }

    List<Song> songs = new ArrayList<>();

    try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
      if (cursor != null && cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          songs.add(getSongInfo(cursor));
        }
      }
    } catch (Exception e) {
      Timber.v(e);
    }
    return songs;
  }

  public static List<Song> getSongs(@Nullable String selection, String[] selectionValues) {
    return getSongs(selection, selectionValues, SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, null));
  }

  /**
   * 歌曲数量
   */
  public static int getCount() {
    try (Cursor cursor = mContext.getContentResolver()
        .query(Media.EXTERNAL_CONTENT_URI, new String[]{Media._ID}, getBaseSelection(),
            getBaseSelectionArgs(),
            null)) {
      if (cursor != null) {
        return cursor.getCount();
      }
    } catch (Exception e) {
      Timber.v(e);
    }

    return 0;
  }
}
