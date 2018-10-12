package remix.myplayer.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import remix.myplayer.appshortcuts.shortcuttype.ContinuePlayShortcutType
import remix.myplayer.appshortcuts.shortcuttype.LastAddedShortcutType
import remix.myplayer.appshortcuts.shortcuttype.MyLoveShortcutType
import remix.myplayer.appshortcuts.shortcuttype.ShuffleShortcutType
import remix.myplayer.service.MusicService
import java.util.*

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class DynamicShortcutManager(private val mContext: Context) : ContextWrapper(mContext.applicationContext) {
    private var mShortcutManger: ShortcutManager? = null

    private val defaultShortcut: List<ShortcutInfo>
        get() = Arrays.asList(ContinuePlayShortcutType(mContext).shortcutInfo,
                LastAddedShortcutType(mContext).shortcutInfo,
                MyLoveShortcutType(mContext).shortcutInfo,
                ShuffleShortcutType(mContext).shortcutInfo)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            mShortcutManger = getSystemService(ShortcutManager::class.java)
    }

    fun setUpShortcut() {
        if (mShortcutManger?.dynamicShortcuts?.size == 0) {
            mShortcutManger?.dynamicShortcuts = defaultShortcut
        }
    }

    fun updateContinueShortcut(service: MusicService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            mShortcutManger?.updateShortcuts(Arrays.asList(ContinuePlayShortcutType(service).shortcutInfo))
    }
}
