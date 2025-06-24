package remix.myplayer.compose.ui.dialog

import android.content.ClipData
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.ui.BaseDialog
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import timber.log.Timber

class ItemsCallbackMultiChoice(
  val selectedIndices: Set<Int>,
  val onCheckChange: (Int, Boolean) -> Unit
)

class ItemsCallbackSingleChoice(
  val selected: Int,
  val onSelect: (Int) -> Unit
)

typealias ItemsCallback = ((Int, String) -> Unit)

@Composable
fun NormalDialog(
  dialogState: DialogState,
  containerPadding: Dp = 20.dp,
  titleRes: Int? = null,
  titleAlignment: Alignment.Horizontal = Alignment.Start,
  contentRes: Int? = null,
  itemRes: List<Int>? = null,
  custom: @Composable (ColumnScope.() -> Unit)? = null,
  positiveRes: Int? = R.string.confirm, onPositive: (() -> Unit)? = null,
  neutralRes: Int? = null, onNeutral: (() -> Unit)? = null,
  negativeRes: Int? = R.string.cancel, onNegative: (() -> Unit)? = null,
  onDismissRequest: (() -> Unit)? = null,
  itemsCallback: ItemsCallback? = null,
  itemsCallbackSingleChoice: ItemsCallbackSingleChoice? = null,
  itemsCallbackMultiChoice: ItemsCallbackMultiChoice? = null
) {
  NormalDialog(
    dialogState,
    containerPadding = containerPadding,
    title = if (titleRes != null) stringResource(titleRes) else null,
    titleAlignment = titleAlignment,
    content = if (contentRes != null) stringResource(contentRes) else null,
    items = itemRes?.map { stringResource(it) },
    custom = custom,
    positive = if (positiveRes != null) stringResource(positiveRes) else null,
    onPositive = onPositive,
    neutral = if (neutralRes != null) stringResource(neutralRes) else null,
    onNeutral = onNeutral,
    negative = if (negativeRes != null) stringResource(negativeRes) else null,
    onNegative = onNegative,
    onDismissRequest = onDismissRequest,
    itemsCallback = itemsCallback,
    itemsCallbackSingleChoice = itemsCallbackSingleChoice,
    itemsCallbackMultiChoice = itemsCallbackMultiChoice
  )
}

