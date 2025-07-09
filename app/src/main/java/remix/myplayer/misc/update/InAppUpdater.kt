package remix.myplayer.misc.update

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.github.Release
import remix.myplayer.compose.prefs.InAppUpdatePrefs
import remix.myplayer.compose.prefs.delegate
import remix.myplayer.misc.update.UpdateAgent.forceCheck
import remix.myplayer.misc.update.UpdateAgent.listener
import remix.myplayer.request.network.HttpClient
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdater @Inject constructor(
  @ApplicationContext private val context: Context,
  private val inAppUpdatePrefs: InAppUpdatePrefs
) {

  suspend fun checkUpdate(force: Boolean): Release? {
    if (!force && inAppUpdatePrefs.ignoreForever) {
      return null
    }

    val release = withContext(Dispatchers.IO) {
      try {
        HttpClient.fetchLatestRelease("rRemix", "APlayer").blockingGet()
      } catch (e: Exception) {
        Timber.w(e)
        null
      }
    } ?: return null

    val showToast = force

    // no assets
    val asset = release.assets?.get(0)
    if (asset == null) {
      listener?.onUpdateReturned(UpdateStatus.No, context.getString(R.string.no_update), null)

      if (showToast) {
        ToastUtil.show(context, R.string.no_update)
      }
      return null
    }

    // compare versionCode
    val versionCode = getOnlineVersionCode(release)
    if (versionCode <= getLocalVersionCode()) {
      listener?.onUpdateReturned(UpdateStatus.No, context.getString(R.string.no_update), null)
      // remove old apks
      val downloadDir = File(context.externalCacheDir, "download")
      if (downloadDir.exists() && downloadDir.listFiles()?.isNotEmpty() == true) {
        Util.deleteFilesByDirectory(downloadDir)
      }

      if (showToast) {
        ToastUtil.show(context, R.string.no_update)
      }
      return null
    }

    // ignore this update?
    val ignoreCurrentVersion by inAppUpdatePrefs.sp.delegate(versionCode.toString(), false)
    if (!forceCheck && ignoreCurrentVersion) {
      if (showToast) {
        ToastUtil.show(context, R.string.update_ignore)
      }
      return null
    }

    // check args
    if (asset.size < 0 || asset.browser_download_url.isNullOrEmpty()) {
      if (showToast) {
        ToastUtil.show(context, "illegal args")
      }
      return null
    }

    return release
  }

  fun ignoreVersion(versionCode: Int) {
    var ignoreThisVersion by inAppUpdatePrefs.sp.delegate(versionCode.toString(), false)
    ignoreThisVersion = true
  }

  fun ignoreForever() {
    inAppUpdatePrefs.ignoreForever = true
  }

  private fun getLocalVersionCode(): Int {
    var versionCode = 0
    try {
      versionCode =
        App.context.packageManager.getPackageInfo(App.context.packageName, 0).versionCode
    } catch (e: PackageManager.NameNotFoundException) {
      Timber.v(e)
    }
    return versionCode
  }

  fun getOnlineVersionCode(release: Release): Int {
    //Release-v1.3.5.2-80
    release.name?.run {
      val numberAndCode = this.split("-")
      if (numberAndCode.size < 2)
        return 0
      return numberAndCode[2].toInt()
    }
    return 0
  }
}