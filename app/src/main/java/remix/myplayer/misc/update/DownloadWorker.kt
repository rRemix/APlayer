package remix.myplayer.misc.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import remix.myplayer.R
import remix.myplayer.data.model.github.Release
import remix.myplayer.request.network.GithubApi
import timber.log.Timber
import java.io.File
import java.io.IOException

class DownloadWorker(private val context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {

  private val githubApi: GithubApi by lazy {
    EntryPointAccessors.fromApplication(
      context.applicationContext,
      DownloadWorkerEntryPoint::class.java
    ).githubApi()
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface DownloadWorkerEntryPoint {
    fun githubApi(): GithubApi
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
    val channel = NotificationChannel(
      UPDATE_NOTIFICATION_CHANNEL_ID,
      context.getString(R.string.update_notification),
      NotificationManager.IMPORTANCE_LOW
    )
    channel.setShowBadge(false)
    channel.enableLights(false)
    channel.enableVibration(false)
    channel.description =
      context.getString(R.string.update_notification_description)
    notificationManager.createNotificationChannel(channel)
  }

  // TODO 断点下载？
  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val release = try {
      Json.decodeFromString<Release>(inputData.getString("release")!!)
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, "Failed to parse release data")
      return@withContext Result.failure()
    }

    try {
      val asset = release.assets?.firstOrNull()
      if (asset == null) {
        Timber.tag(TAG).e("No assets found in release")
        return@withContext Result.failure()
      }

      val downloadUrl = asset.browser_download_url
      if (downloadUrl.isNullOrEmpty()) {
        Timber.tag(TAG).e("Download URL is empty")
        return@withContext Result.failure()
      }

      val downloadDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          ?: return@withContext Result.failure(), "apks"
      )
      if (!downloadDir.exists() && !downloadDir.mkdirs()) {
        throw IOException("Failed to create download directory")
      }

      val downloadFile = File(downloadDir, "${release.name}.apk")

      // 检查文件是否已存在且完整
      if (downloadFile.exists() && downloadFile.length() == asset.size) {
        return@withContext Result.success(workDataOf(EXTRA_FILE_PATH to downloadFile.absolutePath))
      }

      // 删除不完整的文件
      if (downloadFile.exists()) {
        downloadFile.delete()
      }

      // 显示初始通知
      setForeground(createForegroundInfo(asset.size, 0))

      val response = githubApi.downloadFile(downloadUrl)
      if (!response.isSuccessful) {
        throw IOException("response not successful ${response.code()}")
      }

      val responseBody = response.body() ?: throw IOException("response body is null")
      val totalSize = responseBody.contentLength()

      response.body()?.byteStream()?.use { input ->
        val buf = ByteArray(8 * 1024)
        var downloadSize = 0L

        downloadFile.outputStream().use { output ->
          while (true) {
            val numRead = input.read(buf)
            if (numRead == -1) break
            output.write(buf, 0, numRead)
            downloadSize += numRead

            // 通知栏进度
            setForeground(createForegroundInfo(totalSize, downloadSize))
          }
          output.flush()
        }
      }

      return@withContext Result.success(workDataOf(EXTRA_FILE_PATH to downloadFile.absolutePath))
    } catch (e: Exception) {
      Timber.tag(TAG).v("download fail: $e")
      return@withContext Result.failure()
    } finally {
      notificationManager.cancel(UPDATE_NOTIFICATION_ID)
    }
  }

  private fun createForegroundInfo(totalSize: Long, downloadSize: Long): ForegroundInfo {
    val builder = NotificationCompat.Builder(context, UPDATE_NOTIFICATION_CHANNEL_ID)
      .setContentIntent(null)
      .setContentTitle(context.getString(R.string.downloading))
      .setProgress(totalSize.toInt(), downloadSize.toInt(), false)
      .setSmallIcon(R.drawable.icon_notifbar)
      .setAutoCancel(false)
      .setShowWhen(false)
      .setOngoing(true)
      .addAction(
        R.drawable.ic_delete_black_24dp,
        context.getString(R.string.cancel),
        WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
      )
    if (totalSize == 0L) {
      builder.setTicker(context.getString(R.string.downloading))
    }

    val notification = builder.build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(
        UPDATE_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      ForegroundInfo(UPDATE_NOTIFICATION_ID, notification)
    }
  }


  companion object {

    private const val TAG = "DownloadWorker"

    private const val UPDATE_NOTIFICATION_CHANNEL_ID = "update_notification"
    private const val UPDATE_NOTIFICATION_ID = 3

    const val EXTRA_FILE_PATH = "extra_file_path"

  }
}
