package remix.myplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build

import remix.myplayer.R
import remix.myplayer.appshortcuts.AppShortcutActivity

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class LastAddedShortcutType(context: Context) : BaseShortcutType(context) {

    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(context, ID_PREFIX + "last_added")
                .setShortLabel(context.getString(R.string.recently))
                .setLongLabel(context.getString(R.string.recently))
                .setIcon(Icon.createWithResource(context, R.drawable.icon_appshortcut_last_add))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_LAST_ADDED))
                .build()
}
