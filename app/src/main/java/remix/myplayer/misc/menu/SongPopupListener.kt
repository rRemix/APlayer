package remix.myplayer.misc.menu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import android.view.MenuItem
import android.widget.CompoundButton
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.theme.Theme
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.ui.misc.Tag
import remix.myplayer.util.*
import remix.myplayer.util.SPUtil.SETTING_KEY
import java.lang.ref.WeakReference

/**
 * Created by Remix on 2018/3/5.
 */

class SongPopupListener(activity: AppCompatActivity,
                        private val song: Song,
                        private val isDeletePlayList: Boolean,
                        private val playListName: String) : PopupMenu.OnMenuItemClickListener {
  private val tag = Tag(activity, song)
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true

    when (item.itemId) {
      R.id.menu_next -> {
        Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
            .putExtra(EXTRA_SONG, song))
      }
      R.id.menu_add_to_playlist -> {
        AddtoPlayListDialog.newInstance(listOf(song.id))
            .show(activity.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
      }
      R.id.menu_add_to_play_queue -> {
        DatabaseRepository.getInstance()
            .insertToPlayQueue(listOf(song.id))
            .compose(applySingleScheduler())
            .subscribe { it -> ToastUtil.show(activity, activity.getString(R.string.add_song_playqueue_success, it)) }
      }
      R.id.menu_detail -> {
        tag.detail()
      }
      R.id.menu_edit -> {
        tag.edit()
      }
      R.id.menu_album_thumb -> {
        val customCover = CustomCover(song.albumId, Constants.ALBUM,
            song.album)
        val coverIntent = activity.intent
        coverIntent.putExtra("thumb", customCover)
        activity.intent = coverIntent
        Crop.pickImage(activity, Crop.REQUEST_PICK)
      }
      R.id.menu_collect -> {
        DatabaseRepository.getInstance()
            .insertToPlayList(listOf(song.id), activity.getString(R.string.my_favorite))
            .compose<Int>(applySingleScheduler<Int>())
            .subscribe(
                { count -> ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, 1, activity.getString(R.string.my_favorite))) },
                { throwable -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
      }
      R.id.menu_ring -> {
        MediaStoreUtil.setRing(activity, song.id)
      }
      R.id.menu_share -> {
        activity.startActivity(
            Intent.createChooser(Util.createShareSongFileIntent(song, activity), null))
      }
      R.id.menu_delete -> {
        val title = activity.getString(R.string.confirm_delete_from_playlist_or_library,
            if (isDeletePlayList) playListName else "曲库")
        val check = arrayOf(SPUtil.getValue(App.getContext(), SETTING_KEY.NAME, SETTING_KEY.DELETE_SOURCE, false));
        Theme.getBaseDialog(activity)
            .content(title)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, check[0]) { buttonView, isChecked -> check[0] = isChecked }
            .onAny { dialog, which ->
              if (which == POSITIVE) {
                DeleteHelper
                    .deleteSong(song.id, check[0], isDeletePlayList, playListName)
                    .subscribe({ success -> ToastUtil.show(activity, if (success) R.string.delete_success else R.string.delete_error) }, { ToastUtil.show(activity, R.string.delete_error) })
              }
            }
            .show()
      }
    }
    return true
  }
}
