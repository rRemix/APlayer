package remix.myplayer.misc.update

import android.content.Context
import android.content.Intent
import cn.bmob.v3.listener.BmobUpdateListener
import cn.bmob.v3.update.UpdateResponse
import cn.bmob.v3.update.UpdateStatus
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.R
import remix.myplayer.misc.update.UpdateService.Companion.EXTRA_RESPONSE
import remix.myplayer.theme.ThemeStore
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil

class UpdateListener(val mContext:Context) : BmobUpdateListener {

    override fun onUpdateReturned(updateStatus: Int, updateResponse: UpdateResponse?) {
        if(updateResponse == null)
            return
        when (updateStatus) {
            UpdateStatus.Yes -> MaterialDialog.Builder(mContext)
                    .title(R.string.new_version_found)
                    .titleColorAttr(R.attr.text_color_primary)
                    .positiveText(R.string.update)
                    .positiveColorAttr(R.attr.text_color_primary)
                    .onPositive { _, _ ->
                        mContext.startService(Intent(mContext, UpdateService::class.java)
                                .putExtra(EXTRA_RESPONSE, updateResponse))
                    }
                    .negativeText(R.string.cancel)
                    .negativeColorAttr(R.attr.text_color_primary)
                    .onNegative { _, _ ->

                    }
                    .neutralText(R.string.ignore_this_version)
                    .neutralColorAttr(R.attr.text_color_primary)
                    .onNeutral { _, _ -> SPUtil.putValue(mContext, SPUtil.UPDATE_KEY.NAME, updateResponse.version_i.toString(), true) }
                    .content(updateResponse.updateLog)
                    .contentColorAttr(R.attr.text_color_primary)
                    .buttonRippleColorAttr(R.attr.ripple_color)
                    .backgroundColorAttr(R.attr.background_color_3)
                    .theme(ThemeStore.getMDDialogTheme())
                    .show()
            UpdateStatus.No -> ToastUtil.show(mContext, mContext.getString(R.string.no_update))
            UpdateStatus.IGNORED -> {
//                ToastUtil.show(mContext, mContext.getString(R.string.update_ignore))
            }
            else -> ToastUtil.show(mContext, R.string.update_query_error, updateResponse.exception)
        }
    }
}