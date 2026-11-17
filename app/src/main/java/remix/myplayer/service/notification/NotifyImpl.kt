package remix.myplayer.service.notification

import android.app.Notification
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import remix.myplayer.R
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil


/**
 * Created by Remix on 2017/11/22.
 */

class NotifyImpl(context: MusicService) : Notify(context) {

  override fun updateAndNotify() {
    val remoteView = RemoteViews(service.packageName, R.layout.notification)
    val remoteBigView = RemoteViews(service.packageName, R.layout.notification_big)

    val isPlay = playbackState.isPlaying
    val song = playbackState.song

    buildAction(service, remoteView, remoteBigView)
    val notification = buildNotification(service, remoteView, remoteBigView)

    //设置歌手，歌曲名
    remoteBigView.setTextViewText(R.id.notify_song, song.title)
    remoteBigView.setTextViewText(R.id.notify_artist_album, song.artist + " - " + song.album)

    remoteView.setTextViewText(R.id.notify_song, song.title)
    remoteView.setTextViewText(R.id.notify_artist_album, song.artist)

    //非系统背景色 即黑色背景
    if (!service.settingPrefs.notifyUseSystemBackground) {
      //字体颜色
      remoteBigView.setTextColor(
        R.id.notify_song,
        ColorUtil.getColor(R.color.dark_text_color_primary)
      )
      remoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.dark_text_color_primary))
      remoteBigView.setTextColor(
        R.id.notify_artist_album,
        ColorUtil.getColor(R.color.dark_text_color_secondary)
      )
      remoteView.setTextColor(
        R.id.notify_artist_album,
        ColorUtil.getColor(R.color.dark_text_color_secondary)
      )
      //背景
      remoteBigView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
      remoteBigView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
      remoteView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
      remoteView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
    }

    //桌面歌词
    remoteBigView.setImageViewResource(
      R.id.notify_lyric,
      if (lyricManager.isDesktopLyricEnabled) R.drawable.icon_notify_desktop_lyric_unlock else R.drawable.icon_notify_lyric
    )
    remoteView.setImageViewResource(
      R.id.notify_lyric,
      if (lyricManager.isDesktopLyricEnabled) R.drawable.icon_notify_desktop_lyric_unlock else R.drawable.icon_notify_lyric
    )

    //设置播放按钮
    if (!isPlay) {
      remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_play)
      remoteView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_play)
    } else {
      remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_pause)
      remoteView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_pause)
    }

    //设置封面
    val size = DensityUtil.dip2px(service, 128f)
    startForegroundOrNotify(notification)

    loadCover(size) { bitmap ->
      if (bitmap != null && !bitmap.isRecycled) {
        remoteBigView.setImageViewBitmap(R.id.notify_image, bitmap)
        remoteView.setImageViewBitmap(R.id.notify_image, bitmap)
      } else {
        remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
        remoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
      }
      startForegroundOrNotify(notification)
    }
  }

  private fun buildNotification(
    context: Context,
    remoteView: RemoteViews,
    remoteBigView: RemoteViews
  ): Notification {
    val builder = NotificationCompat.Builder(context, PLAYING_NOTIFICATION_CHANNEL_ID)
    builder.setContent(remoteView)
      .setCustomBigContentView(remoteBigView)
      .setContentText("")
      .setContentTitle("")
      .setShowWhen(false)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setOngoing(playbackState.isPlaying)
      .setContentIntent(contentIntent)
      .setSmallIcon(R.drawable.icon_notifbar)
    builder.setCustomBigContentView(remoteBigView)
    builder.setCustomContentView(remoteView)
    return builder.build()
  }

  private fun buildAction(context: Context, remoteView: RemoteViews, remoteBigView: RemoteViews) {
    //添加Action
    //切换
    val playIntent = buildPendingIntent(context, Command.PLAY_PAUSE)
    remoteBigView.setOnClickPendingIntent(R.id.notify_play, playIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_play, playIntent)
    //下一首
    val nextIntent = buildPendingIntent(context, Command.SKIP_TO_NEXT)
    remoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
    //上一首
    val prevIntent = buildPendingIntent(context, Command.SKIP_TO_PREVIOUS)
    remoteBigView.setOnClickPendingIntent(R.id.notify_prev, prevIntent)

    //关闭通知栏
    val closeIntent = buildPendingIntent(context, Command.CLOSE_NOTIFY)
    remoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_close, closeIntent)

    //桌面歌词
    val lyricIntent = buildPendingIntent(
      context,
      if (lyricManager.isDesktopLyricLocked) Command.UNLOCK_DESKTOP_LYRIC else Command.TOGGLE_DESKTOP_LYRIC
    )
    remoteBigView.setOnClickPendingIntent(R.id.notify_lyric, lyricIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_lyric, lyricIntent)
  }
}
