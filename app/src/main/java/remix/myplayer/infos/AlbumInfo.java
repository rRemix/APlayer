package remix.myplayer.infos;

/**
 * Created by Remix on 2015/12/4.
 */
public class AlbumInfo {
    private int AlbumId;
    private String Album;
    private String Artist;
    private String AlbumArt;
    private int AlbumNum;
    public AlbumInfo(int albumId, String album, String artist, String albumArt, int albumnum) {
        AlbumId = albumId;
        Album = album;
        Artist = artist;
        AlbumArt = albumArt;
        AlbumNum = albumnum;
    }

    @Override
    public String toString() {
        return "AlbumId = " + AlbumId + "Album = " + Album + "Artist = " + Artist + "AlbumArt = " + AlbumArt + "AlbumNum = " + AlbumNum;
    }

    public int getAlbumNum() {
        return AlbumNum;
    }

    public void setAlbumNum(int albumNum) {
        AlbumNum = albumNum;
    }

    public int getAlbumId() {
        return AlbumId;
    }

    public void setAlbumId(int albumId) {
        AlbumId = albumId;
    }

    public String getAlbum() {
        return Album;
    }

    public void setAlbum(String album) {
        Album = album;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }

    public String getAlbumArt() {
        return AlbumArt;
    }

    public void setAlbumArt(String albumArt) {
        AlbumArt = albumArt;
    }
}
