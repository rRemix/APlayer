package remix.myplayer.util;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import remix.myplayer.App;
import remix.myplayer.bean.lastfm.Image;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.UriRequest;
import timber.log.Timber;

/**
 * Created by Remix on 2017/11/30.
 */

public class ImageUriUtil {

  private ImageUriUtil() {
  }

  private static final String TAG = "ImageUriUtil";


  /**
   * 获得某歌手在本地数据库的封面
   */
  public static File getArtistThumbInMediaCache(int artistId) {
    try (Cursor cursor = App.getContext().getContentResolver()
        .query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            new String[]{MediaStore.Audio.Albums.ALBUM_ART},
            MediaStore.Audio.Media.ARTIST_ID + "=?", new String[]{artistId + ""}, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        String imagePath = cursor.getString(0);
        if (!TextUtils.isEmpty(imagePath)) {
          return new File(imagePath);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 判断某专辑在本地数据库是否有封面
   */
  public static boolean isAlbumThumbExistInMediaCache(Uri uri) {
    boolean exist = false;
    try (InputStream ignored = App.getContext().getContentResolver().openInputStream(uri)) {
      exist = true;
    } catch (Exception ignored) {
    }
    return exist;
  }

  /**
   * 返回自定义的封面
   */
  public static File getCustomThumbIfExist(long id, int type) {
    File img = type == ImageUriRequest.URL_ALBUM ? new File(
        DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/album") + "/" + Util
            .hashKeyForDisk(id + ""))
        : type == ImageUriRequest.URL_ARTIST ? new File(
            DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/artist") + "/" + Util
                .hashKeyForDisk(id + ""))
            : new File(
                DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/playlist") + "/" + Util
                    .hashKeyForDisk(id + ""));
    if (img.exists()) {
      return img;
    }
    return null;
  }

//    /**
//     * 根据歌曲信息构建请求参数
//     * @param song
//     * @return
//     */
//    public static SearchRequest getSearchRequest(Song song, int localType){
//        if(song == null)
//            return SearchRequest.DEFAULT_REQUEST;
//        boolean isTitleAvailable = !TextUtils.isEmpty(song.getTitle()) && !song.getTitle().contains(mContext.getString(R.string.unknown_song));
//        boolean isAlbumAvailable = !TextUtils.isEmpty(song.getAlbum()) && !song.getAlbum().contains(mContext.getString(R.string.unknown_album));
//        boolean isArtistAvailable = !TextUtils.isEmpty(song.getArtist()) && !song.getArtist().contains(mContext.getString(R.string.unknown_artist));
//
//        //歌曲名合法
//        if(isTitleAvailable){
//            //艺术家合法
//            if(isArtistAvailable){
//                return new SearchRequest(song.getAlbumId(),song.getTitle() + "-" + song.getArtist(), SearchRequest.TYPE_NETEASE_SONG,localType);
//            }
//            //专辑名合法
//            if(isAlbumAvailable){
//                return new SearchRequest(song.getAlbumId(),song.getTitle() + "-" + song.getAlbum(), SearchRequest.TYPE_NETEASE_SONG,localType);
//            }
//        }
//        //根据专辑名字查询
//        if(isAlbumAvailable && isArtistAvailable){
//            return new SearchRequest(song.getAlbumId(),song.getArtist() + "-" + song.getAlbum(), SearchRequest.TYPE_NETEASE_SONG,localType);
//        }
//        return SearchRequest.DEFAULT_REQUEST;
//    }

  /**
   * 根据专辑信息构建请求参数
   */
  public static UriRequest getSearchRequest(Album album) {
//        if(album == null)
//            return SearchRequest.DEFAULT_REQUEST;
//        boolean isAlbumAvailable = !TextUtils.isEmpty(album.getAlbum()) && !album.getAlbum().contains(mContext.getString(R.string.unknown_album));
//        boolean isArtistAvailable = !TextUtils.isEmpty(album.getArtist()) && !album.getArtist().contains(mContext.getString(R.string.unknown_artist));
//        if(isAlbumAvailable && isArtistAvailable){
//            return new SearchRequest(album.getAlbumID(),album.getArtist() + "-" + album.getAlbum(), SearchRequest.TYPE_NETEASE_SONG,ImageUriRequest.URL_ALBUM);
//        }
//        return SearchRequest.DEFAULT_REQUEST;
    if (album == null) {
      return UriRequest.DEFAULT_REQUEST;
    }
    return new UriRequest(album.getAlbumID(), ImageUriRequest.URL_ALBUM,
        UriRequest.TYPE_NETEASE_ALBUM, album.getAlbum(), album.getArtist());
  }

  /**
   * 根据艺术家信息构建请求参数
   */
  public static UriRequest getSearchRequest(Artist artist) {
//        if(artist == null)
//            return SearchRequest.DEFAULT_REQUEST;
//        boolean isArtistAvailable = !TextUtils.isEmpty(artist.getArtist()) && !artist.getArtist().contains(mContext.getString(R.string.unknown_artist));
//        if(isArtistAvailable){
//            return new SearchRequest(artist.getArtistID(),artist.getArtist(), SearchRequest.TYPE_NETEASE_ARTIST,ImageUriRequest.URL_ARTIST);
//        }
//        return SearchRequest.DEFAULT_REQUEST;
    if (artist == null) {
      return UriRequest.DEFAULT_REQUEST;
    }
    return new UriRequest(artist.getArtistID(), ImageUriRequest.URL_ARTIST,
        UriRequest.TYPE_NETEASE_ARTIST, artist.getArtist());
  }

  public static UriRequest getSearchRequestWithAlbumType(Song song) {
    return new UriRequest(song.getAlbumId(),
        song.getId(),
        ImageUriRequest.URL_ALBUM,
        UriRequest.TYPE_NETEASE_SONG,
        song.getTitle(), song.getAlbum(), song.getArtist());
  }

  public static boolean isArtistNameUnknownOrEmpty(@Nullable String artistName) {
    if (TextUtils.isEmpty(artistName)) {
      return true;
    }
    artistName = artistName.trim().toLowerCase();
    return artistName.equals("unknown") || artistName.equals("<unknown>") || artistName
        .equals("未知艺术家");
  }

  public static boolean isAlbumNameUnknownOrEmpty(@Nullable String albumName) {
    if (TextUtils.isEmpty(albumName)) {
      return true;
    }
    albumName = albumName.trim().toLowerCase();
    return albumName.equals("unknown") || albumName.equals("<unknown>") || albumName.equals("未知专辑");
  }

  public static boolean isSongNameUnknownOrEmpty(@Nullable String songName) {
    if (TextUtils.isEmpty(songName)) {
      return true;
    }
    songName = songName.trim().toLowerCase();
    return songName.equals("unknown") || songName.equals("<unknown>") || songName.equals("未知歌曲");
  }

  public enum ImageSize {
    SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
  }

  public static String getLargestAlbumImageUrl(List<Image> images) {
    HashMap<ImageSize, String> imageUrls = new HashMap<>();
    for (Image image : images) {
      ImageSize size = null;
      final String attribute = image.getSize();
      if (attribute == null) {
        size = ImageSize.UNKNOWN;
      } else {
        try {
          size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException e) {
          // if they suddenly again introduce a new image size
        }
      }
      if (size != null) {
        imageUrls.put(size, image.getText());
      }
    }
    return getLargestImageUrl(imageUrls);
  }

  public static String getLargestArtistImageUrl(List<Image> images) {
    Map<ImageSize, String> imageUrls = new HashMap<>();
    for (Image image : images) {
      ImageSize size = null;
      final String attribute = image.getSize();
      if (attribute == null) {
        size = ImageSize.UNKNOWN;
      } else {
        try {
          size = ImageSize.valueOf(attribute.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException e) {
          // if they suddenly again introduce a new image size
        }
      }
      if (size != null) {
        imageUrls.put(size, image.getText());
      }
    }
    return getLargestImageUrl(imageUrls);
  }

  private static String getLargestImageUrl(Map<ImageSize, String> imageUrls) {
    if (imageUrls.containsKey(ImageSize.MEGA)) {
      return imageUrls.get(ImageSize.MEGA);
    }
    if (imageUrls.containsKey(ImageSize.EXTRALARGE)) {
      return imageUrls.get(ImageSize.EXTRALARGE);
    }
    if (imageUrls.containsKey(ImageSize.LARGE)) {
      return imageUrls.get(ImageSize.LARGE);
    }
    if (imageUrls.containsKey(ImageSize.MEDIUM)) {
      return imageUrls.get(ImageSize.MEDIUM);
    }
    if (imageUrls.containsKey(ImageSize.SMALL)) {
      return imageUrls.get(ImageSize.SMALL);
    }
    if (imageUrls.containsKey(ImageSize.UNKNOWN)) {
      return imageUrls.get(ImageSize.UNKNOWN);
    }
    return null;
  }


  public static String getArtistArt(long artistId) {
    try (Cursor cursor = App.getContext().getContentResolver().query(
        MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
        null,
        null, null, null)) {
      if (cursor != null && cursor.getCount() > 0) {

        List<Integer> albumIds = new ArrayList<>();
        while (cursor.moveToNext()) {
          albumIds.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)));
        }
        for (Integer albumId : albumIds) {
          Uri uri = ContentUris
              .withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId);
          if (ImageUriUtil.isAlbumThumbExistInMediaCache(uri)) {
            return uri.toString();
          }
        }
      }
    } catch (Exception e) {
      Timber.v(e);
    }
    return "";
  }
}
