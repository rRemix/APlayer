@file:OptIn(ExperimentalGlideComposeApi::class)

package remix.myplayer.ui.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.glide.addBitmapListener
import remix.myplayer.lyric.CurrentNextLyricsLine
import remix.myplayer.lyric.LyricManager
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_COMMAND
import remix.myplayer.service.playback.MusicStateSource
import remix.myplayer.service.playback.PlaybackUiState
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.blur.StackBlurManager
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.rememberSmoothPosition
import remix.myplayer.ui.widget.lyric.LyricSingleLine
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.clickableWithoutRipple
import remix.myplayer.viewmodel.PlaybackViewModel
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Remix on 2016/3/9.
 */

/**
 * 锁屏界面
 */

@AndroidEntryPoint
class LockScreenActivity : BaseMusicActivity() {

  @Inject
  lateinit var lyricManager: LyricManager

  private val vm: PlaybackViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    addMusicServiceEventListener(vm)
    try {
      requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } catch (e: Exception) {
      Timber.v(e)
    }

    enableEdgeToEdge(
      navigationBarStyle = SystemBarStyle.auto(
        android.graphics.Color.WHITE,
        android.graphics.Color.WHITE
      )
    )

    setContent {
      val state by vm.playbackUiState.collectAsStateWithLifecycle()
      val currentLyricLine by lyricManager.currentNextLyricsLine.collectAsStateWithLifecycle()
      LockScreen(state, currentLyricLine, lyricManager)
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
  }

//  override fun onStart() {
//    super.onStart()
//    overridePendingTransition(0, 0)
//  }
//
//  override fun finish() {
  //    super.finish()
  //  }
}

@Composable
private fun LockScreen(
  playbackUiState: PlaybackUiState,
  currentLyric: CurrentNextLyricsLine,
  lyricManager: LyricManager
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val screenWidth = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
  val offsetX = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val playbackState by MusicStateSource.playbackUiState.collectAsStateWithLifecycle()
  val progressState by MusicStateSource.progressState.collectAsStateWithLifecycle()

  val smoothPosition = rememberSmoothPosition(
    position = progressState.position,
    duration = progressState.duration,
    isPlaying = playbackState.isPlaying,
    speed = playbackState.speed
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.White)
      .offset(x = with(density) { offsetX.value.toDp() })
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onHorizontalDrag = { change, amount ->
            val newOffset = offsetX.value + amount
            if (newOffset >= 0f || offsetX.value > 0f) {
              scope.launch {
                offsetX.snapTo(newOffset.coerceAtLeast(0f))
              }
            }
          },
          onDragStart = {
            scope.launch {
              offsetX.stop()
            }
          },
          onDragEnd = {
            scope.launch {
              // 判断是否超过屏幕宽度的 25%
              if (offsetX.value > screenWidth * 0.25f) {
                // 滑动距离足够，关闭 Activity
                (context as? LockScreenActivity)?.finish()
              } else {
                // 滑动距离不够，回到初始位置
                offsetX.animateTo(
                  targetValue = 0f,
                  animationSpec = tween(durationMillis = 300)
                )
              }
            }
          }
        )
      }
  ) {
    val defaultBitmap = remember {
      BitmapFactory.decodeResource(context.resources, R.drawable.album_empty_bg_night)
    }
    var blurBitmap: Bitmap? by remember {
      mutableStateOf(null)
    }
    var swatch: Palette.Swatch by remember {
      mutableStateOf(Palette.Swatch(Color.Gray.toArgb(), 100))
    }

    blurBitmap?.let {
      Image(
        modifier = Modifier
          .fillMaxSize(),
        contentScale = ContentScale.FillBounds,
        alpha = 0.75f,
        bitmap = it.asImageBitmap(),
        contentDescription = ""
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
//      Spacer(Modifier.statusBarsPadding())
      GlideImage(
        model = playbackUiState.song,
        contentDescription = "LockScreenCover",
        failure = placeholder(LocalTheme.current.albumPlaceHolder),
        loading = placeholder(LocalTheme.current.albumPlaceHolder),
        modifier = Modifier
          .background(Color.White)
          .size(210.dp)
          .padding(1.5.dp)
      ) { builder ->
        builder.addBitmapListener {
          val bitmap = it ?: defaultBitmap
          scope.launch(Dispatchers.IO) {
            blurBitmap = StackBlurManager(bitmap).processNatively(40)
            swatch = ColorUtil.getSwatch(Palette.from(bitmap).generate())
          }
        }
      }

      Text(
        playbackUiState.song.title,
        modifier = Modifier
          .padding(top = 40.dp)
          .padding(horizontal = 10.dp),
        fontSize = 20.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color(swatch.bodyTextColor)
      )

      Text(
        playbackUiState.song.artist,
        modifier = Modifier
          .padding(top = 12.dp)
          .padding(horizontal = 10.dp),
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color(swatch.titleTextColor)
      )

      Row(
        modifier = Modifier.padding(30.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Image(
          modifier = Modifier.clickableWithoutRipple {
            sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(EXTRA_COMMAND, Command.SKIP_TO_PREVIOUS)
            )
          },
          painter = painterResource(R.drawable.lock_btn_prev),
          contentDescription = "LockScreenPlay"
        )

        Image(
          modifier = Modifier.clickableWithoutRipple {
            sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(EXTRA_COMMAND, Command.PLAY_PAUSE)
            )
          },
          painter = painterResource(if (playbackUiState.isPlaying) R.drawable.lock_btn_pause else R.drawable.lock_btn_play),
          contentDescription = "LockScreenPlay"
        )

        Image(
          modifier = Modifier.clickableWithoutRipple {
            sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(EXTRA_COMMAND, Command.SKIP_TO_NEXT)
            )
          },
          painter = painterResource(R.drawable.lock_btn_next),
          contentDescription = "LockScreenPlay"
        )
      }

      Column(
        modifier = Modifier
          .padding(top = 40.dp)
          .padding(12.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val currentLine = currentLyric.currentLine
        LyricSingleLine(
          sungColor = Color(swatch.bodyTextColor),
          unSungColor = Color(swatch.bodyTextColor).copy(alpha = 0.45f),
          fontSize = 16.sp,
          useShadow = false,
          progress = if (currentLine != null) {
            LyricManager.computeLineProgress(
              line = currentLine,
              time = smoothPosition + lyricManager.offset,
              endTime = currentLyric.nextLine?.time ?: (progressState.duration + lyricManager.offset)
            )
          } else {
            null
          },
          line = currentLine
        )
        Spacer(modifier = Modifier.height(8.dp))
        val secondLine = if (!currentLine?.translation.isNullOrBlank()) {
          currentLine.translation!!
        } else {
          currentLyric.nextLine?.content ?: ""
        }
        Text(
          secondLine,
          fontSize = 16.sp,
          color = Color(swatch.bodyTextColor).copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
