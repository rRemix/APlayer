package remix.myplayer.misc.menu

import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.misc.AudioTag
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.RxUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import java.lang.ref.WeakReference

class WebDavItemPopupListener(
  activity: BaseActivity,
  private val webDav: WebDav,
  private val davResource: DavResource,
  private val callback: Callback? = null
) : PopupMenu.OnMenuItemClickListener {
  private val ref = WeakReference(activity)
  private val song: Song.Remote by lazy {
    Song.Remote(
      title = davResource.name.substringBeforeLast('.'),
      data = webDav.base().plus(davResource.path),
      size = davResource.contentLength,
      dateModified = davResource.creation?.time ?: 0,
      account = webDav.account ?: "",
      pwd = webDav.pwd ?: ""
    )
  }

  private val audioTag by lazy {
    AudioTag(activity, song)
  }

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true
    val loading = Theme.getLoadingDialog(activity, activity.getString(R.string.loading)).build()
    when (item.itemId) {
      R.id.menu_next -> {
        Util.sendLocalBroadcast(
          MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
            .putExtra(MusicService.EXTRA_SONG, song)
        )
      }

      R.id.menu_add_to_play_queue -> {
        DatabaseRepository.getInstance()
          .insertSongsToPlayQueue(listOf(song))
          .compose(RxUtil.applySingleScheduler())
          .subscribe { it ->
            ToastUtil.show(
              activity,
              activity.getString(R.string.add_song_playqueue_success, it)
            )
          }
      }

      R.id.menu_detail -> {
        if (loading.isShowing) {
          loading.dismiss()
        }
        loading.show()
        activity.launch {
          withContext(Dispatchers.IO) {
            MusicService.retrieveRemoteSong(song, song)
          }
          loading.dismiss()
          audioTag.detail()
        }
      }

      R.id.menu_delete -> {
        if (loading.isShowing) {
          loading.dismiss()
        }
        loading.show()
        val sardine = OkHttpSardine()
        sardine.setCredentials(webDav.account, webDav.pwd)
        activity.launch {
          withContext(Dispatchers.IO) {
            sardine.delete(webDav.base().plus(davResource.path))
          }
          loading.dismiss()
          callback?.onDavResourceRemove(davResource)
        }
      }
    }

    return true
  }

  interface Callback {
    fun onDavResourceRemove(davResource: DavResource)
  }
}