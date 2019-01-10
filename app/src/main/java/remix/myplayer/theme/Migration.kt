package remix.myplayer.theme

import android.content.Context
import android.graphics.Color
import remix.myplayer.R
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.SPUtil

object Migration {
  /**
   * 迁移主题
   */
  @JvmStatic
  fun migrationTheme(context: Context) {
    //已经迁移过
    if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "migration_theme", false)) {
      return
    }
    SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, "migration_theme", true)

    //先判断是不是夜间模式
    if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "ThemeMode", 0) == 1) {
      ThemeStore.setGeneralTheme(ThemeStore.DARK)
      ThemeStore.saveAccentColor(ColorUtil.getColor(R.color.md_purple_primary))
      ThemeStore.saveMaterialPrimaryColor(ColorUtil.getColor(R.color.dark_background_color_main))
    } else {
      //读取以前的materialprimary color
      val oldThemeColor = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "ThemeColor", 108)
      val colorRes = when (oldThemeColor) {
        100 -> R.color.md_red_primary
        101 -> R.color.md_brown_primary
        102 -> R.color.md_navy_primary
        103 -> R.color.md_green_primary
        104 -> R.color.md_yellow_primary
        105 -> R.color.md_purple_primary
        106 -> R.color.md_indigo_primary
        107 -> R.color.md_plum_primary
        108 -> R.color.md_blue_primary
        109 -> R.color.md_white_primary
        110 -> R.color.md_pink_primary
        else -> R.color.md_blue_primary
      }
      val color = ColorUtil.getColor(colorRes)
      ThemeStore.setGeneralTheme(ThemeStore.LIGHT)
      //白色主题AccentColor是黑色
      ThemeStore.saveAccentColor(if (colorRes == R.color.md_white_primary) Color.BLACK else color)
      ThemeStore.saveMaterialPrimaryColor(color)
    }
  }
}