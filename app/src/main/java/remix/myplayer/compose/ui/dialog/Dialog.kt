package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.clickableWithoutRipple
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
  dialogState: DialogState = rememberDialogState(false),
  text: String,
  title: String? = null,
  content: String? = null,
  positive: String? = stringResource(R.string.confirm),
  neutral: String? = null, onNeutral: (() -> Unit)? = null,
  negative: String? = stringResource(R.string.cancel), onNegative: (() -> Unit)? = null,
  onDismissRequest: (() -> Unit)? = null,
  onValueChange: (String) -> Unit,
  onInput: (String) -> Unit
) {
  BaseDialog(dialogState.isOpen, onDismissRequest = {
    onDismissRequest?.invoke()
    dialogState.dismiss()
  }) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      if (title != null) {
        TextPrimary(
          title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }

      if (content != null) {
        TextPrimary(content, modifier = Modifier.padding(top = 16.dp), fontSize = 15.sp)
      }

      val theme = LocalTheme.current
      val interactionSource = remember { MutableInteractionSource() }
      BasicTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 16.dp),
        visualTransformation = VisualTransformation.None,
        interactionSource = interactionSource,
        enabled = true,
        singleLine = false,
        textStyle = TextStyle(fontSize = 15.sp)
      ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
          value = text,
          visualTransformation = VisualTransformation.None,
          innerTextField = innerTextField,
          singleLine = false,
          enabled = true,
          colors = TextFieldDefaults.colors().copy(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            cursorColor = theme.primary,
            focusedTextColor = theme.textPrimary,
            unfocusedTextColor = theme.textPrimary,
            focusedIndicatorColor = theme.primary,
            unfocusedIndicatorColor = theme.primary
          ),
          interactionSource = interactionSource,
          contentPadding = PaddingValues(0.dp)
        )
      }

      if (positive != null || neutral != null || negative != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
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
                      onInput.invoke(text)
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

//@Composable
//fun SingleChoiceDialog(
//  dialogState: DialogState = rememberDialogState(false),
//  title: String? = null,
//  items: List<String>,
//  initialPos: Int,
//  onDismissRequest: (() -> Unit)? = null,
//  onSelect: (Int) -> Unit
//) {
//  var selected by rememberSaveable {
//    mutableIntStateOf(initialPos)
//  }
//
//  BaseDialog(dialogState.isOpen, onDismissRequest = {
//    onDismissRequest?.invoke()
//    dialogState.dismiss()
//  }) {
//    Column(
//      modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
//    ) {
//      if (title != null) {
//        TextPrimary(
//          title,
//          modifier = Modifier.padding(bottom = 12.dp),
//          fontSize = 18.sp,
//          fontWeight = FontWeight.Bold
//        )
//      }
//
//      LazyColumn {
//        fun callback(index: Int) {
//          dialogState.dismiss()
//          selected = index
//          onSelect(index)
//        }
//
//        itemsIndexed(items) { index, item ->
//          Row(
//            modifier = Modifier
//              .clickableWithoutRipple {
//                callback(index)
//              },
//            verticalAlignment = Alignment.CenterVertically
//          ) {
//            RadioButton(selected = selected == index, onClick = {
//              callback(index)
//            })
//            TextPrimary(item)
//          }
//        }
//      }
//    }
//  }
//}
//
//@Composable
//fun SingleChoiceDialog(
//  dialogState: DialogState = rememberDialogState(false),
//  titleRes: Int? = null,
//  itemRes: List<Int>,
//  initialPos: Int,
//  onDismissRequest: (() -> Unit)? = null,
//  onSelect: (Int) -> Unit
//) {
//  SingleChoiceDialog(
//    dialogState = dialogState,
//    title = if (titleRes != null) stringResource(titleRes) else null,
//    items = itemRes.map { stringResource(it) },
//    initialPos = initialPos,
//    onDismissRequest = onDismissRequest,
//    onSelect = onSelect
//  )
//}

@Composable
fun NormalDialog(
  dialogState: DialogState = rememberDialogState(false),
  titleRes: Int? = null,
  titleAlignment: Alignment.Horizontal = Alignment.Start,
  contentRes: Int? = null,
  itemRes: List<Int>? = null,
  custom: @Composable (() -> Unit)? = null,
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
  dialogState: DialogState = rememberDialogState(false),
  title: String? = null,
  titleAlignment: Alignment.Horizontal = Alignment.Start,
  content: String? = null,
  items: List<String>? = null,
  custom: @Composable (() -> Unit)? = null,
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
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
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
        TextPrimary(content, modifier = Modifier.padding(top = 16.dp), fontSize = 15.sp)
      }

      if (items != null) {
        // 标题或者内容和列表的间距
        if (title != null || content != null) {
          if (itemsCallback != null) {
            Spacer(modifier = Modifier.height(32.dp))
          } else {
            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        LazyColumn {
          itemsIndexed(items) { index, item ->
            if (itemsCallback != null) {
              TextPrimary(
                item,
                fontSize = 16.sp,
                modifier = Modifier
                  .padding(bottom = 24.dp)
                  .clickableWithoutRipple {
                    dialogState.dismiss()
                    itemsCallback(index, items[index])
                  })
            } else if (itemsCallbackSingleChoice != null) {
              Row(
                modifier = Modifier
                  .clickableWithoutRipple {
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
                  .clickableWithoutRipple {
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
            .fillMaxWidth()
            .padding(top = 16.dp),
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

@Composable
internal fun BaseDialog(
  show: Boolean,
  onDismissRequest: (() -> Unit)?,
  content: @Composable () -> Unit
) {
  if (!show) {
    return
  }
  Dialog(onDismissRequest = {
    Timber.v("BaseDialog onDismissRequest")
    onDismissRequest?.invoke()
  }) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      color = LocalTheme.current.dialogBackground,
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 8.dp,
    ) {
      content()
    }
  }
}
