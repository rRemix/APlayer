package remix.myplayer.ui.widget.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import remix.myplayer.ui.theme.LocalTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditField(
  value: String,
  labelRes: Int,
  modifier: Modifier = Modifier,
  isError: Boolean = false,
  isLast: Boolean = false,
  maxLine: Int = 1,
  contentType: ContentType? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onDone: () -> Unit = {},
  onValueChange: (String) -> Unit,
) {
  val theme = LocalTheme.current
  OutlinedTextField(
    value = value,
    maxLines = maxLine,
    keyboardActions = KeyboardActions(onDone = {
      onDone()
    }),
    keyboardOptions = if (!isLast) KeyboardOptions(
      imeAction = ImeAction.Next,
      keyboardType = keyboardType
    ) else KeyboardOptions(
      imeAction = ImeAction.Done,
      keyboardType = keyboardType
    ),
    visualTransformation = visualTransformation,
    isError = isError,
    onValueChange = onValueChange,
    label = {
      TextPrimary(stringResource(labelRes))
    },
    colors = OutlinedTextFieldDefaults.colors(
      focusedTextColor = theme.textPrimary,
      unfocusedTextColor = theme.textPrimary,
      errorTextColor = theme.textPrimary,
      cursorColor = theme.primary,
      errorCursorColor = theme.primary,
      focusedContainerColor = Color.Transparent,
      unfocusedContainerColor = Color.Transparent,
      errorContainerColor = Color.Transparent
    ),
    modifier = Modifier
      .semantics {
        if (contentType != null) {
          this.contentType = contentType
        }
      }
      .then(modifier)
  )
}
