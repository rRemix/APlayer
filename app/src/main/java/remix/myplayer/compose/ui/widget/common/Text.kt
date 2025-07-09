package remix.myplayer.compose.ui.widget.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
  textAlign: TextAlign? = TextAlign.Unspecified,
  fontWeight: FontWeight = FontWeight.Normal,
  color: Color = LocalTheme.current.textPrimary,
) {
  Text(
    text = text,
    style = TextStyle(
      lineHeight = 1.em,
      textAlign = textAlign ?: TextAlign.Unspecified,
      fontSize = fontSize,
      fontWeight = fontWeight,
      color = color,
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
  textAlign: TextAlign? = TextAlign.Unspecified,
  fontWeight: FontWeight = FontWeight.Normal
) {
  Text(
    text = text,
    modifier = modifier,
    style = TextStyle(
      lineHeight = 1.em,
      textAlign = textAlign ?: TextAlign.Unspecified,
      fontSize = fontSize,
      fontWeight = fontWeight,
    ),
    maxLines = maxLine,
    color = LocalTheme.current.textSecondary,
    overflow = overflow,
  )
}