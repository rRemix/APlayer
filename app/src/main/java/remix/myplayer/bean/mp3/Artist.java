package remix.myplayer.bean.mp3;

/**
 * Created by Remix on 2017/10/22.
 */

public class Artist {

  private int ArtistID;
  private String Artist;
  private int Count;

  public Artist(int artistID, String artist, int count) {
    ArtistID = artistID;
    Artist = artist;
    Count = count;
  }

  public int getCount() {
    return Count;
  }

  public void setCount(int count) {
    this.Count = count;
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
