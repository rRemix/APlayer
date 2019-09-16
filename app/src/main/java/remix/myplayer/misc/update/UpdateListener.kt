package remix.myplayer.misc.update

import android.content.Context
import android.content.Intent
import remix.myplayer.R
import remix.myplayer.bean.github.Release
import remix.myplayer.misc.update.DownloadService.Companion.EXTRA_RESPONSE
import remix.myplayer.theme.Theme
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import java.lang.ref.WeakReference


class UpdateListener(activity: Context) : Listener {
  private val ref = WeakReference(activity)

  override fun onUpdateReturned(code: Int, message: String, release: Release?) {
    val showToast = UpdateAgent.forceCheck
    if (release == null) {
      if (showToast)
        ToastUtil.show(ref.get() ?: return, message)
      return
    }
    when (code) {
      UpdateStatus.Yes -> {
        val builder = Theme.getBaseDialog(ref.get() ?: return)
            .title(R.string.new_version_found)
            .positiveText(R.string.update)
            .onPositive { _, _ ->
              ref.get()?.startService(Intent(ref.get() ?: return@onPositive, DownloadService::class.java)
                  .putExtra(EXTRA_RESPONSE, release))
            }
            .content(release.body ?: "")

        if (!isForce(release)) {
          builder
              .negativeText(R.string.ignore_check_update_forever)
              .onNegative { _, _ ->
                SPUtil.putValue(ref.get() ?: return@onNegative, SPUtil.UPDATE_KEY.NAME, SPUtil.UPDATE_KEY.IGNORE_FOREVER, true)
              }
              .neutralText(R.string.ignore_this_version)
              .neutralColorAttr(R.attr.text_color_primary)
              .onNeutral { _, _ -> SPUtil.putValue(ref.get() ?: return@onNeutral, SPUtil.UPDATE_KEY.NAME, UpdateAgent.getOnlineVersionCode(release).toString(), true) }
        } else {

          builder.canceledOnTouchOutside(false)
          builder.cancelable(false)
        }
        builder.show()
      }

      UpdateStatus.No, UpdateStatus.ErrorSizeFormat -> {
        if (showToast)
          ToastUtil.show(ref.get() ?: return, message)
      }
      UpdateStatus.IGNORED -> {
        // ignore
      }
      else -> {
        if (showToast)
          ToastUtil.show(ref.get() ?: return, message)
      }
    }
  }

  override fun onUpdateError(throwable: Throwable) {
    Timber.v("onUpdateError: $throwable")
  }

  private fun isForce(release: Release?): Boolean {
    val split = release?.name?.split("-")
    return split != null && split.size > 3
  }
}