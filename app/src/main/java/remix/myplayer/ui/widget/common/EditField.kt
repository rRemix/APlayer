package remix.myplayer.ui.widget.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditField(
  value: String,
  labelRes: Int,
  isError: Boolean = false,
  isLast: Boolean = false,
  maxLine: Int = 1,
  modifier: Modifier = Modifier,
  contentType: ContentType? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onDone: () -> Unit = {},
  onValueChange: (String) -> Unit,
) {
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
    modifier = Modifier
      .semantics {
        if (contentType != null) {
          this.contentType = contentType
        }
      }
      .then(modifier)
  )
}