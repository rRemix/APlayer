package remix.myplayer.compose.ui.screen.setting.logic.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.ColorDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.ThemePreference
import remix.myplayer.compose.ui.theme.LocalThemeController

@Composable
fun SecondaryColorLogic() {
    val controller = LocalThemeController.current
    val state = rememberDialogState(false)

    ThemePreference(
        stringResource(R.string.accent_color),
        stringResource(R.string.accent_color_tip),
        false
    ) {
        state.show()
    }

    var secondaryColor by remember {
        mutableStateOf(controller.appTheme.secondary)
    }
    ColorDialog(
        dialogState = state,
        initialColor = secondaryColor,
        onColorChange = {
            secondaryColor = it
        },
        onPositive =  {
            controller.setSecondary(secondaryColor)
        }
    )
}