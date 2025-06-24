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
fun PrimaryColorLogic() {
    val controller = LocalThemeController.current
    val state = rememberDialogState(false)

    ThemePreference(
        stringResource(R.string.primary_color),
        stringResource(R.string.primary_color_tip)
    ) {
        state.show()
    }

    var primaryColor by remember {
        mutableStateOf(controller.appTheme.primary)
    }
    ColorDialog(
        dialogState = state,
        initialColor = primaryColor,
        onColorChange = {
            primaryColor = it
        },
        onPositive =  {
            controller.setPrimary(primaryColor)
        }
    )
}