package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
  dialogState: DialogState,
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
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Text
        ),
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