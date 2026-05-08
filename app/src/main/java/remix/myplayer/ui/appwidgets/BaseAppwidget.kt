package remix.myplayer.ui.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_COMMAND
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.activity.ComposeActivity
import remix.myplayer.ui.appwidgets.big.AppWidgetBig
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.ext.getPendingIntentFlag
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

  protected val progressState
    get() = MusicStateSource.currentProgressState
  protected val playbackState
    get() = MusicStateSource.currentPlaybackUiState

  private val defaultDrawableRes: Int
    get() = R.drawable.album_empty_bg_night

  private fun buildServicePendingIntent(
    context: Context,
    componentName: ComponentName,
    cmd: Int
  ): PendingIntent {
    val intent = Intent(MusicService.ACTION_APPWIDGET_OPERATE)
    intent.putExtra(EXTRA_COMMAND, cmd)
    intent.component = componentName
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
      && Command.isAllowForForegroundService(cmd)
    ) {
      PendingIntent.getForegroundService(context, cmd, intent, getPendingIntentFlag())
    } else {
      PendingIntent.getService(context, cmd, intent, getPendingIntentFlag())
    }
  }

  protected fun hasInstances(context: Context): Boolean {
    try {
      val appIds =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, javaClass))
      return appIds != null && appIds.isNotEmpty()
    } catch (e: Exception) {
      Timber.v(e)
    }
    return false
  }

  protected fun updateCover(
    context: Context,
    remoteViews: RemoteViews,
    appWidgetIds: IntArray?,
    reloadCover: Boolean
  ) {
    val song = playbackState.song
    val size =
      if (this.javaClass.simpleName == AppWidgetBig::class.java.simpleName) IMAGE_SIZE_BIG else IMAGE_SIZE_MEDIUM

    Glide.with(context)
      .asBitmap()
      .load(song)
      .centerCrop()
      .override(size, size)
      .into(object : CustomTarget<Bitmap>() {
        override fun onLoadStarted(placeholder: Drawable?) {
          updateWithDefaultCover()
        }

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
          if (song.id != playbackState.song.id) {
            return
          }
          if (resource.isRecycled) {
            updateWithDefaultCover()
            return
          }

          remoteViews.setImageViewBitmap(R.id.appwidget_image, resource)
          pushUpdate(context, appWidgetIds, remoteViews)
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
          updateWithDefaultCover()
        }

        override fun onLoadCleared(placeholder: Drawable?) {
        }

        private fun updateWithDefaultCover() {
          if (song.id != playbackState.song.id) {
            return
          }
          remoteViews.setImageViewResource(R.id.appwidget_image, defaultDrawableRes)
          pushUpdate(context, appWidgetIds, remoteViews)
        }
      })
  }

  protected fun buildAction(context: Context, views: RemoteViews) {
    val componentNameForService = ComponentName(context, MusicService::class.java)
    views.setOnClickPendingIntent(
      R.id.appwidget_toggle,
      buildServicePendingIntent(context, componentNameForService, Command.PLAY_PAUSE)
    )
    views.setOnClickPendingIntent(
      R.id.appwidget_prev,
      buildServicePendingIntent(context, componentNameForService, Command.SKIP_TO_PREVIOUS)
    )
    views.setOnClickPendingIntent(
      R.id.appwidget_next,
      buildServicePendingIntent(context, componentNameForService, Command.SKIP_TO_NEXT)
    )
    views.setOnClickPendingIntent(
      R.id.appwidget_model,
      buildServicePendingIntent(context, componentNameForService, Command.CHANGE_MODEL)
    )
    views.setOnClickPendingIntent(
      R.id.appwidget_love,
      buildServicePendingIntent(context, componentNameForService, Command.LOVE)
    )
    views.setOnClickPendingIntent(
      R.id.appwidget_timer,
      buildServicePendingIntent(context, componentNameForService, Command.TOGGLE_TIMER)
    )

    val action = Intent(context, ComposeActivity::class.java)
    action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    views.setOnClickPendingIntent(
      R.id.appwidget_clickable,
      PendingIntent.getActivity(context, 0, action, getPendingIntentFlag())
    )
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

  protected fun pushPartiallyUpdate(
    context: Context,
    appWidgetId: IntArray?,
    remoteViews: RemoteViews
  ) {
    if (!hasInstances(context)) {
      return
    }
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetId != null) {
      appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }
  }

  protected fun updateRemoteViews(
    context: Context,
    remoteViews: RemoteViews,
    song: Song,
    primaryColor: Int
  ) {
    //        int skin = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
    //        skin = skin == SKIN_TRANSPARENT ? AppWidgetSkin.TRANSPARENT : AppWidgetSkin.WHITE_1F;
    //        updateBackground(remoteViews);
    updateTitle(remoteViews, song)
    updateArtist(remoteViews, song)
    //        updateSkin(remoteViews);
    updatePlayPause(remoteViews)
    updateLove(remoteViews)
    updateModel(remoteViews)
    updateNextAndPrev(remoteViews)
    updateProgress(remoteViews, song, primaryColor)
    updateTimer(remoteViews)
  }

  private fun updateTimer(remoteViews: RemoteViews) {
    remoteViews.setImageViewResource(R.id.appwidget_timer, skin.timerRes)
  }

  private fun updateProgress(remoteViews: RemoteViews, song: Song, primaryColor: Int) {
    // 设置时间
    remoteViews.setTextColor(R.id.appwidget_progress, skin.progressColor)
    // 进度
    remoteViews.setProgressBar(
      R.id.appwidget_seekbar,
      song.duration.toInt(),
      progressState.position.toInt(),
      false
    )
    // 轨道颜色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val tint = ColorStateList.valueOf(primaryColor)
      remoteViews.setColorStateList(R.id.appwidget_seekbar, "setProgressTintList", tint)
    }
  }

  private fun updateLove(remoteViews: RemoteViews) {
    remoteViews.setImageViewResource(
      R.id.appwidget_love,
      if (playbackState.isFavorite) skin.lovedRes else skin.loveRes
    )
  }

  private fun updateNextAndPrev(remoteViews: RemoteViews) {
    // 上下首歌曲
    remoteViews.setImageViewResource(R.id.appwidget_next, skin.nextRes)
    remoteViews.setImageViewResource(R.id.appwidget_prev, skin.prevRes)
  }

  private fun updateModel(remoteViews: RemoteViews) {
    // 播放模式
    remoteViews.setImageViewResource(R.id.appwidget_model, skin.getModeRes())
  }

  private fun updatePlayPause(remoteViews: RemoteViews) {
    //播放暂停按钮
    remoteViews.setImageViewResource(
      R.id.appwidget_toggle,
      if (playbackState.isPlaying) skin.pauseRes else skin.playRes
    )
  }

  private fun updateTitle(remoteViews: RemoteViews, song: Song) {
    // 歌曲名
    remoteViews.setTextColor(R.id.appwidget_title, skin.titleColor)
    remoteViews.setTextViewText(R.id.appwidget_title, song.title)
  }

  private fun updateArtist(remoteViews: RemoteViews, song: Song) {
    // 歌手名
    remoteViews.setTextColor(R.id.appwidget_artist, skin.artistColor)
    remoteViews.setTextViewText(R.id.appwidget_artist, song.artist)
  }

  protected fun updateBackground(remoteViews: RemoteViews) {
    remoteViews.setImageViewResource(R.id.appwidget_clickable, skin.background)
  }

  abstract fun updateWidget(
    context: Context,
    appWidgetIds: IntArray?,
    reloadCover: Boolean,
    primaryColor: Int
  )

  abstract fun partiallyUpdateWidget(context: Context, primaryColor: Int)

  companion object {

    const val EXTRA_WIDGET_NAME = "WidgetName"
    const val EXTRA_WIDGET_IDS = "WidgetIds"

    val SKIN_WHITE_1F = 1//白色不带透明
    val SKIN_TRANSPARENT = 2//透明

    private val IMAGE_SIZE_BIG = DensityUtil.dip2px(App.context, 270f)
    private val IMAGE_SIZE_MEDIUM = DensityUtil.dip2px(App.context, 72f)

  }
}
