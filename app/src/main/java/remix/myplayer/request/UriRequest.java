package remix.myplayer.request;

import java.io.Serializable;

import remix.myplayer.util.ImageUriUtil;

public class UriRequest implements Serializable {
    private static final long serialVersionUID = 3225464659169043757L;
    public static final int TYPE_NETEASE_SONG = 1;
    public static final int TYPE_NETEASE_ALBUM = 10;
    public static final int TYPE_NETEASE_ARTIST = 100;
    public static final int TYPE_NONE = 0;

    public static final UriRequest DEFAULT_REQUEST = new UriRequest();

    private int mId;
    private int mSearchType;
    private int mNeteaseType;
    private String mTitle = "";
    private String mAlbumName = "";
    private String mArtistName = "";

    public UriRequest(){}

    public UriRequest(int id, int searchType,int neteaseType) {
        this.mSearchType = searchType;
        this.mId = id;
        this.mNeteaseType = neteaseType;
    }

    public UriRequest(int id, int searchType,int neteaseType ,String albumName, String artistName) {
        this.mId = id;
        this.mSearchType = searchType;
        this.mNeteaseType = neteaseType;
        this.mAlbumName = albumName;
        this.mArtistName = artistName;
    }

    public UriRequest(int id, int searchType,int neteaseType, String artistName) {
        this.mId = id;
        this.mSearchType = searchType;
        this.mNeteaseType = neteaseType;
        this.mArtistName = artistName;
    }

    public UriRequest(int id, int searchType,int neteaseType, String title, String albumName, String artistName) {
        this.mId = id;
        this.mSearchType = searchType;
        this.mNeteaseType = neteaseType;
        this.mTitle = title;
        this.mAlbumName = albumName;
        this.mArtistName = artistName;
    }

    public String getNeteaseCacheKey(){
        return mSearchType + "-" + mId;
    }

    public String getNeteaseSearchKey(){
        boolean isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(mTitle);
        boolean isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(mAlbumName);
        boolean isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(mArtistName);
        if(mSearchType == ImageUriRequest.URL_ALBUM){
            //歌曲名合法
            if(isTitleAvailable){
                //艺术家合法
                if(isArtistAvailable){
                    return mArtistName + "-" + mTitle;
                }
                //专辑名合法
                if(isAlbumAvailable){
                    return mAlbumName + "-" + mTitle;
                }
            }
            //根据专辑名字查询
//            if(isAlbumAvailable && isArtistAvailable){
//                return mArtistName + "-" + mAlbumName;
//            }
            if(isAlbumAvailable)
                return mAlbumName;
        }else if(mSearchType == ImageUriRequest.URL_ARTIST){
            if(isArtistAvailable)
                return mArtistName;
        }
        return "";
    }

    public String getLastFMKey() {
        return mSearchType * mSearchType + mId * mId + mSearchType == ImageUriRequest.URL_ALBUM ? mAlbumName : mArtistName;
//        return (mSearchType >> 2  + mId >> 4) + "";
    }

    public int getID() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getSearchType() {
        return mSearchType;
    }

    public void setSearchType(int searchType) {
        mSearchType = searchType;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public void setAlbumName(String albumName) {
        mAlbumName = albumName;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public void setArtistName(String artistName) {
        mArtistName = artistName;
    }

    public int getNeteaseType() {
        return mNeteaseType;
    }
}
