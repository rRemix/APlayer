package remix.myplayer.request;

import java.io.Serializable;
import remix.myplayer.util.ImageUriUtil;

public class UriRequest implements Serializable {

  private static final long serialVersionUID = 3225464659169043757L;
  public static final int TYPE_NETEASE_SONG = 1;
  public static final int TYPE_NETEASE_ALBUM = 10;
  public static final int TYPE_NETEASE_ARTIST = 100;

  public static final UriRequest DEFAULT_REQUEST = new UriRequest();

  private long mId;
  private int mSearchType;
  private int mNeteaseType;
  private String mTitle = "";
  private String mAlbumName = "";
  private String mArtistName = "";
  private int mSongId;

  public UriRequest() {
  }

  public UriRequest(long id, int searchType, int neteaseType) {
    this.mSearchType = searchType;
    this.mId = id;
    this.mNeteaseType = neteaseType;
  }

  public UriRequest(long id, int searchType, int neteaseType, String albumName, String artistName) {
    this.mId = id;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mAlbumName = albumName;
    this.mArtistName = artistName;
  }

  public UriRequest(long id, int searchType, int neteaseType, String artistName) {
    this.mId = id;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mArtistName = artistName;
  }

  public UriRequest(long id, int songId,int searchType, int neteaseType, String title, String albumName,
      String artistName) {
    this.mId = id;
    this.mSongId = songId;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mTitle = title;
    this.mAlbumName = albumName;
    this.mArtistName = artistName;
  }

  public String getNeteaseCacheKey() {
    return "netease " + hashCode();
  }

  public String getNeteaseSearchKey() {
    boolean isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(mTitle);
    boolean isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(mAlbumName);
    boolean isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(mArtistName);
    if (mSearchType == ImageUriRequest.URL_ALBUM) {
      //歌曲名合法
      if (isTitleAvailable) {
        //艺术家合法
        if (isArtistAvailable) {
          return mTitle + "-" + mArtistName;
        }
        //专辑名合法
        if (isAlbumAvailable) {
          return mTitle + "-" + mAlbumName;
        }
      }
      //根据专辑名字查询
      if (isAlbumAvailable && isArtistAvailable) {
        return mArtistName + "-" + mAlbumName;
      }
//            if(isAlbumAvailable)
//                return mAlbumName;
    } else if (mSearchType == ImageUriRequest.URL_ARTIST) {
      if (isArtistAvailable) {
        return mArtistName;
      }
    }
    return "";
  }

  public String getLastFMKey() {
    return "lastfm " + hashCode();
  }

  public long getID() {
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

  public int getSongId() {
    return mSongId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UriRequest)) {
      return false;
    }

    UriRequest that = (UriRequest) o;

    if (mId != that.mId) {
      return false;
    }
    if (mSearchType != that.mSearchType) {
      return false;
    }
    if (mNeteaseType != that.mNeteaseType) {
      return false;
    }
    if (mTitle != null ? !mTitle.equals(that.mTitle) : that.mTitle != null) {
      return false;
    }
    if (mAlbumName != null ? !mAlbumName.equals(that.mAlbumName) : that.mAlbumName != null) {
      return false;
    }
    return mArtistName != null ? mArtistName.equals(that.mArtistName) : that.mArtistName == null;
  }

  @Override
  public int hashCode() {
    int result = (int) (mId ^ (mId >>> 32));
    result = 31 * result + mSearchType;
    result = 31 * result + mNeteaseType;
    result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
    result = 31 * result + (mAlbumName != null ? mAlbumName.hashCode() : 0);
    result = 31 * result + (mArtistName != null ? mArtistName.hashCode() : 0);
    return result;
  }
}