@Composable
fun NormalDialog(
  dialogState: DialogState,
  containerPadding: Dp = 20.dp,
  space: Dp = 16.dp,
  title: String? = null,
  titleAlignment: Alignment.Horizontal = Alignment.Start,
  content: String? = null,
  items: List<String>? = null,
  custom: @Composable (ColumnScope.() -> Unit)? = null,
  positive: String? = stringResource(R.string.confirm), onPositive: (() -> Unit)? = null,
  neutral: String? = null, onNeutral: (() -> Unit)? = null,
  negative: String? = stringResource(R.string.cancel), onNegative: (() -> Unit)? = null,
  onDismissRequest: (() -> Unit)? = null,
  itemsCallback: ItemsCallback? = null,
  itemsCallbackSingleChoice: ItemsCallbackSingleChoice? = null,
  itemsCallbackMultiChoice: ItemsCallbackMultiChoice? = null
) {
  BaseDialog(dialogState.isOpen, onDismissRequest = {
    onDismissRequest?.invoke()
    dialogState.dismiss()
  }) {
//    SubcomposeLayout(modifier = Modifier.padding(containerPadding)) { constraints ->
//      val spaceInPx = space.roundToPx()
//
//      val title: @Composable (() -> Unit)? = if (title != null) {
//        { TextPrimary(title, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
//      } else {
//        null
//      }
//      val content: @Composable (() -> Unit)? = if (content != null) {
//        { TextPrimary(content, fontSize = 15.sp) }
//      } else {
//        null
//      }
//      val columnOrCustom = if (items != null) {
//        {
//          LazyColumn {
//            itemsIndexed(items) { index, item ->
//              if (itemsCallback != null) {
//                TextPrimary(
//                  item,
//                  fontSize = 16.sp,
//                  modifier = Modifier
//                    .padding(bottom = if (index < items.size - 1) 16.dp else 0.dp)
//                    .clickableWithoutRipple {
//                      dialogState.dismiss()
//                      itemsCallback(index, items[index])
//                    })
//              } else if (itemsCallbackSingleChoice != null) {
//                Row(
//                  modifier = Modifier
//                    .fillMaxWidth()
//                    .clickWithRipple(false) {
//                      dialogState.dismiss()
//                      itemsCallbackSingleChoice.onSelect(index)
//                    },
//                  verticalAlignment = Alignment.CenterVertically
//                ) {
//                  RadioButton(selected = itemsCallbackSingleChoice.selected == index, onClick = {
//                    dialogState.dismiss()
//                    itemsCallbackSingleChoice.onSelect(index)
//                  })
//                  TextPrimary(item, fontSize = 15.sp)
//                }
//              } else if (itemsCallbackMultiChoice != null) {
//                val checked = itemsCallbackMultiChoice.selectedIndices.contains(index)
//                Row(
//                  modifier = Modifier
//                    .fillMaxWidth()
//                    .clickWithRipple(false) {
//                      itemsCallbackMultiChoice.onCheckChange.invoke(index, !checked)
//                    },
//                  verticalAlignment = Alignment.CenterVertically
//                ) {
//                  Checkbox(
//                    checked = checked,
//                    onCheckedChange = {
//                      itemsCallbackMultiChoice.onCheckChange.invoke(index, it)
//                    })
//                  TextPrimary(item, fontSize = 15.sp)
//                }
//
//              } else {
//                throw IllegalArgumentException("no available callback")
//              }
//            }
//          }
//        }
//      } else custom
//      val buttons: @Composable (() -> Unit)? = if (positive != null || neutral != null || negative != null) {
//        {
//          Row(
//            modifier = Modifier
//              .fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//          ) {
//            val buttonPadding = 6.dp
//            val buttonFontSize = 15.sp
//            val buttonWeight = FontWeight.SemiBold
//            if (neutral != null) {
//              TextPrimary(
//                neutral,
//                modifier = Modifier
//                  .clickable {
//                    dialogState.dismiss()
//                    onNeutral?.invoke()
//                  }
//                  .padding(buttonPadding),
//                fontWeight = buttonWeight,
//                fontSize = buttonFontSize)
//            }
//
//            if (positive != null || negative != null) {
//              Row(
//                modifier = Modifier.weight(1f),
//                horizontalArrangement = Arrangement.End,
//                verticalAlignment = Alignment.CenterVertically
//              ) {
//                if (negative != null) {
//                  TextPrimary(
//                    negative,
//                    modifier = Modifier
//                      .clickable {
//                        dialogState.dismiss()
//                        onNegative?.invoke()
//                      }
//                      .padding(buttonPadding),
//                    fontWeight = buttonWeight,
//                    fontSize = buttonFontSize
//                  )
//                }
//                if (positive != null) {
//                  TextPrimary(
//                    positive,
//                    modifier = Modifier
//                      .padding(start = 12.dp)
//                      .clickable {
//                        dialogState.dismiss()
//                        onPositive?.invoke()
//                      }
//                      .padding(buttonPadding),
//                    fontWeight = buttonWeight,
//                    fontSize = buttonFontSize
//                  )
//                }
//              }
//            }
//          }
//        }
//      } else {
//        null
//      }
//      val slots = listOfNotNull(
//        "title" to title,
//        "content" to content,
//        "columnOrCustom" to columnOrCustom,
//        "buttons" to buttons,
//      ).filter { it.second != null }
//
//      var remainingHeight = constraints.maxHeight
//      val measuredPlaceables = mutableListOf<Pair<String, Placeable>>()
//
//      // 先测量其他
//      slots.filter { it.first != "columnOrCustom" }.forEach {
//        val placeable = subcompose(it.first, it.second!!).first()
//          .measure(constraints.copy(minWidth = 0, minHeight = 0))
//        measuredPlaceables.add(it.first to placeable)
//        remainingHeight -= placeable.height
//      }
//
//      // 计算所有间距
//      val totalSpace = (slots.size - 1) * spaceInPx
//      remainingHeight -= totalSpace
//
//      // 测量column
//      slots.firstOrNull { it.first == "columnOrCustom" }?.let {
//        val placeable = subcompose(it.first, it.second!!).first()
//          .measure(constraints.copy(maxHeight = remainingHeight.coerceAtLeast(0)))
//        // 将column放在合适的位置(button之上)
//        val index = if (buttons != null) {
//          measuredPlaceables.indexOfFirst { it.first == "buttons" }
//        } else {
//          measuredPlaceables.size
//        }
//        measuredPlaceables.add(index, it.first to placeable)
//      }
//
//      val totalHeight = measuredPlaceables.sumOf { it.second.height } + totalSpace
//      layout(constraints.maxWidth,
//        totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
//        var yPosition = 0
//
//        measuredPlaceables.forEachIndexed { index, (name, placeable) ->
//          var x = 0
//          if (name == "title") {
//            x = if (titleAlignment == Alignment.CenterHorizontally) {
//              (constraints.maxWidth - placeable.width) / 2
//            } else {
//              0
//            }
//          }
//          placeable.placeRelative(x, yPosition)
//
//          yPosition += if (index < measuredPlaceables.size - 1) {
//            placeable.height + spaceInPx
//          } else {
//            placeable.height
//          }
//        }
//      }
//    }

    Column(
      modifier = Modifier.padding(containerPadding),
      verticalArrangement = Arrangement.spacedBy(space)
    ) {
      if (title != null) {
        TextPrimary(
          title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.align(titleAlignment)
        )
      }

      if (content != null) {
        TextPrimary(content, fontSize = 15.sp, maxLine = Int.MAX_VALUE)
      }

      if (items != null) {
        LazyColumn {
          itemsIndexed(items) { index, item ->
            if (itemsCallback != null) {
              TextPrimary(
                item,
                fontSize = 16.sp,
                modifier = Modifier
                  .padding(bottom = if (index < items.size - 1) 16.dp else 0.dp)
                  .clickableWithoutRipple {
                    dialogState.dismiss()
                    itemsCallback(index, items[index])
                  })
            } else if (itemsCallbackSingleChoice != null) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickWithRipple(false) {
                    dialogState.dismiss()
                    itemsCallbackSingleChoice.onSelect(index)
                  },
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(selected = itemsCallbackSingleChoice.selected == index, onClick = {
                  dialogState.dismiss()
                  itemsCallbackSingleChoice.onSelect(index)
                })
                TextPrimary(item, fontSize = 15.sp)
              }
            } else if (itemsCallbackMultiChoice != null) {
              val checked = itemsCallbackMultiChoice.selectedIndices.contains(index)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickWithRipple(false) {
                    itemsCallbackMultiChoice.onCheckChange.invoke(index, !checked)
                  },
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = checked,
                  onCheckedChange = {
                    itemsCallbackMultiChoice.onCheckChange.invoke(index, it)
                  })
                TextPrimary(item, fontSize = 15.sp)
              }

            } else {
              throw IllegalArgumentException("no available callback")
            }
          }
        }
      } else if (custom != null) {
        custom()
      }

      if (positive != null || neutral != null || negative != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val buttonPadding = 6.dp
          val buttonFontSize = 15.sp
          val buttonWeight = FontWeight.SemiBold
          if (neutral != null) {
            TextPrimary(
              neutral,
              modifier = Modifier
                .clickable {
                  dialogState.dismiss()
                  onNeutral?.invoke()
                }
                .padding(buttonPadding),
              fontWeight = buttonWeight,
              fontSize = buttonFontSize)
          }

          if (positive != null || negative != null) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (negative != null) {
                TextPrimary(
                  negative,
                  modifier = Modifier
                    .clickable {
                      dialogState.dismiss()
                      onNegative?.invoke()
                    }
                    .padding(buttonPadding),
                  fontWeight = buttonWeight,
                  fontSize = buttonFontSize
                )
              }
              if (positive != null) {
                TextPrimary(
                  positive,
                  modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable {
                      dialogState.dismiss()
                      onPositive?.invoke()
                    }
                    .padding(buttonPadding),
                  fontWeight = buttonWeight,
                  fontSize = buttonFontSize
                )
              }
            }
          }
        }
      }
    }
  }
}