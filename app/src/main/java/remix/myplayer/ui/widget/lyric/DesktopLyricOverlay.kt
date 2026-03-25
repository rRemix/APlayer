package remix.myplayer.ui.widget.lyric

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import remix.myplayer.R
import remix.myplayer.data.prefs.DesktopLyricPrefs.Companion.ELLIPSIS
import remix.myplayer.data.prefs.DesktopLyricPrefs.Companion.HIDE_PANEL_DELAY
import remix.myplayer.lyric.CurrentNextLyricsLine
import remix.myplayer.lyric.LyricManager
import remix.myplayer.service.Command
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.ui.dialog.ColorSpace
import remix.myplayer.ui.theme.ThemeController
import remix.myplayer.ui.widget.app.rememberSmoothPosition
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.CenterInBox
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.util.ext.clickableWithoutRipple
import remix.myplayer.util.ext.isTablet

@Composable
fun DesktopLyricOverlay(
  lyricManager: LyricManager,
  themeController: ThemeController,
  onLock: () -> Unit,
  onDrag: (PointerInputChange, Float) -> Unit,
  onDragEnd: () -> Unit
) {
  val uiState by lyricManager.desktopUiState.collectAsStateWithLifecycle()

  var showPanel by remember {
    mutableStateOf(false)
  }
  var showSetting by remember {
    mutableStateOf(false)
  }
  var showSizeContainer by remember {
    mutableStateOf(true)
  }

  val desktopLyricPrefs = lyricManager.desktopLyricPrefs
  val fontSizeRange = if (LocalContext.current.isTablet()) {
    DESKTOP_LYRIC_FONT_SIZE_RANGE_LARGE_SCREEN
  } else {
    DESKTOP_LYRIC_FONT_SIZE_RANGE_DEFAULT
  }

  var firstLineSize by remember {
    mutableFloatStateOf(desktopLyricPrefs.firstLineSize)
  }
  var secondLineSize by remember {
    mutableFloatStateOf(desktopLyricPrefs.secondLineSize)
  }

  var sungColor by remember {
    val savedColor = desktopLyricPrefs.sungColor
    mutableStateOf(if (savedColor != 0) Color(savedColor) else themeController.appTheme.primary)
  }

  val unSungColor = Color(desktopLyricPrefs.unSungColor)
  val translationColor = Color(desktopLyricPrefs.translationColor)

  var interactionTrigger by remember { mutableIntStateOf(0) }
  fun markInteraction() {
    interactionTrigger++
  }

  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth()
      .background(
        colorResource(
          if (showPanel)
            R.color.desktop_lyrics_window_background
          else
            R.color.transparent
        )
      )
      .pointerInput(uiState.locked) {
        if (uiState.locked) {
          return@pointerInput
        }
        detectVerticalDragGestures(
          onVerticalDrag = { change, dragAmount ->
            onDrag(change, dragAmount)
          },
          onDragEnd = {
            onDragEnd()
          }
        )
      }
      .clickableWithoutRipple(enabled = !uiState.locked) {
        showPanel = !showPanel
        markInteraction()
      }
  ) {
    // 关闭按钮
    Image(
      modifier = Modifier
        .clickWithRipple(enabled = showPanel) {
          lyricManager.setDesktopLyricEnabled(false)
        }
        .size(dimensionResource(R.dimen.desktop_lyrics_slider_icon_size))
        .padding(dimensionResource(R.dimen.desktop_lyrics_slider_icon_padding))
        .align(Alignment.End)
        .alpha(if (showPanel) 1f else 0f),
      painter = painterResource(R.drawable.ic_close_white_24dp),
      contentDescription = "DkpClose"
    )

    DesktopLyricLines(
      lyricManager = lyricManager,
      uiState = uiState,
      firstLineSize = firstLineSize,
      secondLineSize = secondLineSize,
      sungColor = sungColor,
      unSungColor = unSungColor,
      translationColor = translationColor
    )

    if (showPanel) {
      // 控制按钮
      val buttons = resolveControls(
        uiState.playing,
        onClickLock = {
          showPanel = false
          onLock()
        },
        onClickSetting = {
          showSetting = !showSetting
          markInteraction()
        })
      Row(
        modifier = Modifier.fillMaxWidth()
      ) {
        buttons.map { action ->
          CenterInBox(
            modifier = Modifier
              .weight(1f)
          ) {
            Icon(
              modifier = Modifier
                .clickWithRipple(true) {
                  action.action()
                  markInteraction()
                }
                .size(dimensionResource(R.dimen.desktop_lyrics_control_button_size))
                .padding(dimensionResource(R.dimen.desktop_lyrics_control_button_padding)),
              painter = painterResource(action.icon),
              contentDescription = action.contentDescription,
              tint = colorResource(R.color.desktop_lyrics_control_color)
            )
          }
        }
      }

      if (showSetting) {
        // 分割线
        HorizontalDivider(
          modifier = Modifier.fillMaxWidth(),
          thickness = DividerDefaults.Thickness,
          color = colorResource(R.color.desktop_lyric_divider_color)
        )

        // 设置面板(字体+颜色)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            modifier = Modifier
              .clickWithRipple {
                showSizeContainer = !showSizeContainer
                markInteraction()
              }
              .size(dimensionResource(R.dimen.desktop_lyrics_control_button_size))
              .padding(dimensionResource(R.dimen.desktop_lyrics_control_button_padding)),
            painter = painterResource(if (showSizeContainer) R.drawable.ic_palette_24dp else R.drawable.ic_font_download_24),
            contentDescription = "DkpFontSizeAndColor",
            tint = colorResource(R.color.desktop_lyrics_control_color)
          )

          Box(modifier = Modifier.weight(1f)) {
            if (showSizeContainer) {
              FontSizeContainer(
                firstLineSize,
                secondLineSize,
                fontSizeRange,
                sungColor,
                onInteractingStatusChange = {
                  if (it) {
                    markInteraction()
                  }
                }
              ) { line, size ->
                markInteraction()
                if (line == 0) {
                  firstLineSize = size
                  desktopLyricPrefs.firstLineSize = firstLineSize
                } else {
                  secondLineSize = size
                  desktopLyricPrefs.secondLineSize = secondLineSize
                }
              }
            } else {
              FontColorContainer(sungColor, onInteractingStatusChange = {
                if (it) {
                  markInteraction()
                }
              }) { space, value ->
                markInteraction()
                sungColor = when (space) {
                  ColorSpace.Red -> {
                    sungColor.copy(red = value)
                  }

                  ColorSpace.Green -> {
                    sungColor.copy(green = value)
                  }

                  ColorSpace.Blue -> {
                    sungColor.copy(blue = value)
                  }
                }
                desktopLyricPrefs.sungColor = sungColor.toArgb()
              }
            }
          }
        }
      }
    }
  }

  // 3s后隐藏
  LaunchedEffect(interactionTrigger) {
    delay(HIDE_PANEL_DELAY)
    showPanel = false
    showSetting = false
    showSizeContainer = true
  }
}

