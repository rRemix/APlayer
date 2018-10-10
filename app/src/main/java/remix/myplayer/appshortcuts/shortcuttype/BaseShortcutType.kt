package remix.myplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build

import remix.myplayer.appshortcuts.AppShortcutActivity

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
abstract class BaseShortcutType constructor(val context: Context) {

    abstract val shortcutInfo: ShortcutInfo


    fun getIntent(type: Int): Intent {
        val intent = Intent(context, AppShortcutActivity::class.java)
        intent.putExtra(AppShortcutActivity.KEY_SHORTCUT_TYPE, type)
        intent.action = Intent.ACTION_VIEW
        return intent
    }

    companion object {
        val ID_PREFIX = "com.remix.myplayer.appshortcuts.id."

    }
}
