package remix.myplayer.compose.ui.screen.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import remix.myplayer.R
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary


@Composable
fun ArrowPreference(res: Int, onClick: () -> Unit) {
  val title = stringResource(res)
  Preference(onClick = onClick, title) {
    Icon(
      painter = painterResource(R.drawable.ic_navigate_next_white_24dp), contentDescription = title,
      tint = LocalTheme.current.secondary
    )
  }
}

@Composable
fun NormalPreference(title: String, content: String = "", onClick: () -> Unit) {
  Preference(onClick = onClick, title, content)
}

@Composable
fun ThemePreference(title: String, content: String, primary: Boolean = true, onClick: () -> Unit) {
  Preference(onClick = onClick, title, content) {
    Icon(
      painter = rememberDrawablePainter(
        ContextCompat.getDrawable(
          LocalContext.current,
          R.drawable.bg_circle_small
        )
      ),
      tint = if (primary) LocalTheme.current.primary else LocalTheme.current.secondary,
      contentDescription = "Theme_${primary}"
    )
  }
}

@Composable
fun SwitchPreference(
  title: String,
  content: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Preference(onClick = {
    onCheckedChange(!checked)
  }, title, content) {
    Switch(
      checked = checked,
      colors = SwitchDefaults.colors().copy(
        checkedTrackColor = LocalTheme.current.secondary,
        uncheckedTrackColor = Color.Transparent
      ),
      onCheckedChange = {
        onCheckedChange(it)
      })
  }
}

@Composable
fun Preference(
  onClick: () -> Unit,
  title: String,
  content: String? = null,
  trailing: (@Composable (() -> Unit))? = null
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .clickWithRipple(false) {
        onClick()
      }
      .background(color = LocalTheme.current.mainBackground, shape = RectangleShape)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically) {
    Column(
      modifier = Modifier
        .padding(end = 16.dp)
        .weight(1f),
      verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(title, modifier = Modifier.padding(bottom = 4.dp), fontSize = 16.sp)
      if (content != null) {
        TextSecondary(content, fontSize = 14.sp)
      }
    }

    trailing?.invoke()
  }
}