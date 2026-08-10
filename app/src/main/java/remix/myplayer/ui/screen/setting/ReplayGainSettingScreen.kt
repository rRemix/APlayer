package remix.myplayer.ui.screen.setting

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.ui.screen.setting.logic.play.ReplayGainSettingsLogic
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar

@Composable
fun ReplayGainSettingScreen() {
  Scaffold(
    topBar = {
      CommonAppBar(title = stringResource(R.string.play_replay_gain), actions = emptyList())
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    LazyColumn(modifier = Modifier.padding(contentPadding)) {
      item {
        ReplayGainSettingsLogic()
      }
    }
  }
}
