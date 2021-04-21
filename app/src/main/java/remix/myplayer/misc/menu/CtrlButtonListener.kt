package remix.myplayer.misc.menu

import android.content.Intent
import android.view.View
import remix.myplayer.R
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.util.Util

/**
 * Created by Remix on 2015/12/3.
 */
/**
 * 播放控制
 */
class CtrlButtonListener : View.OnClickListener {
  override fun onClick(v: View) {
    val intent = Intent(MusicService.ACTION_CMD)
    when (v.id) {
      R.id.lockscreen_prev, R.id.playbar_prev -> intent.putExtra(EXTRA_CONTROL, Command.PREV)
      R.id.lockscreen_next, R.id.playbar_next -> intent.putExtra(EXTRA_CONTROL, Command.NEXT)
      R.id.lockscreen_play, R.id.playbar_play -> intent.putExtra(EXTRA_CONTROL, Command.TOGGLE)
    }
    Util.sendLocalBroadcast(intent)
  }
}