package remix.myplayer.misc.update

import android.content.Context
import android.content.Intent
import remix.myplayer.R
import remix.myplayer.bean.github.Release
import remix.myplayer.misc.update.DownloadService.Companion.EXTRA_RESPONSE
import remix.myplayer.theme.Theme
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil


class UpdateListener(val mContext: Context) : Listener {

    override fun onUpdateReturned(code: Int, message: String, release: Release?) {
        val showToast = UpdateAgent.forceCheck
        if (release == null) {
            if (showToast)
                ToastUtil.show(mContext, message)
            return
        }
        when (code) {
            UpdateStatus.Yes -> {
                val builder = Theme.getBaseDialog(mContext)
                        .title(R.string.new_version_found)
                        .positiveText(R.string.update)
                        .onPositive { _, _ ->
                            mContext.startService(Intent(mContext, DownloadService::class.java)
                                    .putExtra(EXTRA_RESPONSE, release))
                        }
                        .content(release.body ?: "")

                if (!isForce(release)) {
                    builder.negativeText(R.string.cancel)
                            .negativeColorAttr(R.attr.text_color_primary)
                            .onNegative { _, _ ->
                            }
                            .neutralText(R.string.ignore_this_version)
                            .neutralColorAttr(R.attr.text_color_primary)
                            .onNeutral { _, _ -> SPUtil.putValue(mContext, SPUtil.UPDATE_KEY.NAME, UpdateAgent.getOnlineVersionCode(release).toString(), true) }
                } else {
                    builder.canceledOnTouchOutside(false)
                    builder.cancelable(false)
                }
                builder.show()
            }

            UpdateStatus.No, UpdateStatus.ErrorSizeFormat -> {
                if (showToast)
                    ToastUtil.show(mContext, message)
            }
            UpdateStatus.IGNORED -> {
//                if(showToast)
//                    ToastUtil.show(mContext, message)
            }
            else -> {
                if (showToast)
                    ToastUtil.show(mContext, message)
            }
        }
    }

    override fun onUpdateError(throwable: Throwable) {
//        ToastUtil.show(mContext, R.string.update_error, throwable)
    }

    private fun isForce(release: Release?): Boolean {
        val split = release?.name?.split("-")
        return split != null && split.size > 3
    }
}