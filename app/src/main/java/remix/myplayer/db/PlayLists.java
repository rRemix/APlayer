package remix.myplayer.db;

import android.net.Uri;

/**
 * Created by Remix on 2016/10/12.
 */
public class PlayLists {

    public static final String TABLE_NAME = "play_list";
    //    public static final Uri SINGLE = Uri.importM3UFile(DBContentProvider.CONTENT_AUTHORITY_SLASH + TABLE_NAME + "#");
    public static final Uri CONTENT_URI = Uri.parse(DBContentProvider.CONTENT_AUTHORITY_SLASH + TABLE_NAME);

    public static class PlayListColumns {
        public static final String _ID = "_id";
        public static final String NAME = "name";
        public static final String COUNT = "count";
        public static final String DATE = "date";
    }
}
