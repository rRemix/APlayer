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
class MyLoveShortcutType(context: Context) : BaseShortcutType(context) {

    override val shortcutInfo: ShortcutInfo
        get() = ShortcutInfo.Builder(context, ID_PREFIX + "my_love")
                .setShortLabel(context.getString(R.string.my_favorite))
                .setLongLabel(context.getString(R.string.my_favorite))
                .setIcon(Icon.createWithResource(context, R.drawable.icon_appshortcut_my_love))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_MY_LOVE))
                .build()
}
