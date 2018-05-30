package remix.myplayer.request;

import java.io.Serializable;

import remix.myplayer.util.ImageUriUtil;

public class NewUriRequest implements Serializable {
    private static final long serialVersionUID = 3225464659169043757L;
    public static final int TYPE_NETEASE_SONG = 1;
    public static final int TYPE_NETEASE_ALBUM = 10;
    public static final int TYPE_NETEASE_ARTIST = 100;

    public static final NewUriRequest DEFAULT_REQUEST = new NewUriRequest();

    private int mId;
    private int mSearchType;
    private String mTitle = "";
    private String mAlbumName = "";
    private String mArtistName = "";

    public NewUriRequest(){}

    public NewUriRequest(int id,int searchType) {
        this.mSearchType = searchType;
        this.mId = id;
    }

    public NewUriRequest(int mId, int mSearchType, String mAlbumName, String mArtistName) {
        this.mId = mId;
        this.mSearchType = mSearchType;
        this.mAlbumName = mAlbumName;
        this.mArtistName = mArtistName;
    }

    public NewUriRequest(int mId, int mSearchType, String mArtistName) {
        this.mId = mId;
        this.mSearchType = mSearchType;
        this.mArtistName = mArtistName;
    }

    public NewUriRequest(int id, int searchType, String title, String albumName, String artistName) {
        this.mId = id;
        this.mSearchType = searchType;
        this.mTitle = title;
        this.mAlbumName = albumName;
        this.mArtistName = artistName;
    }

    public String getNeteaseKey(){
        if(mSearchType == ImageUriRequest.URL_ALBUM){
            if(!ImageUriUtil.isAlbumNameUnknown(mAlbumName))
                return mAlbumName;
        }
        if(mSearchType == ImageUriRequest.URL_ARTIST){
            if(!ImageUriUtil.isArtistNameUnknown(mArtistName))
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

}
