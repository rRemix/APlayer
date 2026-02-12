package remix.myplayer.ui.screen.setting.logic.playingscreen

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.playing.COVER_ANIMATION_SPEED_MAX
import remix.myplayer.ui.screen.playing.COVER_ANIMATION_SPEED_MIN
import remix.myplayer.ui.screen.playing.COVER_ANIMATION_SPEED_STEP
import remix.myplayer.ui.screen.playing.PlayingCoverAnimationStyle
import remix.myplayer.ui.screen.playing.buildCoverContentTransform
import remix.myplayer.ui.screen.playing.horizontalFlip3DModifier
import remix.myplayer.ui.screen.playing.normalizeCoverAnimationSpeed
import remix.myplayer.ui.screen.playing.sliceStaggerModifier
import remix.myplayer.ui.screen.playing.snapCoverAnimationSpeed
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.settingViewModel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class CoverAnimationOption(
  val style: PlayingCoverAnimationStyle,
  @StringRes val titleRes: Int,
  @StringRes val descRes: Int,
)

private val coverAnimationOptions = listOf(
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.CLASSIC,
    titleRes = R.string.cover_animation_classic,
    descRes = R.string.cover_animation_desc_classic
  ),
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.PARALLAX_PUSH,
    titleRes = R.string.cover_animation_parallax_push,
    descRes = R.string.cover_animation_desc_parallax_push
  ),
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.CARD_SQUEEZE,
    titleRes = R.string.cover_animation_card_squeeze,
    descRes = R.string.cover_animation_desc_card_squeeze
  ),
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.PAGE_TURN,
    titleRes = R.string.cover_animation_page_turn,
    descRes = R.string.cover_animation_desc_page_turn
  ),
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.SLICE_STAGGER,
    titleRes = R.string.cover_animation_slice_stagger,
    descRes = R.string.cover_animation_desc_slice_stagger
  ),
  CoverAnimationOption(
    style = PlayingCoverAnimationStyle.DISSOLVE_ZOOM,
    titleRes = R.string.cover_animation_dissolve_zoom,
    descRes = R.string.cover_animation_desc_dissolve_zoom
  )
)

private const val PREVIEW_LOOP_INTERVAL_MS = 1200L
private const val SPEED_VALUE_CHANGE_THRESHOLD = 0.001f
private val COVER_ANIMATION_SPEED_SLIDER_STEPS =
  ((COVER_ANIMATION_SPEED_MAX - COVER_ANIMATION_SPEED_MIN) / COVER_ANIMATION_SPEED_STEP).roundToInt() - 1

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayingCoverAnimationLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val currentStyle = settingState.cover.coverAnimationStyle
  val currentSpeed = normalizeCoverAnimationSpeed(settingState.cover.coverAnimationSpeed)
  val currentIndex =
    coverAnimationOptions.indexOfFirst { it.style == currentStyle }.coerceAtLeast(0)

  val dialogState = rememberDialogState(false)

  NormalPreference(
    title = stringResource(R.string.now_playing_cover_animation),
    content = "${stringResource(coverAnimationOptions[currentIndex].titleRes)} (${formatCoverAnimationSpeed(currentSpeed)})"
  ) {
    dialogState.show()
  }

  var selectedIndex by remember(dialogState.isOpen, currentIndex) {
    mutableIntStateOf(currentIndex)
  }
  var previewStep by remember(dialogState.isOpen) {
    mutableIntStateOf(0)
  }
  var selectedSpeed by remember(dialogState.isOpen, currentSpeed) {
    mutableFloatStateOf(currentSpeed)
  }

  LaunchedEffect(dialogState.isOpen, selectedIndex, selectedSpeed) {
    if (!dialogState.isOpen) {
      return@LaunchedEffect
    }
    while (true) {
      delay(PREVIEW_LOOP_INTERVAL_MS)
      previewStep++
    }
  }

  fun onSelect(index: Int) {
    if (selectedIndex == index) {
      return
    }
    selectedIndex = index
    previewStep++
    settingVM.setCoverAnimationStyle(coverAnimationOptions[index].style)
  }

  fun onSpeedChange(speed: Float) {
    val snapped = snapCoverAnimationSpeed(speed)
    if (abs(selectedSpeed - snapped) < SPEED_VALUE_CHANGE_THRESHOLD) {
      return
    }
    selectedSpeed = snapped
    previewStep++
    settingVM.setCoverAnimationSpeed(snapped)
  }

  NormalDialog(
    dialogState = dialogState,
    titleRes = R.string.now_playing_cover_animation,
    positiveRes = null,
    negativeRes = null,
    custom = {
      CoverAnimationPreview(
        style = coverAnimationOptions[selectedIndex].style,
        speed = selectedSpeed,
        step = previewStep
      )
      CoverAnimationSpeedSelector(
        speed = selectedSpeed,
        onValueChange = ::onSpeedChange
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        itemsIndexed(coverAnimationOptions) { index, option ->
          CoverAnimationItem(
            option = option,
            selected = selectedIndex == index,
            onClick = { onSelect(index) }
          )
        }
      }
    }
  )
}

