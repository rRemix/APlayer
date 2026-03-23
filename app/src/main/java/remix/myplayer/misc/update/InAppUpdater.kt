package remix.myplayer.misc.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.data.model.github.Release
import remix.myplayer.data.prefs.InAppUpdatePrefs
import remix.myplayer.request.network.GithubApi
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdater @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val inAppUpdatePrefs: InAppUpdatePrefs,
  private val githubApi: GithubApi
) {

  private val workManager by lazy {
    WorkManager.getInstance(context)
  }

  private val notificationManager: NotificationManager by lazy {
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannelIfNeed()
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannelIfNeed() {
    val updateNotificationChannel = NotificationChannel(
      UPDATE_NOTIFICATION_CHANNEL_ID,
      context.getString(R.string.update_notification),
      NotificationManager.IMPORTANCE_LOW
    )
    updateNotificationChannel.setShowBadge(false)
    updateNotificationChannel.enableLights(false)
    updateNotificationChannel.enableVibration(false)
    updateNotificationChannel.description =
      context.getString(R.string.update_notification_description)
    notificationManager.createNotificationChannel(updateNotificationChannel)
  }

  fun cancelDownloadWorker() {
    workManager.cancelUniqueWork(UNIQUE_NAME)
  }

  fun startDownloadWorker(release: Release): Flow<WorkInfo?> {
    val json = Json.encodeToString(release)
    val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
      .setInputData(workDataOf("release" to json))
      .build()

    workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, downloadRequest)
    return workManager.getWorkInfoByIdFlow(downloadRequest.id)
  }

  suspend fun checkUpdate(force: Boolean): Release? {
    val showToast = force

    if (!force && inAppUpdatePrefs.ignoreForever) {
      return null
    }

    val release = try {
      githubApi.fetchLatestRelease("rRemix", "APlayer")
    } catch (e: Exception) {
      Timber.tag(TAG).v("e: $e")
      return null
    }
    Timber.v("assets: ${release.assets?.map { "asset: ${it.name}" }}")

    // 取第一个名称包含normal的
    val asset = release.assets?.run {
      firstOrNull { it.name?.contains("normal", ignoreCase = true) == true }
        ?: firstOrNull()
    }
    if (asset == null) {
      if (showToast) {
        MessageNotifier.show(R.string.no_update)
      }
      return null
    }

    // compare versionCode
    val versionCode = getOnlineVersionCode(release)
    if (versionCode <= getLocalVersionCode()) {
      if (showToast) {
        MessageNotifier.show(R.string.no_update)
      }
      return null
    }

    // ignore this update?
    if (!force && inAppUpdatePrefs.isVersionIgnored(versionCode)) {
      return null
    }

    // check args
    if (asset.size < 0 || asset.browser_download_url.isNullOrEmpty()) {
      if (showToast) {
        MessageNotifier.show("illegal args")
      }
      return null
    }

    return release
  }

  fun ignoreVersion(versionCode: Int) {
    inAppUpdatePrefs.setIgnoreVersion(versionCode, true)
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

  companion object {

    private const val TAG = "InAppUpdater"

    private const val UNIQUE_NAME = "download_apk"

    private const val UPDATE_NOTIFICATION_CHANNEL_ID = "update_notification"
    private const val UPDATE_NOTIFICATION_ID = 3
  }
}
