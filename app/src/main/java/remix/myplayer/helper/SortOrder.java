package remix.myplayer.helper;

import static remix.myplayer.helper.SortOrder.SongSortOrder.SONG_ALBUM_A_Z;
import static remix.myplayer.helper.SortOrder.SongSortOrder.SONG_ARTIST_A_Z;
import static remix.myplayer.helper.SortOrder.SongSortOrder.SONG_ARTIST_Z_A;

import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;

/**
 * Created by Remix on 2018/3/22.
 */

public final class SortOrder {

  private SortOrder() {
  }

  public interface SongSortOrder {

    String SONG_A_Z = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
    String SONG_Z_A = SONG_A_Z + " desc ";
    String SONG_ARTIST_A_Z = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
    String SONG_ARTIST_Z_A = SONG_ARTIST_A_Z + " desc ";
    String SONG_ALBUM_A_Z = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
    String SONG_ALBUM_Z_A = SONG_ALBUM_A_Z + " desc ";
    String SONG_DATE = MediaStore.Audio.Media.DATE_ADDED;
    String SONG_DATE_DESC = MediaStore.Audio.Media.DATE_ADDED + " desc ";
    String SONG_DISPLAY_TITLE_A_Z = Media.DISPLAY_NAME;
    String SONG_DISPLAY_TITLE_Z_A = Media.DISPLAY_NAME + " desc ";
    String SONG_DURATION = MediaStore.Audio.Media.DURATION;
    String SONG_YEAR = MediaStore.Audio.Media.YEAR;
  }

  public interface AlbumSortOrder {

    String ALBUM_A_Z = SONG_ALBUM_A_Z;
    String ALBUM_Z_A = SONG_ALBUM_A_Z + " desc";
    String ALBUM_ARTIST_A_Z = SONG_ARTIST_A_Z;
    String ALBUM_ARTIST_Z_A = SONG_ARTIST_Z_A;
  }

  public interface ArtistSortOrder {

    String ARTIST_A_Z = SONG_ARTIST_A_Z;
    String ARTIST_Z_A = SONG_ARTIST_Z_A;
  }

  public interface PlayListSortOrder {

    String PLAYLIST_A_Z = "name";
    String PLAYLIST_Z_A = PLAYLIST_A_Z + " desc ";
    String PLAYLIST_DATE = "date";
  }

  public interface ChildHolderSongSortOrder extends SongSortOrder {

    String SONG_TRACK_NUMBER = MediaStore.Audio.Media.TRACK;
  }

  public interface PlayListSongSortOrder extends ChildHolderSongSortOrder {

    String PLAYLIST_SONG_CUSTOM = "custom";
  }
}
