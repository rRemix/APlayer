package remix.myplayer.model;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 播放列表Item
 */
public class PlayListItem {
    public String SongName;
    public int SongId;
    public int AlbumId;
    public String Artist;
    public PlayListItem(){}
    public PlayListItem(String SongName, int Id,int AlbumId,String Artist) {
        this.SongName = SongName;
        this.SongId = Id;
        this.AlbumId = AlbumId;
        this.Artist = Artist;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String SongName) {
        this.Artist = SongName;
    }

    public String getSongame() {
        return SongName;
    }

    public void setSongName(String SongName) {
        this.SongName = SongName;
    }

    public int getId() {
        return SongId;
    }

    public void setId(int mId) {
        this.SongId = mId;
    }

    public int getAlbumId() {
        return AlbumId;
    }

    public void setAlbumId(int mId) {
        this.AlbumId = mId;
    }

    @Override
    public String toString() {
        return "PlayListItem{" +
                "SongName='" + SongName + '\'' +
                ", Id=" + SongId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        PlayListItem item = (PlayListItem)o;
        return item.getId() == SongId;
    }
}
