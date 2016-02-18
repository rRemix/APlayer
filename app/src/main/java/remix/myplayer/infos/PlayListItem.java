package remix.myplayer.infos;

/**
 * Created by taeja on 16-2-1.
 */
public class PlayListItem {
    private String mSongName;
    private int mId;
    private int mAlbumId;
    public PlayListItem(){}
    public PlayListItem(String SongName, int Id,int AlbumId) {
        this.mSongName = SongName;
        this.mId = Id;
        this.mAlbumId = AlbumId;
    }

    public String getmSongame() {
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
}
