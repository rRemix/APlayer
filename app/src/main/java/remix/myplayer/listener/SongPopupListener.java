package remix.myplayer.listener;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;

/**
 * Created by Remix on 2018/3/5.
 */

public class SongPopupListener implements PopupMenu.OnMenuItemClickListener {
    public SongPopupListener(Context mContext, int albumId, int album, String album1) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return false;
    }
}
