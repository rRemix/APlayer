package remix.myplayer.ui.screen.playing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.glide.addBitmapListener
import remix.myplayer.service.Command
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel
import timber.log.Timber

/**
 * 封面切换动画样式
 */
enum class PlayingCoverAnimationStyle(val prefValue: String) {
  /**
   * 经典样式：方向滑动 + 淡入淡出 + 轻微缩放
   */
  CLASSIC(SettingPrefs.COVER_ANIMATION_CLASSIC),

  /**
   * 视差推进：新旧封面使用不同滑动距离
   */
  PARALLAX_PUSH(SettingPrefs.COVER_ANIMATION_PARALLAX_PUSH),

  /**
   * 卡片挤压：通过水平压缩/展开
   */
  CARD_SQUEEZE(SettingPrefs.COVER_ANIMATION_CARD_SQUEEZE),

  /**
   * 翻页：沿垂直轴做左右翻页效果
   */
  PAGE_TURN(SettingPrefs.COVER_ANIMATION_PAGE_TURN),

  /**
   * 分片错峰：封面分片按错峰节奏位移切换
   */
  SLICE_STAGGER(SettingPrefs.COVER_ANIMATION_SLICE_STAGGER),

  /**
   * 溶解缩放：弱位移 + 柔和淡入淡出
   */
  DISSOLVE_ZOOM(SettingPrefs.COVER_ANIMATION_DISSOLVE_ZOOM);

  companion object {
    private val selectableStyles = listOf(
      CLASSIC,
      PARALLAX_PUSH,
      CARD_SQUEEZE,
      PAGE_TURN,
      SLICE_STAGGER,
      DISSOLVE_ZOOM
    )

    fun fromPrefValue(value: String): PlayingCoverAnimationStyle {
      return selectableStyles.firstOrNull { it.prefValue == value } ?: CLASSIC
    }
  }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun PlayingCover(modifier: Modifier, song: Song) {
  val playbackVM = playbackViewModel
  val settingState by settingViewModel.settingsState.collectAsStateWithLifecycle()
  val coverAnimationStyle = settingState.cover.coverAnimationStyle
  val coverAnimationSpeed = normalizeCoverAnimationSpeed(settingState.cover.coverAnimationSpeed)
  Timber.v("coverAnimationStyle: $coverAnimationStyle")
  val isPrevious = MusicStateSource.currentPlaybackUiState.lastOp == Command.SKIP_TO_PREVIOUS

  AnimatedContent(
    targetState = song,
    modifier = modifier,
    transitionSpec = {
      buildCoverContentTransform(
        style = coverAnimationStyle,
        isPrevious = isPrevious,
        speed = coverAnimationSpeed
      )
    },
    label = "CoverAnimation"
  ) { currentSong ->
    val imageModifier = Modifier
      .fillMaxSize()
//      .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
      .clip(RoundedCornerShape(8.dp))
    val contentModifier = when (coverAnimationStyle) {
      PlayingCoverAnimationStyle.PAGE_TURN -> {
        horizontalFlip3DModifier(
          modifier = imageModifier,
          isTargetContent = currentSong.id == song.id,
          isPrevious = isPrevious,
          speed = coverAnimationSpeed
        )
      }

      PlayingCoverAnimationStyle.SLICE_STAGGER -> {
        sliceStaggerModifier(
          modifier = imageModifier,
          isTargetContent = currentSong.id == song.id,
          isPrevious = isPrevious,
          speed = coverAnimationSpeed
        )
      }

      else -> imageModifier
    }

    GlideImage(
      model = currentSong,
      contentDescription = "PlayingCover",
      contentScale = ContentScale.Crop,
      failure = placeholder(LocalTheme.current.albumPlaceHolder),
      loading = placeholder(LocalTheme.current.albumPlaceHolder),
      modifier = contentModifier
    ) { builder ->
      builder.addBitmapListener { bitmap ->
        playbackVM.updateSwatch(bitmap)
      }
    }
  }
}
