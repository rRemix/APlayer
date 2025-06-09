package remix.myplayer.compose.ui.widget.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import remix.myplayer.compose.ui.theme.LocalTheme

@Composable
fun TextPrimary(
  text: String,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 14.sp,
  maxLine: Int = 1,
  overflow: TextOverflow = TextOverflow.Ellipsis,
  textAlign: TextAlign? = TextAlign.Start,
  fontWeight: FontWeight = FontWeight.Normal
) {
  Text(
    text = text,
    style = TextStyle(
      lineHeight = 1.em,
      textAlign = textAlign ?: TextAlign.Unspecified,
      fontSize = fontSize,
      fontWeight = fontWeight,
      color = LocalTheme.current.textPrimary,
      platformStyle = PlatformTextStyle(
        includeFontPadding = false,
      )
    ),
    modifier = modifier,
    maxLines = maxLine,
    overflow = overflow,
  )
}

@Composable
fun TextSecondary(
  text: String,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = 12.sp,
  maxLine: Int = 1,
  overflow: TextOverflow = TextOverflow.Ellipsis,
  textAlign: TextAlign? = TextAlign.Start,
  fontWeight: FontWeight = FontWeight.Normal
) {
  Text(
    text = text,
    modifier = modifier,
    maxLines = maxLine,
    textAlign = textAlign,
    fontSize = fontSize,
    fontWeight = fontWeight,
    color = LocalTheme.current.textSecondary,
    overflow = overflow,
  )
}