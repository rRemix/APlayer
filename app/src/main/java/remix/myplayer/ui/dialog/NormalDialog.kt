package remix.myplayer.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.util.ext.clickWithRipple

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
  autoDismiss: Boolean = true,
  cancelOutside: Boolean = true,
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
  onDismiss: (() -> Unit)? = null,
  itemsCallback: ItemsCallback? = null,
  itemsCallbackSingleChoice: ItemsCallbackSingleChoice? = null,
  itemsCallbackMultiChoice: ItemsCallbackMultiChoice? = null,
  usePlatformDefaultWidth: Boolean = true,
) {
  NormalDialog(
    dialogState,
    containerPadding = containerPadding,
    autoDismiss = autoDismiss,
    cancelOutside = cancelOutside,
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
    onDismiss = onDismiss,
    itemsCallback = itemsCallback,
    itemsCallbackSingleChoice = itemsCallbackSingleChoice,
    itemsCallbackMultiChoice = itemsCallbackMultiChoice,
    usePlatformDefaultWidth = usePlatformDefaultWidth
  )
}

@Composable
fun NormalDialog(
  dialogState: DialogState,
  containerPadding: Dp = 16.dp,
  autoDismiss: Boolean = true,
  cancelOutside: Boolean = true,
  // space between title,content,items,buttons
  contentSpacer: Dp = 16.dp,
  title: String? = null,
  titleAlignment: Alignment.Horizontal = Alignment.Start,
  content: String? = null,
  items: List<String>? = null,
  custom: @Composable (ColumnScope.() -> Unit)? = null,
  positive: String? = stringResource(R.string.confirm), onPositive: (() -> Unit)? = null,
  neutral: String? = null, onNeutral: (() -> Unit)? = null,
  negative: String? = stringResource(R.string.cancel), onNegative: (() -> Unit)? = null,
  onDismissRequest: (() -> Unit)? = null,
  onDismiss: (() -> Unit)? = null,
  itemsCallback: ItemsCallback? = null,
  itemsCallbackSingleChoice: ItemsCallbackSingleChoice? = null,
  itemsCallbackMultiChoice: ItemsCallbackMultiChoice? = null,
  usePlatformDefaultWidth: Boolean = true,
) {
  BaseDialog(
    show = dialogState.isOpen,
    cancelOutside = cancelOutside,
    usePlatformDefaultWidth = usePlatformDefaultWidth,
    onDismissRequest = {
      onDismissRequest?.invoke()
      dialogState.dismiss()
    },
    onDismiss = {
      onDismiss?.invoke()
    }
  ) {
    Column(
      modifier = Modifier.padding(containerPadding),
      verticalArrangement = Arrangement.spacedBy(contentSpacer)
    ) {
      if (title != null) {
        TextPrimary(
          title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLine = Int.MAX_VALUE,
          modifier = Modifier.align(titleAlignment)
        )
      }

      if (content != null) {
        TextPrimary(content, fontSize = 15.sp, maxLine = Int.MAX_VALUE)
      }

      if (items != null) {
        LazyColumn(modifier = Modifier.weight(1f, false)) {
          itemsIndexed(items) { index, item ->
            if (itemsCallback != null) {
              TextPrimary(
                item,
                fontSize = 16.sp,
                modifier = Modifier
                  .clickWithRipple(false) {
                    if (autoDismiss) {
                      dialogState.dismiss()
                    }
                    if (index in items.indices) {
                      itemsCallback(index, items[index])
                    }
                  }
                  .fillMaxWidth()
                  .padding(vertical = 12.dp)
              )
            } else if (itemsCallbackSingleChoice != null) {
              Row(
                modifier = Modifier
                  .clickWithRipple(false) {
                    if (autoDismiss) {
                      dialogState.dismiss()
                    }
                    itemsCallbackSingleChoice.onSelect(index)
                  }
                  .fillMaxWidth()
                  .padding(horizontal = 0.dp, vertical = 12.dp),

                verticalAlignment = Alignment.CenterVertically
              ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                  RadioButton(
                    modifier = Modifier.padding(end = 8.dp),
                    selected = itemsCallbackSingleChoice.selected == index,
                    onClick = {
                      if (autoDismiss) {
                        dialogState.dismiss()
                      }
                      itemsCallbackSingleChoice.onSelect(index)
                    })
                }
                TextPrimary(item, fontSize = 15.sp)
              }
            } else if (itemsCallbackMultiChoice != null) {
              val checked = itemsCallbackMultiChoice.selectedIndices.contains(index)
              Row(
                modifier = Modifier
                  .clickWithRipple(false) {
                    itemsCallbackMultiChoice.onCheckChange(index, !checked)
                  }
                  .fillMaxWidth()
                  .padding(horizontal = 0.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                  Checkbox(
                    modifier = Modifier.padding(end = 8.dp),
                    checked = checked,
                    onCheckedChange = {
                      itemsCallbackMultiChoice.onCheckChange(index, it)
                    })
                }
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
        NormalDialogActions(
          dialogState = dialogState,
          autoDismiss = autoDismiss,
          positive = positive,
          onPositive = onPositive,
          neutral = neutral,
          onNeutral = onNeutral,
          negative = negative,
          onNegative = onNegative
        )
      }
    }
  }
}

@Composable
private fun NormalDialogActions(
  dialogState: DialogState,
  autoDismiss: Boolean,
  positive: String?,
  onPositive: (() -> Unit)?,
  neutral: String?,
  onNeutral: (() -> Unit)?,
  negative: String?,
  onNegative: (() -> Unit)?,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    NormalDialogActionButton(neutral) {
      if (autoDismiss) {
        dialogState.dismiss()
      }
      onNeutral?.invoke()
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      NormalDialogActionButton(negative) {
        if (autoDismiss) {
          dialogState.dismiss()
        }
        onNegative?.invoke()
      }
      NormalDialogActionButton(positive) {
        if (autoDismiss) {
          dialogState.dismiss()
        }
        onPositive?.invoke()
      }
    }
  }
}

@Composable
private fun NormalDialogActionButton(
  text: String?,
  onClick: () -> Unit,
) {
  if (text.isNullOrEmpty()) {
    return
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .widthIn(min = 48.dp)
      .heightIn(min = 36.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 6.dp)
  ) {
    TextPrimary(
      text,
      textAlign = TextAlign.Center,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.sp,
      maxLine = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
