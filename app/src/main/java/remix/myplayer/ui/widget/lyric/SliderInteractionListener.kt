package remix.myplayer.ui.widget.lyric

import android.annotation.SuppressLint
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * 监听多个slider的交互状态，当任何一个slider正在交互时回调
 * @param interactionSources slider的交互源列表
 * @param onInteractingStatusChange 交互状态变化回调，true表示有slider正在交互
 */
@SuppressLint("ComposableNaming")
@Composable
fun rememberSlidersInteractionListener(
  interactionSources: List<MutableInteractionSource>,
  onInteractingStatusChange: (Boolean) -> Unit
) {
  val allInteractions = remember { mutableStateListOf<Interaction>() }

  interactionSources.forEachIndexed { index, source ->
    LaunchedEffect(source) {
      source.interactions.collect { interaction ->
        when (interaction) {
          is PressInteraction.Press -> allInteractions.add(interaction)
          is PressInteraction.Release -> allInteractions.remove(interaction.press)
          is PressInteraction.Cancel -> allInteractions.remove(interaction.press)
          is DragInteraction.Start -> allInteractions.add(interaction)
          is DragInteraction.Stop -> allInteractions.remove(interaction.start)
          is DragInteraction.Cancel -> allInteractions.remove(interaction.start)
        }
        onInteractingStatusChange(allInteractions.isNotEmpty())
      }
    }
  }
}

/**
 * 为单个slider创建交互监听
 * @param interactionSource slider的交互源
 * @param onInteractingChange 交互状态变化回调
 */
@SuppressLint("ComposableNaming")
@Composable
fun rememberSliderInteractionListener(
  interactionSource: MutableInteractionSource,
  onInteractingChange: (Boolean) -> Unit
) {
  rememberSlidersInteractionListener(
    interactionSources = listOf(interactionSource),
    onInteractingStatusChange = onInteractingChange
  )
}