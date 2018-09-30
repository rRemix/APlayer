package remix.myplayer.bean.mp3;

/**
 * Created by Remix on 2017/10/22.
 */

public class Album {
    private int AlbumID;
    private String Album;
    private int ArtistID;
    private String Artist;
    private int Count;

    public Album(int albumID, String album, int artistId, String artist, int count) {
        AlbumID = albumID;
        Album = album;
        ArtistID = artistId;
        Artist = artist;
        Count = count;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        this.Count = count;
    }

    public int getAlbumID() {
        return AlbumID;
    }

    public void setAlbumID(int albumID) {
        AlbumID = albumID;
    }

    public String getAlbum() {
        return Album;
    }

    public void setAlbum(String album) {
        Album = album;
    }

    public int getArtistID() {
        return ArtistID;
    }

    public void setArtistID(int artistID) {
        ArtistID = artistID;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }
}
