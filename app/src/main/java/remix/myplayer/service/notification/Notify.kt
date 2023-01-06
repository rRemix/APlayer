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
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.request.target.CustomTarget
import remix.myplayer.R
import remix.myplayer.misc.getPendingIntentFlag
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.ui.activity.PlayerActivity

/**
 * Created by Remix on 2017/11/22.
 */

abstract class Notify internal constructor(internal var service: MusicService) {
  private val FLAG_ALWAYS_SHOW_TICKER = 0x1000000
  private val FLAG_ONLY_UPDATE_TICKER = 0x2000000

  protected var target: CustomTarget<Bitmap>? = null

  private val notificationManager: NotificationManager by lazy {
    service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private var notifyMode = NOTIFY_MODE_BACKGROUND

  internal val contentIntent: PendingIntent
    get() {
      val result = Intent(service, PlayerActivity::class.java)
      result.putExtra(EXTRA_SONG, service.currentSong)

      val stackBuilder = TaskStackBuilder.create(service)
      stackBuilder.addParentStack(PlayerActivity::class.java)
      stackBuilder.addNextIntent(result)

//      stackBuilder.editIntentAt(1)?.putExtra(EXTRA_SHOW_ANIMATION, false)
//      stackBuilder.editIntentAt(0)?.putExtra(EXTRA_SHOW_ANIMATION, false)
      return stackBuilder.getPendingIntent(
          0,
        getPendingIntentFlag()
      )!!
    }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val playingNotificationChannel = NotificationChannel(PLAYING_NOTIFICATION_CHANNEL_ID, service.getString(R.string.playing_notification), NotificationManager.IMPORTANCE_LOW)
    playingNotificationChannel.setShowBadge(false)
    playingNotificationChannel.enableLights(false)
    playingNotificationChannel.enableVibration(false)
    playingNotificationChannel.description = service.getString(R.string.playing_notification_description)
    notificationManager.createNotificationChannel(playingNotificationChannel)
  }

  abstract fun updateForPlaying()

  fun updateWithLyric(lrc: String) {
    if (!service.isPlaying) return
    val song = service.currentSong
    val builder = NotificationCompat.Builder(service, PLAYING_NOTIFICATION_CHANNEL_ID)
    builder.setContentText("${song.artist} - ${song.album}")
        .setContentTitle(song.title)
        .setShowWhen(false)
        .setTicker(lrc)
        .setOngoing(service.isPlaying)
        .setContentIntent(contentIntent)
        .setSmallIcon(R.drawable.icon_notifbar)

    val notification = builder.build()
    notification.extras.putInt("ticker_icon", R.drawable.icon_notifibar_lrc)
    notification.extras.putBoolean("ticker_icon_switch", false)
    notification.flags = notification.flags.or(FLAG_ALWAYS_SHOW_TICKER)
    notification.flags = notification.flags.or(FLAG_ONLY_UPDATE_TICKER)
    notificationManager.notify(STATUS_BAR_LYRIC_NOTIFICATION_ID, notification)
  }

  internal fun pushNotify(notification: Notification) {
    if (service.stop)
      return
    val newNotifyMode: Int = if (service.isPlaying) {
      NOTIFY_MODE_FOREGROUND
    } else {
      NOTIFY_MODE_BACKGROUND
    }

    if (notifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
      //            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
      service.stopForeground(false)
    }
    if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        service.startForeground(PLAYING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
      } else{
        service.startForeground(PLAYING_NOTIFICATION_ID, notification)
      }
    } else {
      notificationManager.notify(PLAYING_NOTIFICATION_ID, notification)
    }

    notifyMode = newNotifyMode
    isNotifyShowing = true
  }

  /**
   * 取消通知栏
   */
  fun cancelPlayingNotify() {
    service.stopForeground(true)
    notificationManager.cancel(PLAYING_NOTIFICATION_ID)
    isNotifyShowing = false
    //        notifyMode = NOTIFY_MODE_NONE;
  }

  internal fun buildPendingIntent(context: Context, operation: Int): PendingIntent {
    val intent = Intent(MusicService.ACTION_CMD)
    intent.putExtra(EXTRA_CONTROL, operation)
    intent.component = ComponentName(context, MusicService::class.java)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return PendingIntent.getService(context, operation, intent,
        getPendingIntentFlag()
      )
    } else {
      if (operation != Command.TOGGLE_DESKTOP_LYRIC &&
          operation != Command.CLOSE_NOTIFY &&
          operation != Command.UNLOCK_DESKTOP_LYRIC) {
        return PendingIntent.getForegroundService(context, operation, intent,
          getPendingIntentFlag()
        )
      } else {
        PendingIntent.getService(context, operation, intent,
          getPendingIntentFlag()
        )
      }
    }

    return PendingIntent.getService(context, operation, intent,
      getPendingIntentFlag()
    )
  }

  companion object {
    /**
     * 通知栏是否显示
     */
    @JvmStatic
    var isNotifyShowing = false

    private const val NOTIFY_MODE_FOREGROUND = 1
    private const val NOTIFY_MODE_BACKGROUND = 2

    internal const val PLAYING_NOTIFICATION_CHANNEL_ID = "playing_notification"
    private const val PLAYING_NOTIFICATION_ID = 1
    private const val STATUS_BAR_LYRIC_NOTIFICATION_ID = 2
  }
}