@Composable
private fun DesktopLyricLines(
  lyricManager: LyricManager,
  uiState: DesktopLyricUiState,
  firstLineSize: Float,
  secondLineSize: Float,
  sungColor: Color,
  unSungColor: Color,
  translationColor: Color
) {
  val playbackState by MusicStateSource.playbackUiState.collectAsStateWithLifecycle()
  val progressState by MusicStateSource.progressState.collectAsStateWithLifecycle()

  val smoothPosition = rememberSmoothPosition(
    position = progressState.position,
    duration = progressState.duration,
    isPlaying = playbackState.isPlaying,
    speed = playbackState.speed
  )

  val currentLyric = uiState.currentLyricLine
  val currentLine = currentLyric.currentLine

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    LyricSingleLine(
      sungColor = sungColor,
      unSungColor = unSungColor,
      fontSize = firstLineSize.sp,
      progress = if (currentLine != null) {
        LyricManager.computeLineProgress(
          line = currentLyric.currentLine,
          time = smoothPosition + lyricManager.offset,
          endTime = currentLyric.nextLine?.time ?: (progressState.duration + lyricManager.offset)
        )
      } else {
        null
      },
      line = currentLine
    )

    val isTranslation = !currentLine?.translation.isNullOrBlank()
    Text(
      text = if (isTranslation) {
        currentLine.translation!!
      } else {
        // 翻译和下一行歌词都没有时显示省略号
        (currentLyric.nextLine?.content ?: "").ifBlank { ELLIPSIS }
      },
      style = TextStyle(
        color = if (isTranslation) translationColor else unSungColor,
        fontSize = secondLineSize.sp,
        shadow = Shadow(
          color = Color.Black,
          offset = Offset(1f, 1f),
          blurRadius = 2f
        )
      ),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      softWrap = false
    )
  }
}

@Composable
private fun resolveControls(
  playing: Boolean,
  onClickLock: () -> Unit,
  onClickSetting: () -> Unit
): List<DesktopLyricControl> = listOf(
  DesktopLyricControl(R.drawable.ic_lock_24dp, "DkpLock") {
    onClickLock()
  },
  DesktopLyricControl(R.drawable.ic_skip_previous_black_24dp, "DkpPrevious") {
    sendLocalBroadcast(makeCmdIntent(Command.SKIP_TO_PREVIOUS))
  },
  DesktopLyricControl(
    if (playing) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_black_24dp,
    "DkpPlayPause"
  ) {
    sendLocalBroadcast(makeCmdIntent(Command.PLAY_PAUSE))
  },
  DesktopLyricControl(R.drawable.ic_skip_next_black_24dp, "DkpNext") {
    sendLocalBroadcast(makeCmdIntent(Command.SKIP_TO_NEXT))
  },
  DesktopLyricControl(R.drawable.ic_settings_24dp, "DkpSettings") {
    onClickSetting()
  }
)

private class DesktopLyricControl(
  val icon: Int,
  val contentDescription: String? = null,
  val action: () -> Unit
)

@Stable
data class DesktopLyricUiState(
  val playing: Boolean,
  val locked: Boolean,
  val currentLyricLine: CurrentNextLyricsLine
)

private val DESKTOP_LYRIC_FONT_SIZE_RANGE_DEFAULT = 8f..32f
private val DESKTOP_LYRIC_FONT_SIZE_RANGE_LARGE_SCREEN = 8f..48f
