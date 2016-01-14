package remix.myplayer.utils;

/**
 * Created by Remix on 2015/12/4.
 */
public class ArtistInfo {
    private String Artist;
    private int Artist_Id;
    private int AlbumNum;
    private int SongNum;
    public ArtistInfo(String artist,int artist_id,int songNum) {
        Artist = artist;
        Artist_Id = artist_id;
        SongNum = songNum;
    }

    @Override
    public String toString() {
        return "Artist = " + Artist + " Artist_Id = " + Artist_Id + " SongNum = " + SongNum + "\n";
    }

    public int getArtist_Id() {
        return Artist_Id;
    }

    public void setArtist_Id(int artist_Id) {
        Artist_Id = artist_Id;
    }

    public int getSongNum() {
        return SongNum;
    }

    public void setSongNum(int songNum) {
        SongNum = songNum;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }

    public int getAlbumNum() {
        return AlbumNum;
    }

    public void setAlbumNum(int albumNum) {
        AlbumNum = albumNum;
    }

}
