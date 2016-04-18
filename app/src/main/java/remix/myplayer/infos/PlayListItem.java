package remix.myplayer.infos;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 播放列表Item
 */
public class PlayListItem {
    private String mSongName;
    private int mId;
    private int mAlbumId;
    private String mArtist;
    private PlayListItem(){}
    public PlayListItem(String SongName, int Id,int AlbumId,String Artist) {
        this.mSongName = SongName;
        this.mId = Id;
        this.mAlbumId = AlbumId;
        this.mArtist = Artist;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String SongName) {
        this.mArtist = SongName;
    }

    public String getSongame() {
        return mSongName;
    }

    public void setSongName(String SongName) {
        this.mSongName = SongName;
    }

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }

    public int getAlbumId() {
        return mAlbumId;
    }

    public void setAlbumId(int mId) {
        this.mAlbumId = mId;
    }

    @Override
    public String toString() {
        return "PlayListItem{" +
                "SongName='" + mSongName + '\'' +
                ", Id=" + mId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        PlayListItem item = (PlayListItem)o;
        return item.getId() == mId;
    }
}
