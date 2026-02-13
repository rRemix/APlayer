package remix.myplayer.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.request.target.CustomTarget
import remix.myplayer.R
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_COMMAND
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.activity.ComposeActivity
import remix.myplayer.ui.nav.playingScreenDeepLink
import remix.myplayer.util.ext.getPendingIntentFlag
import timber.log.Timber

/**
 * Created by Remix on 2017/11/22.
 */

abstract class Notify internal constructor(internal var service: MusicService) {

  protected val playbackState
    get() = MusicStateSource.currentPlaybackUiState

  private val FLAG_ALWAYS_SHOW_TICKER = 0x1000000
  private val FLAG_ONLY_UPDATE_TICKER = 0x2000000

  protected var target: CustomTarget<Bitmap>? = null

  protected val lyricManager = service.lyricManager

  var isNotifyShowing = false
    set(value) {
      field = value
      lyricManager.isNotifyShowing = value
    }

  private val notificationManager: NotificationManager by lazy {
    service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  internal val contentIntent: PendingIntent
    get() = TaskStackBuilder.create(service).run {
      addNextIntentWithParentStack(
        Intent(
          Intent.ACTION_VIEW,
          playingScreenDeepLink,
          service,
          ComposeActivity::class.java
        )
      )
      getPendingIntent(0, getPendingIntentFlag())!!
    }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val playingNotificationChannel = NotificationChannel(
      PLAYING_NOTIFICATION_CHANNEL_ID,
      service.getString(R.string.playing_notification), NotificationManager.IMPORTANCE_LOW
    )
    playingNotificationChannel.setShowBadge(false)
    playingNotificationChannel.enableLights(false)
    playingNotificationChannel.enableVibration(false)
    playingNotificationChannel.description = service.getString(
      R.string.playing_notification_description
    )
    notificationManager.createNotificationChannel(playingNotificationChannel)
  }

  abstract fun updateAndNotify()

  fun updateWithLyric(lrc: String) {
    if (!playbackState.isPlaying) return
    val song = playbackState.song
    val builder = NotificationCompat.Builder(service, PLAYING_NOTIFICATION_CHANNEL_ID)
    builder.setContentText("${song.artist} - ${song.album}")
      .setContentTitle(song.title)
      .setShowWhen(false)
      .setTicker(lrc)
      .setOngoing(playbackState.isPlaying)
      .setContentIntent(contentIntent)
      .setSmallIcon(R.drawable.icon_notifbar)

    val notification = builder.build()
    notification.extras.putInt("ticker_icon", R.drawable.icon_notifibar_lrc)
    notification.extras.putBoolean("ticker_icon_switch", false)
    notification.flags = notification.flags.or(FLAG_ALWAYS_SHOW_TICKER)
    notification.flags = notification.flags.or(FLAG_ONLY_UPDATE_TICKER)
    notificationManager.notify(STATUS_BAR_LYRIC_NOTIFICATION_ID, notification)
  }

  fun clearStatusBarLyricNotification() {
    notificationManager.cancel(STATUS_BAR_LYRIC_NOTIFICATION_ID)
  }

  var isForeground = false
    private set

  fun startForegroundOrNotify(notification: Notification) {
    val song = playbackState.song
    val playing = playbackState.isPlaying

    Timber.v("startForegroundOrNotify, song: $song playing: $playing")

    if (service.stop || !song.valid()) {
      return
    }

    if (isForeground && !playing) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_DETACH)
        isForeground = false
      }
    }

    if (!isForeground && playing) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          service.startForeground(
            PLAYING_NOTIFICATION_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
          )
        } else {
          service.startForeground(PLAYING_NOTIFICATION_ID, notification)
        }
        isForeground = true
      } catch (e: Exception) {
        notificationManager.notify(PLAYING_NOTIFICATION_ID, notification)
        Timber.w(e, "startForeground failed, fallback to notify")
      }
    } else {
      notificationManager.notify(PLAYING_NOTIFICATION_ID, notification)
    }

    isNotifyShowing = true
  }

  /**
   * 取消通知栏
   */
  fun stopForegroundAndNotification() {
    Timber.v("stopForegroundAndNotification")
    ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    notificationManager.cancel(PLAYING_NOTIFICATION_ID)
    clearStatusBarLyricNotification()
    isForeground = false
    isNotifyShowing = false
  }

  internal fun buildPendingIntent(context: Context, cmd: Int): PendingIntent {
    val intent = Intent(MusicService.ACTION_CMD)
    intent.putExtra(EXTRA_COMMAND, cmd)
    intent.component = ComponentName(context, MusicService::class.java)

    return PendingIntent.getService(
      context, cmd, intent,
      getPendingIntentFlag()
    )
  }

  companion object {

    internal const val PLAYING_NOTIFICATION_CHANNEL_ID = "playing_notification"
    private const val PLAYING_NOTIFICATION_ID = 1
    private const val STATUS_BAR_LYRIC_NOTIFICATION_ID = 2
  }
}
