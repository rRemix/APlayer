package remix.myplayer.misc.menu

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import androidx.appcompat.widget.PopupMenu
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.LyricsHelper
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.lyrics.provider.EmbeddedProvider
import remix.myplayer.lyrics.provider.IgnoredProvider
import remix.myplayer.lyrics.provider.StubProvider
import remix.myplayer.service.Command
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.ui.dialog.TimerDialog
import remix.myplayer.ui.misc.AudioTag
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import java.lang.ref.WeakReference

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
class AudioPopupListener(activity: PlayerActivity, private val song: Song) :
    ContextWrapper(activity), PopupMenu.OnMenuItemClickListener {
  private val audioTag: AudioTag = AudioTag(activity, song)
  private val ref = WeakReference(activity)

  override fun onMenuItemClick(item: MenuItem): Boolean {
    val activity = ref.get() ?: return true
    when (item.itemId) {
      R.id.menu_lyric -> {
        LyricsHelper.showLocalLyricsTip(activity) {
          onClickLyric(activity)
        }
        return true
      }
      R.id.menu_edit -> {
        if (!song.isLocal()) {
          return true
        }
        audioTag.edit()
      }
      R.id.menu_detail -> {
        audioTag.detail()
      }
      R.id.menu_timer -> {
        val fm = activity.supportFragmentManager
        TimerDialog.newInstance().show(fm, TimerDialog::class.java.simpleName)
      }
      R.id.menu_eq -> {
        EQHelper.startEqualizer(activity)
      }
      R.id.menu_collect -> {
        if (!song.isLocal()) {
          return true
        }
        DatabaseRepository.getInstance()
            .insertToPlayList(listOf(song.id), getString(R.string.my_favorite))
            .compose(applySingleScheduler())
            .subscribe(
                { count -> ToastUtil.show(activity, getString(R.string.add_song_playlist_success, 1, getString(R.string.my_favorite))) },
                { throwable -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
      }
      R.id.menu_add_to_playlist -> {
        if (!song.isLocal()) {
          return true
        }
        AddtoPlayListDialog.newInstance(listOf(song.id))
            .show(activity.supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
      }
      R.id.menu_delete -> {
        if (!song.isLocal()) {
          return true
        }
        val checked = arrayOf(SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))

        getBaseDialog(activity)
            .content(R.string.confirm_delete_from_library)
            .positiveText(R.string.confirm)
            .negativeText(R.string.cancel)
            .checkBoxPromptRes(R.string.delete_source, checked[0], CompoundButton.OnCheckedChangeListener { buttonView, isChecked -> checked[0] = isChecked })
            .onAny { dialog, which ->
              if (which == POSITIVE) {
                DeleteHelper.deleteSong(activity, song.id, checked[0], false, "")
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

  private fun onClickLyric(activity: PlayerActivity) {
    getBaseDialog(activity).items(
        getString(R.string.default_lyrics),
        getString(R.string.embedded_lyric),
        getString(R.string.local),
        getString(R.string.kugou),
        getString(R.string.netease),
        getString(R.string.qq),
        getString(R.string.select_lrc),
        getString(R.string.ignore_lrc),
        getString(R.string.lyric_adjust_font_size),
        getString(R.string.change_offset)
      ).itemsCallback { _, _, position, _ ->
        when (position) {
          0 -> MusicServiceRemote.service?.updateLyrics(StubProvider) // 恢复默认
          1 -> MusicServiceRemote.service?.updateLyrics(EmbeddedProvider) // 内嵌
          2, 3, 4, 5 -> TODO() //  本地 酷狗 网易 QQ
          6 -> try {
            activity.getContent.launch(lrcMimeType) // 手动选择
          } catch (e: ActivityNotFoundException) {
            ToastUtil.show(activity, R.string.activity_not_found_tip)
          }

          7 -> MusicServiceRemote.service?.updateLyrics(IgnoredProvider) // 忽略
          8 -> TODO() // 调整字体大小
          9 -> activity.showLyricOffsetView() // 调整时间轴
        }
      }.show()
    // TODO: update LyricsFragment
  }

  companion object {
    private val lrcMimeType: String
      get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension("lrc") ?: "*/*"
  }
}
