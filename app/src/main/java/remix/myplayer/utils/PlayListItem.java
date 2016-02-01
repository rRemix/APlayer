package remix.myplayer.utils;

/**
 * Created by taeja on 16-2-1.
 */
public class PlayListItem {
    private String mSongName;
    private int mId;
    public PlayListItem(){}
    public PlayListItem(String SongName, int Id) {
        this.mSongName = SongName;
        this.mId = Id;
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

    @Override
    public String toString() {
        return "PlayListItem{" +
                "SongName='" + mSongName + '\'' +
                ", Id=" + mId +
                '}';
    }
}
