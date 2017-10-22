package remix.myplayer.model.mp3;

/**
 * Created by Remix on 2017/10/22.
 */

public class Artist {
    private int ArtistID;
    private String Artist;

    public Artist(int artistID, String artist) {
        ArtistID = artistID;
        Artist = artist;
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
