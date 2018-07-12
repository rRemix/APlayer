package remix.myplayer.misc.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.TextUtils
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.BmobUpdateListener
import cn.bmob.v3.listener.FindListener
import cn.bmob.v3.update.AppVersion
import cn.bmob.v3.update.UpdateResponse
import cn.bmob.v3.update.UpdateStatus
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.misc.update.UpdateService.Companion.EXTRA_RESPONSE
import remix.myplayer.theme.ThemeStore
import remix.myplayer.util.LogUtil
import remix.myplayer.util.SPUtil

object UpdateAgent {
    private const val TAG = "UpdateAgent"

    var listener: BmobUpdateListener? = null

    var cancelIgnore = false

    private val versionCode: Int
        get() {
            var versionCode = 0
            try {
                versionCode = App.getContext().packageManager.getPackageInfo(App.getContext().packageName, 0).versionCode
            } catch (e: PackageManager.NameNotFoundException) {
                LogUtil.e(TAG, e)
            }

            return versionCode
        }

    fun check(context: Context) {
        if(listener == null)
            return
        val bmobQuery = BmobQuery<AppVersion>()
        bmobQuery.addWhereEqualTo("platform", "Android")
        bmobQuery.addWhereGreaterThan("version_i", versionCode)
        bmobQuery.order("-version_i")
        bmobQuery.findObjects(object : FindListener<AppVersion>() {
            override fun done(list: List<AppVersion>?, e: BmobException?) {
                if (e != null) {
                    listener?.onUpdateReturned(-1, UpdateResponse(e.errorCode, e.message))
                    return
                }
                if (list == null || list.isEmpty()) {
                    listener?.onUpdateReturned(UpdateStatus.No, UpdateResponse(UpdateStatus.No, context.getString(R.string.no_update)))
                    //todo 删除以前下载的
                    return
                }
                val updateResponse = UpdateResponse(list[0])
                //是否忽略了该版本的更新
                if(!cancelIgnore && SPUtil.getValue(context,SPUtil.UPDATE_KEY.NAME,updateResponse.version_i.toString(),false)){
                    listener?.onUpdateReturned(UpdateStatus.IGNORED, UpdateResponse(UpdateStatus.IGNORED, "该版本已经忽略"))
                    return
                }
                if (updateResponse.target_size <= 0L) {
                    listener?.onUpdateReturned(UpdateStatus.EmptyField, UpdateResponse(UpdateStatus.EmptyField, "target_size为空或格式不对，请填写apk文件大小(long类型)。"))
                    return
                }
                if (TextUtils.isEmpty(updateResponse.path)) {
                    listener?.onUpdateReturned(UpdateStatus.EmptyField, UpdateResponse(UpdateStatus.EmptyField, "path/android_url需填写其中之一。"))
                    return
                }
                listener?.onUpdateReturned(UpdateStatus.Yes, updateResponse)
            }
        })
    }

    fun showUpdateDialog(context: Context,updateResponse:UpdateResponse){
        MaterialDialog.Builder(context)
                .title(R.string.new_version_found)
                .titleColorAttr(R.attr.text_color_primary)
                .positiveText(R.string.update)
                .positiveColorAttr(R.attr.text_color_primary)
                .onPositive { _, _ ->
                    context.startService(Intent(context, UpdateService::class.java)
                            .putExtra(EXTRA_RESPONSE, updateResponse))
                }
                .negativeText(R.string.cancel)
                .negativeColorAttr(R.attr.text_color_primary)
                .onNegative { _, _ ->

                }
                .neutralText(R.string.ignore_this_version)
                .neutralColorAttr(R.attr.text_color_primary)
                .onNeutral { _, _ -> SPUtil.putValue(context, SPUtil.UPDATE_KEY.NAME, updateResponse.version_i.toString(), true) }
                .content(updateResponse.updateLog)
                .contentColorAttr(R.attr.text_color_primary)
                .buttonRippleColorAttr(R.attr.ripple_color)
                .backgroundColorAttr(R.attr.background_color_3)
                .theme(ThemeStore.getMDDialogTheme())
                .show()
    }

}
