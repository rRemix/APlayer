package remix.myplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Album;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NSearchRequest;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.ImageUriRequest;

/**
 * Created by Remix on 2017/11/30.
 */

public class ImageUriUtil {
    private ImageUriUtil(){}
    private static final String TAG = "ImageUriUtil";
    private static Context mContext;
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 获得某歌手在本地数据库的封面
     */
    public static File getArtistThumbInMediaCache(int artistId){
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Media.ARTIST_ID + "=?",new String[]{artistId + ""},null);
            if(cursor != null && cursor.moveToFirst()) {
                String imagePath = cursor.getString(0);
                if(!TextUtils.isEmpty(imagePath))
                    return new File(imagePath);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if(cursor != null )
                cursor.close();
        }
        return null;
    }

    /**
     * 判断某专辑在本地数据库是否有封面
     * @param uri
     * @return
     */
    public static boolean isAlbumThumbExistInMediaCache(Uri uri){
        boolean exist = false;
        InputStream stream = null;
        try {
            stream = mContext.getContentResolver().openInputStream(uri);
            exist = true;
        } catch (Exception e) {
            exist = false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return exist;
    }

    /**
     * 返回自定义的封面
     * @param arg
     * @param type
     * @return
     */
    public static File getCustomThumbIfExist(int arg, int type){
        File img = type == ImageUriRequest.URL_ALBUM ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/album") + "/" + Util.hashKeyForDisk(arg * 255 + ""))
                : type == ImageUriRequest.URL_ARTIST ? new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/artist") + "/" + Util.hashKeyForDisk(arg* 255 + ""))
                : new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + Util.hashKeyForDisk(arg * 255 + ""));
        if(img.exists()){
            return img;
        }
        return null;
    }

    /**
     * 根据歌曲信息构建请求参数
     * @param song
     * @return
     */
    public static NSearchRequest getSearchRequest(Song song,int localType){
        if(song == null)
            return NSearchRequest.DEFAULT_REQUEST;
        boolean isTitleAvailable = !TextUtils.isEmpty(song.getTitle()) && !song.getTitle().contains(mContext.getString(R.string.unknown_song));
        boolean isAlbumAvailable = !TextUtils.isEmpty(song.getAlbum()) && !song.getAlbum().contains(mContext.getString(R.string.unknown_album));
        boolean isArtistAvailable = !TextUtils.isEmpty(song.getArtist()) && !song.getArtist().contains(mContext.getString(R.string.unknown_artist));

        //歌曲名合法
        if(isTitleAvailable){
            //艺术家合法
            if(isArtistAvailable){
                return new NSearchRequest(song.getAlbumId(),song.getTitle() + "-" + song.getArtist(),NSearchRequest.TYPE_NETEASE_SONG,localType);
            }
            //专辑名合法
            if(isAlbumAvailable){
                return new NSearchRequest(song.getAlbumId(),song.getTitle() + "-" + song.getAlbum(),NSearchRequest.TYPE_NETEASE_SONG,localType);
            }
        }
        //根据专辑名字查询
        if(isAlbumAvailable && isArtistAvailable){
            return new NSearchRequest(song.getAlbumId(),song.getArtist() + "-" + song.getAlbum(),NSearchRequest.TYPE_NETEASE_SONG,localType);
        }
        return NSearchRequest.DEFAULT_REQUEST;
    }

    /**
     * 根据专辑信息构建请求参数
     * @param album
     * @return
     */
    public static NSearchRequest getSearchRequest(Album album){
        if(album == null)
            return NSearchRequest.DEFAULT_REQUEST;
        boolean isAlbumAvailable = !TextUtils.isEmpty(album.getAlbum()) && !album.getAlbum().contains(mContext.getString(R.string.unknown_album));
        boolean isArtistAvailable = !TextUtils.isEmpty(album.getArtist()) && !album.getArtist().contains(mContext.getString(R.string.unknown_artist));
        if(isAlbumAvailable && isArtistAvailable){
            return new NSearchRequest(album.getAlbumID(),album.getArtist() + "-" + album.getAlbum(),NSearchRequest.TYPE_NETEASE_SONG,ImageUriRequest.URL_ALBUM);
        }
        return NSearchRequest.DEFAULT_REQUEST;
    }

    /**
     * 根据艺术家信息构建请求参数
     * @param artist
     * @return
     */
    public static NSearchRequest getSearchRequest(Artist artist){
        if(artist == null)
            return NSearchRequest.DEFAULT_REQUEST;
        boolean isArtistAvailable = !TextUtils.isEmpty(artist.getArtist()) && !artist.getArtist().contains(mContext.getString(R.string.unknown_artist));
        if(isArtistAvailable){
            return new NSearchRequest(artist.getArtistID(),artist.getArtist(),NSearchRequest.TYPE_NETEASE_ARTIST,ImageUriRequest.URL_ARTIST);
        }
        return NSearchRequest.DEFAULT_REQUEST;
    }

    public static NSearchRequest getSearchRequestWithAlbumType(Song song){
        return getSearchRequest(song, ImageUriRequest.URL_ALBUM);
    }

}
