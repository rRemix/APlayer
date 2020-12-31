package remix.myplayer.appshortcuts

import android.content.Intent
import android.os.Bundle
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity

/**
 * Created by Remix on 2017/11/1.
 */

class AppShortcutActivity : BaseMusicActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val type = if (intent != null) intent.getIntExtra(KEY_SHORTCUT_TYPE, -1) else -1
    startService(type)
    finish()
  }

  private fun startService(type: Int) {
    val intent = Intent(this, MusicService::class.java)
    when (type) {
      SHORTCUT_TYPE_LAST_ADDED -> intent.action = MusicService.ACTION_SHORTCUT_LASTADDED
      SHORTCUT_TYPE_SHUFFLE_ALL -> intent.action = MusicService.ACTION_SHORTCUT_SHUFFLE
      SHORTCUT_TYPE_MY_LOVE -> intent.action = MusicService.ACTION_SHORTCUT_MYLOVE
    }
    startService(intent)
  }

  companion object {
    const val SHORTCUT_TYPE_SHUFFLE_ALL = 0
    const val SHORTCUT_TYPE_MY_LOVE = 1
    const val SHORTCUT_TYPE_LAST_ADDED = 2

    const val KEY_SHORTCUT_TYPE = "com.remix.myplayer.appshortcuts.ShortcutType"
  }
}