@Composable
private fun CoverAnimationItem(
  option: CoverAnimationOption,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickWithRipple(false) { onClick() }
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
      RadioButton(
        modifier = Modifier.padding(end = 8.dp),
        selected = selected,
        onClick = onClick
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      TextPrimary(
        text = stringResource(option.titleRes),
        fontSize = 15.sp,
        maxLine = Int.MAX_VALUE
      )
      TextSecondary(
        text = stringResource(option.descRes),
        fontSize = 13.sp,
        maxLine = Int.MAX_VALUE
      )
    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CoverAnimationPreview(
  style: PlayingCoverAnimationStyle,
  speed: Float,
  step: Int,
) {
  val theme = LocalTheme.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(theme.mainBackground)
      .padding(12.dp),
    contentAlignment = Alignment.Center
  ) {
    AnimatedContent(
      targetState = step,
      transitionSpec = {
        buildCoverContentTransform(
          style = style,
          isPrevious = false,
          speed = speed
        )
      },
      label = "CoverAnimationSettingPreview"
    ) { previewState ->
      val baseModifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(10.dp))

      val contentModifier = when (style) {
        PlayingCoverAnimationStyle.PAGE_TURN -> {
          horizontalFlip3DModifier(
            modifier = baseModifier,
            isTargetContent = previewState == step,
            isPrevious = false,
            speed = speed
          )
        }

        PlayingCoverAnimationStyle.SLICE_STAGGER -> {
          sliceStaggerModifier(
            modifier = baseModifier,
            isTargetContent = previewState == step,
            isPrevious = false,
            speed = speed
          )
        }

        else -> baseModifier
      }

      Box(
        modifier = contentModifier,
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(R.drawable.cover_preview),
          contentDescription = "CoverAnimationPreviewImage",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
private fun CoverAnimationSpeedSelector(
  speed: Float,
  onValueChange: (Float) -> Unit,
) {
  val theme = LocalTheme.current

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextPrimary(
        text = stringResource(R.string.speed),
        fontSize = 14.sp
      )
      TextSecondary(
        text = formatCoverAnimationSpeed(speed),
        fontSize = 13.sp
      )
    }

    LineSlider(
      modifier = Modifier
        .fillMaxWidth()
        .height(28.dp),
      value = speed,
      onValueChange = onValueChange,
      valueRange = COVER_ANIMATION_SPEED_MIN..COVER_ANIMATION_SPEED_MAX,
      steps = COVER_ANIMATION_SPEED_SLIDER_STEPS,
      properties = defaultLineSliderProperties.copy(
        trackProgressColor = theme.primary,
        thumbColor = theme.primary
      )
    )
  }
}

private fun formatCoverAnimationSpeed(speed: Float): String {
  return String.format(Locale.getDefault(), "%.1fx", snapCoverAnimationSpeed(speed))
}
