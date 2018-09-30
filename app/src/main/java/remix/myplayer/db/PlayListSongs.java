package remix.myplayer.db;

import android.net.Uri;

/**
 * Created by Remix on 2016/10/12.
 */
public class PlayListSongs {
    public static final String TABLE_NAME = "play_list_song";
    //    public static final Uri SINGLE = Uri.importM3UFile(DBContentProvider.CONTENT_AUTHORITY_SLASH + TABLE_NAME + "#");
    public static final Uri CONTENT_URI = Uri.parse(DBContentProvider.CONTENT_AUTHORITY_SLASH + TABLE_NAME);

    public static class PlayListSongColumns {
        public static final String _ID = "_id";
        public static final String AUDIO_ID = "audio_id";
        public static final String PLAY_LIST_ID = "play_list_id";
        public static final String PLAY_LIST_NAME = "play_list_name";
//        public static final String Album_ID = "album_id";
//        public static final String Album = "album";
//        public static final String Artist = "artist";
//        public static final String Artist_ID = "artist_id";
//        public static final String DATA = "data";
    }
}
