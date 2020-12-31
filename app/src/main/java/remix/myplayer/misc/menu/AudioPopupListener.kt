package remix.myplayer.misc.menu

import android.content.ContextWrapper
import androidx.appcompat.widget.PopupMenu
import android.view.MenuItem
import android.widget.CompoundButton
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.service.Command
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.ui.dialog.FileChooserDialog
import remix.myplayer.ui.dialog.TimerDialog
import remix.myplayer.ui.misc.Tag
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.sendLocalBroadcast
import java.lang.ref.WeakReference

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
class AudioPopupListener<ActivityCallback>(activity: ActivityCallback, private val song: Song) : ContextWrapper(activity), PopupMenu.OnMenuItemClickListener
    where ActivityCallback : PlayerActivity, ActivityCallback : FileChooserDialog.FileCallback {
  private val tag: Tag = Tag(activity, song)
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true
    when (item.itemId) {
      R.id.menu_lyric -> {
        val alreadyIgnore = (SPUtil
            .getValue(ref.get(), SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE)

        val lyricFragment = ref.get()?.lyricFragment ?: return true
        getBaseDialog(ref.get())
            .items(getString(R.string.kugou),
                getString(R.string.netease),
                getString(R.string.qq),
                getString(R.string.local),
                getString(R.string.embedded_lyric),
                getString(R.string.select_lrc),
                getString(if (!alreadyIgnore) R.string.ignore_lrc else R.string.cancel_ignore_lrc),
                getString(R.string.change_offset))
            .itemsCallback { dialog, itemView, position, text ->
              when (position) {
                0, 1, 2, 3, 4 -> { //0酷狗 1网易 2QQ 3本地 4内嵌
                  SPUtil.putValue(ref.get(), SPUtil.LYRIC_KEY.NAME, song.id.toString(), position + 2)
                  lyricFragment.updateLrc(song, true)
                  sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
                }
                5 -> { //手动选择歌词
                  FileChooserDialog.Builder(activity)
                      .extensionsFilter(".lrc")
                      .show()
                }
                6 -> { //忽略或者取消忽略
                  getBaseDialog(activity)
                      .title(if (!alreadyIgnore) R.string.confirm_ignore_lrc else R.string.confirm_cancel_ignore_lrc)
                      .negativeText(R.string.cancel)
                      .positiveText(R.string.confirm)
                      .onPositive { dialog1, which ->
                        if (!alreadyIgnore) {//忽略
                          SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                              SPUtil.LYRIC_KEY.LYRIC_IGNORE)
                          lyricFragment.updateLrc(song)
                        } else {//取消忽略
                          SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, song.id.toString(),
                              SPUtil.LYRIC_KEY.LYRIC_DEFAULT)
                          lyricFragment.updateLrc(song)
                        }
                        sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC))
                      }
                      .show()
                }
                7 -> { //歌词时间轴调整
                  activity.showLyricOffsetView()
                }
              }

            }
            .show()
      }
      R.id.menu_edit -> {
        tag.edit()
      }
      R.id.menu_detail -> {
        tag.detail()
      }
      R.id.menu_timer -> {
        val fm = activity.supportFragmentManager ?: return true
        TimerDialog.newInstance().show(fm, TimerDialog::class.java.simpleName)
      }
      R.id.menu_eq -> {
        EQHelper.startEqualizer(activity)
      }
      R.id.menu_collect -> {
        DatabaseRepository.getInstance()
            .insertToPlayList(listOf(song.id), getString(R.string.my_favorite))
            .compose<Int>(applySingleScheduler<Int>())
            .subscribe(
                { count -> ToastUtil.show(activity, getString(R.string.add_song_playlist_success, 1, getString(R.string.my_favorite))) },
                { throwable -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
      }
      R.id.menu_add_to_playlist -> {
        AddtoPlayListDialog.newInstance(listOf(song.id))
            .show(activity.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
      }
      R.id.menu_delete -> {
        val checked = arrayOf(SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))

        getBaseDialog(activity)
            .content(R.string.confirm_delete_from_library)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, checked[0], CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> checked[0] = isChecked })
            .onAny { dialog, which ->
              if (which == POSITIVE) {
                DeleteHelper.deleteSong(song.id, checked[0], false, "")
                    .compose<Boolean>(applySingleScheduler<Boolean>())
                    .subscribe({ success ->
                      if (success) {
                        //移除的是正在播放的歌曲
                        if (song.id == getCurrentSong().id) {
                          Util.sendCMDLocalBroadcast(Command.NEXT)
                        }
                      }
                      ToastUtil.show(activity, if (success) R.string.delete_success else R.string.delete_error)
                    }, { ToastUtil.show(activity, R.string.delete) })
              }
            }
            .show()
      }
      //            case R.id.menu_vol:
      //                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
      //                if(audioManager != null){
      //                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
      //                }
      R.id.menu_speed -> {
        getBaseDialog(activity)
            .title(R.string.speed)
            .input(SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"),
                "",
                MaterialDialog.InputCallback { dialog, input ->
                  var speed = 0f
                  try {
                    speed = java.lang.Float.parseFloat(input.toString())
                  } catch (ignored: Exception) {

                  }

                  if (speed > 2f || speed < 0.5f) {
                    ToastUtil.show(activity, R.string.speed_range_tip)
                    return@InputCallback
                  }
                  SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED,
                      input.toString())
                })
            .show()
      }
    }
    return true
  }

}
