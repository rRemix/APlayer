package remix.myplayer.compose.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import remix.myplayer.compose.ui.widget.common.TextPrimary

@Composable
fun PlayingScreen() {
  Scaffold { padding ->
    Box(modifier = Modifier.padding(padding), contentAlignment = Alignment.Center) {
      TextPrimary("PlayingScreen", fontSize = 30.sp)
    }
  }
}