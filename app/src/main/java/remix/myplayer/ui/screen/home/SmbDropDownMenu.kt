package remix.myplayer.ui.screen.home

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme

@Composable
internal fun SmbDropDownMenu(onShowSmbAddDialog: () -> Unit) {
  DropdownMenuItem(
    text = { Text(stringResource(R.string.smb), color = LocalTheme.current.textPrimary) },
    onClick = onShowSmbAddDialog
  )
}
