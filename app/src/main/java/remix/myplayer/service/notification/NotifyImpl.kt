package remix.myplayer.service.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.app.NotificationCompat
import android.view.View
import android.widget.RemoteViews
import remix.myplayer.R
import remix.myplayer.request.RemoteUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import remix.myplayer.util.LogUtil
import remix.myplayer.util.SPUtil

/**
 * Created by Remix on 2017/11/22.
 */

class NotifyImpl(context: MusicService) : Notify(context) {
    private lateinit var remoteView: RemoteViews
    private lateinit var remoteBigView: RemoteViews

    override fun updateForPlaying() {
        remoteBigView = RemoteViews(service.packageName, R.layout.notification_big)
        remoteView = RemoteViews(service.packageName, R.layout.notification)
        val isPlay = service.isPlaying

        buildAction(service)
        val notification = buildNotification(service)

        val song = service.currentSong
        if (song != null) {
            val isSystemColor = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR, true)

            //设置歌手，歌曲名
            remoteBigView.setTextViewText(R.id.notify_song, song.title)
            remoteBigView.setTextViewText(R.id.notify_artist_album, song.artist + " - " + song.album)

            remoteView.setTextViewText(R.id.notify_song, song.title)
            remoteView.setTextViewText(R.id.notify_artist_album, song.artist + " - " + song.album)

            //设置了黑色背景
            if (!isSystemColor) {
                remoteBigView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary))
                remoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary))
                //背景
                remoteBigView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
                remoteBigView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
                remoteView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
                remoteView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
            }
            //设置播放按钮
            if (!isPlay) {
                remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_play)
                remoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_play)
            } else {
                remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause)
                remoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause)
            }
            //设置封面
            val size = DensityUtil.dip2px(service, 128f)

            object : RemoteUriRequest(getSearchRequestWithAlbumType(song), RequestConfig.Builder(size, size).build()) {
                override fun onError(errMsg: String) {
                    remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
                    remoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
                    pushNotify(notification)
                }

                override fun onSuccess(result: Bitmap?) {
                    try {
                        //                        Bitmap result = copy(bitmap);
                        if (result != null) {
                            remoteBigView.setImageViewBitmap(R.id.notify_image, result)
                            remoteView.setImageViewBitmap(R.id.notify_image, result)
                        } else {
                            remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
                            remoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day)
                        }
                    } catch (e: Exception) {
                        LogUtil.d("Notify", e.toString())
                    } finally {
                        pushNotify(notification)
                    }
                }

            }.load()
        }
    }

    private fun buildNotification(context: Context): Notification {
        val builder = NotificationCompat.Builder(context, Notify.PLAYING_NOTIFICATION_CHANNEL_ID)
        builder.setContent(remoteView)
                .setCustomBigContentView(remoteBigView)
                .setContentText("")
                .setContentTitle("")
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(service.isPlaying)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.notifbar_icon)
        builder.setCustomBigContentView(remoteBigView)
        builder.setCustomContentView(remoteView)
        return builder.build()
    }

    private fun buildAction(context: Context) {
        //添加Action
        //切换
        val playIntent = buildPendingIntent(context, Command.TOGGLE)
        remoteBigView.setOnClickPendingIntent(R.id.notify_play, playIntent)
        remoteView.setOnClickPendingIntent(R.id.notify_play, playIntent)
        //下一首
        val nextIntent = buildPendingIntent(context, Command.NEXT)
        remoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
        remoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
        //上一首
        val prevIntent = buildPendingIntent(context, Command.PREV)
        remoteBigView.setOnClickPendingIntent(R.id.notify_prev, prevIntent)

        //关闭通知栏
        val closeIntent = buildPendingIntent(context, Command.CLOSE_NOTIFY)
        remoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent)
        remoteView.setOnClickPendingIntent(R.id.notify_close, closeIntent)

        //桌面歌词
        val lyricIntent = buildPendingIntent(context, Command.TOGGLE_FLOAT_LRC)
        remoteBigView.setOnClickPendingIntent(R.id.notify_lyric, lyricIntent)
        remoteView.setOnClickPendingIntent(R.id.notify_lyric, lyricIntent)
    }

}
