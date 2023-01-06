package remix.myplayer.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.appwidgets.big.AppWidgetBig
import remix.myplayer.bean.mp3.Song
import remix.myplayer.glide.GlideApp
import remix.myplayer.glide.UriFetcher
import remix.myplayer.misc.getPendingIntentFlag
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.ui.activity.MainActivity
import remix.myplayer.util.DensityUtil
import timber.log.Timber

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

abstract class BaseAppwidget
  : AppWidgetProvider() {

  protected lateinit var skin: AppWidgetSkin

//  private val defaultDrawableRes: Int
//    @DrawableRes
//    get() = if (skin == WHITE_1F) R.drawable.album_empty_bg_night else R.drawable.album_empty_bg_day

  private fun buildServicePendingIntent(context: Context, componentName: ComponentName, cmd: Int): PendingIntent {
    val intent = Intent(MusicService.ACTION_APPWIDGET_OPERATE)
    intent.putExtra(EXTRA_CONTROL, cmd)
    intent.component = componentName
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isAllowForForegroundService(cmd)) {
      PendingIntent.getForegroundService(context, cmd, intent, getPendingIntentFlag())
    } else {
      PendingIntent.getService(context, cmd, intent, getPendingIntentFlag())
    }
  }

  private fun isAllowForForegroundService(cmd: Int): Boolean {
    return cmd != Command.CHANGE_MODEL && cmd != Command.LOVE && cmd != Command.TOGGLE_TIMER
  }

  protected fun hasInstances(context: Context): Boolean {
    try {
      val appIds = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, javaClass))
      return appIds != null && appIds.isNotEmpty()
    } catch (e: Exception) {
      Timber.v(e)
    }
    return false
  }

  protected fun updateCover(service: MusicService, remoteViews: RemoteViews, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    val size = if (this.javaClass.simpleName == AppWidgetBig::class.java.simpleName) IMAGE_SIZE_BIG else IMAGE_SIZE_MEDIUM

    GlideApp.with(service)
        .asBitmap()
        .load(song)
        .centerCrop()
        .signature(ObjectKey(UriFetcher.albumVersion))
        .override(size, size)
        .into(AppWidgetTarget(service, size, size, R.id.appwidget_image, remoteViews, ComponentName(service, javaClass)))
  }

  protected fun buildAction(context: Context, views: RemoteViews) {
    val componentNameForService = ComponentName(context, MusicService::class.java)
    views.setOnClickPendingIntent(R.id.appwidget_toggle, buildServicePendingIntent(context, componentNameForService, Command.TOGGLE))
    views.setOnClickPendingIntent(R.id.appwidget_prev, buildServicePendingIntent(context, componentNameForService, Command.PREV))
    views.setOnClickPendingIntent(R.id.appwidget_next, buildServicePendingIntent(context, componentNameForService, Command.NEXT))
    views.setOnClickPendingIntent(R.id.appwidget_model, buildServicePendingIntent(context, componentNameForService, Command.CHANGE_MODEL))
    views.setOnClickPendingIntent(R.id.appwidget_love, buildServicePendingIntent(context, componentNameForService, Command.LOVE))
    views.setOnClickPendingIntent(R.id.appwidget_timer, buildServicePendingIntent(context, componentNameForService, Command.TOGGLE_TIMER))

    val action = Intent(context, MainActivity::class.java)
    action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    views.setOnClickPendingIntent(R.id.appwidget_clickable, PendingIntent.getActivity(context, 0, action, getPendingIntentFlag()))
  }

  protected fun pushUpdate(context: Context, appWidgetId: IntArray?, remoteViews: RemoteViews) {
    if (!hasInstances(context)) {
      return
    }
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetId != null) {
      appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    } else {
      appWidgetManager.updateAppWidget(ComponentName(context, javaClass), remoteViews)
    }
  }

  protected fun pushPartiallyUpdate(context: Context, appWidgetId: IntArray?, remoteViews: RemoteViews) {
    if (!hasInstances(context)) {
      return
    }
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetId != null) {
      appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }
  }

  protected fun updateRemoteViews(service: MusicService, remoteViews: RemoteViews, song: Song) {
    //        int skin = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
    //        skin = skin == SKIN_TRANSPARENT ? AppWidgetSkin.TRANSPARENT : AppWidgetSkin.WHITE_1F;
    //        updateBackground(remoteViews);
    updateTitle(remoteViews, song)
    updateArtist(remoteViews, song)
    //        updateSkin(remoteViews);
    updatePlayPause(service, remoteViews)
    updateLove(service, remoteViews, song)
    updateModel(service, remoteViews)
    updateNextAndPrev(remoteViews)
    updateProgress(service, remoteViews, song)
    updateTimer(remoteViews)
  }

  private fun updateTimer(remoteViews: RemoteViews) {
    remoteViews.setImageViewResource(R.id.appwidget_timer, skin.timerRes)
  }

  private fun updateProgress(service: MusicService, remoteViews: RemoteViews, song: Song) {
    //设置时间
    remoteViews.setTextColor(R.id.appwidget_progress, skin.progressColor)
    //进度
    remoteViews.setProgressBar(R.id.appwidget_seekbar, song.duration.toInt(), service.progress, false)
  }

  private fun updateLove(service: MusicService, remoteViews: RemoteViews, song: Song) {
    remoteViews.setImageViewResource(R.id.appwidget_love, if (service.isLove) skin.lovedRes else skin.loveRes)
  }

  private fun updateNextAndPrev(remoteViews: RemoteViews) {
    //上下首歌曲
    remoteViews.setImageViewResource(R.id.appwidget_next, skin.nextRes)
    remoteViews.setImageViewResource(R.id.appwidget_prev, skin.prevRes)
  }

  private fun updateModel(service: MusicService, remoteViews: RemoteViews) {
    //播放模式
    remoteViews.setImageViewResource(R.id.appwidget_model, skin.getModeRes(service))
  }

  private fun updatePlayPause(service: MusicService, remoteViews: RemoteViews) {
    //播放暂停按钮
    remoteViews.setImageViewResource(R.id.appwidget_toggle, if (service.isPlaying) skin.pauseRes else skin.playRes)
  }

  private fun updateTitle(remoteViews: RemoteViews, song: Song) {
    //歌曲名
    remoteViews.setTextColor(R.id.appwidget_title, skin.titleColor)
    remoteViews.setTextViewText(R.id.appwidget_title, song.title)
  }

  private fun updateArtist(remoteViews: RemoteViews, song: Song) {
    //歌手名
    remoteViews.setTextColor(R.id.appwidget_artist, skin.artistColor)
    remoteViews.setTextViewText(R.id.appwidget_artist, song.artist)
  }

  protected fun updateBackground(remoteViews: RemoteViews) {
    remoteViews.setImageViewResource(R.id.appwidget_clickable, skin.background)
  }

  abstract fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean)

  abstract fun partiallyUpdateWidget(service: MusicService)

  companion object {
    const val EXTRA_WIDGET_NAME = "WidgetName"
    const val EXTRA_WIDGET_IDS = "WidgetIds"


    val SKIN_WHITE_1F = 1//白色不带透明
    val SKIN_TRANSPARENT = 2//透明

    private val IMAGE_SIZE_BIG = DensityUtil.dip2px(App.context, 270f)
    private val IMAGE_SIZE_MEDIUM = DensityUtil.dip2px(App.context, 72f)

  }
}
