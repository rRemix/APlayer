package remix.myplayer.misc.update

import android.content.Context
import android.content.Intent
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.R
import remix.myplayer.bean.github.Release
import remix.myplayer.misc.update.DownloadService.Companion.EXTRA_RESPONSE
import remix.myplayer.theme.ThemeStore
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil

class UpdateListener(val mContext: Context) : Listener {
    override fun onUpdateReturned(code: Int, message: String, release: Release?) {
        val showToast= UpdateAgent.forceCheck
        if (release == null ){
            if(showToast)
                ToastUtil.show(mContext,message)
            return
        }
        when (code) {
            UpdateStatus.Yes -> MaterialDialog.Builder(mContext)
                    .title(R.string.new_version_found)
                    .titleColorAttr(R.attr.text_color_primary)
                    .positiveText(R.string.update)
                    .positiveColorAttr(R.attr.text_color_primary)
                    .onPositive { _, _ ->
                        mContext.startService(Intent(mContext, DownloadService::class.java)
                                .putExtra(EXTRA_RESPONSE, release))
                    }
                    .negativeText(R.string.cancel)
                    .negativeColorAttr(R.attr.text_color_primary)
                    .onNegative { _, _ ->
                    }
                    .neutralText(R.string.ignore_this_version)
                    .neutralColorAttr(R.attr.text_color_primary)
                    .onNeutral { _, _ -> SPUtil.putValue(mContext, SPUtil.UPDATE_KEY.NAME, UpdateAgent.getOnlineVersionCode(release).toString(), true) }
                    .content(release.body)
                    .contentColorAttr(R.attr.text_color_primary)
                    .buttonRippleColorAttr(R.attr.ripple_color)
                    .backgroundColorAttr(R.attr.background_color_3)
                    .theme(ThemeStore.getMDDialogTheme())
                    .show()
            UpdateStatus.No,UpdateStatus.ErrorSizeFormat -> {
                if(showToast)
                    ToastUtil.show(mContext, message)
            }
            UpdateStatus.IGNORED -> {
//                if(showToast)
//                    ToastUtil.show(mContext, message)
            }
            else -> {
                if(showToast)
                    ToastUtil.show(mContext, message)
            }
        }
    }

    override fun onUpdateError(throwable: Throwable) {
        ToastUtil.show(mContext, R.string.update_error, throwable)
    }

}