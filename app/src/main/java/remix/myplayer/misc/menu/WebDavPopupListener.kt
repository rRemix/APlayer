package remix.myplayer.misc.menu

import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.db.room.AppDatabase
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.ui.activity.WebDavActivity
import remix.myplayer.ui.activity.WebDavDetailActivity
import timber.log.Timber
import java.lang.ref.WeakReference

class WebDavPopupListener(activity: WebDavActivity, private val webDav: WebDav) :
  PopupMenu.OnMenuItemClickListener {
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val context = ref.get() ?: return true
    when (item.itemId) {
      R.id.menu_connect -> {
        WebDavDetailActivity.start(context, webDav)
      }

      R.id.menu_edit -> {
        WebDavActivity.showWebDavDialog(context, webDav)
      }

      R.id.menu_delete -> {
        context.launch {
          val count = AppDatabase.getInstance(context.applicationContext).webDavDao().delete(webDav)
          Timber.v("delete count: $count")
        }
      }
    }

    return true
  }
}