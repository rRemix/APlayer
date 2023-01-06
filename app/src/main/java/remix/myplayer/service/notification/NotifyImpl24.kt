package remix.myplayer.service.notification

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.glide.GlideApp
import remix.myplayer.glide.UriFetcher
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.util.DensityUtil
import timber.log.Timber

/**
 * Created by Remix on 2017/11/22.
 */
@TargetApi(Build.VERSION_CODES.O)
class
NotifyImpl24(context: MusicService) : Notify(context) {
  private val defaultBitmap = BitmapFactory.decodeResource(service.resources, R.drawable.album_empty_bg_night)
  private val size = DensityUtil.dip2px(service, 128f)

  override fun updateForPlaying() {
    val song = service.currentSong

    //设置封面
    GlideApp.with(service).clear(target)
    target = GlideApp.with(service)
      .asBitmap()
      .load(song)
      .centerCrop()
      .signature(ObjectKey(UriFetcher.albumVersion))
      .override(size, size)
      .into(object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
          updateWithBitmap(resource, song)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
          updateWithBitmap(defaultBitmap, song)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
        }

        override fun onLoadStarted(placeholder: Drawable?) {
          updateWithBitmap(null, song)
        }
      })
  }

  private fun updateWithBitmap(bitmap: Bitmap?, song: Song) {
    if (song.id != service.currentSong.id) {
      return
    }
    val playPauseIcon =
      if (service.isPlaying) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp

    val deleteIntent = Intent(MusicService.ACTION_CMD)
    deleteIntent.putExtra(EXTRA_CONTROL, Command.CLOSE_NOTIFY)

    val desktopLyricLock = service.isDesktopLyricLocked

    val notification = NotificationCompat.Builder(service, PLAYING_NOTIFICATION_CHANNEL_ID)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.icon_notifbar)
        .addAction(R.drawable.ic_skip_previous_black_24dp, service.getString(R.string.previous),
            buildPendingIntent(service, Command.PREV))
        .addAction(playPauseIcon, service.getString(R.string.play_pause),
            buildPendingIntent(service, Command.TOGGLE))
        .addAction(R.drawable.ic_skip_next_black_24dp, service.getString(R.string.next),
            buildPendingIntent(service, Command.NEXT))
        //根据当前桌面歌词的状态判断是显示开关桌面歌词还是解锁桌面歌词
        //当前显示了桌面歌词并且已经锁定,显示解锁的按钮
        .addAction(if (desktopLyricLock) R.drawable.ic_lock_open_black_24dp else R.drawable.ic_desktop_lyric_black_24dp,
            service.getString(if (desktopLyricLock) R.string.desktop_lyric__unlock else R.string.desktop_lyric_lock),
            buildPendingIntent(service, if (desktopLyricLock) Command.UNLOCK_DESKTOP_LYRIC else Command.TOGGLE_DESKTOP_LYRIC))
        .setDeleteIntent(buildPendingIntent(service, Command.CLOSE_NOTIFY))
        .setContentIntent(contentIntent)
        .setContentTitle(song.title)
        .setLargeIcon(bitmap)
        .setShowWhen(false)
        .setOngoing(service.isPlaying)
        .setPriority(PRIORITY_MAX)
        .setContentText(song.artist + " - " + song.album)
        .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2, 3)
            .setMediaSession(service.mediaSession.sessionToken))
        .build()
    pushNotify(notification)
  }
}
