package remix.myplayer.theme;

import static remix.myplayer.theme.Theme.resolveColor;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.SPUtil;

/**
 * @ClassName
 * @Description 保存和主题相关的数据
 * @Author Xiaoborui
 * @Date 2016/8/1 09:50
 */
public class ThemeStore {

  public static final String NAME = "aplayer-theme";

  public static final String LIGHT = "light";
  public static final String DARK = "dark";
  public static final String BLACK = "black";
  public static final String KEY_THEME = "theme";
  public static final String KEY_PRIMARY_COLOR = "primary_color";
  public static final String KEY_PRIMARY_DARK_COLOR = "primary_dark_color";
  public static final String KEY_ACCENT_COLOR = "accent_color";
  public static final String KEY_FLOAT_LYRIC_TEXT_COLOR = "float_lyric_text_color";

  public static int STATUS_BAR_ALPHA = 150;

  public static boolean sColoredNavigation = false;
  public static boolean sImmersiveMode = false;
  public static String sTheme = LIGHT;


  public static void setGeneralTheme(int pos) {
    setGeneralTheme(pos == 0 ? LIGHT : pos == 1 ? DARK : BLACK);
  }

  private static void setGeneralTheme(String theme) {
    sTheme = theme;
    SPUtil.putValue(App.getContext(), NAME, KEY_THEME, theme);
  }

  public static String getThemeText() {
    switch (sTheme) {
      case LIGHT:
        return App.getContext().getString(R.string.light_theme);
      case BLACK:
        return App.getContext().getString(R.string.black_theme);
      case DARK:
        return App.getContext().getString(R.string.dark_theme);
      default:
        return App.getContext().getString(R.string.light_theme);
    }
  }

  @StyleRes
  public static int getThemeRes() {
    switch (sTheme) {
      case LIGHT:
        return R.style.Theme_APlayer;
      case BLACK:
        return R.style.Theme_APlayer_Black;
      case DARK:
        return R.style.Theme_APlayer_Dark;
      default:
        return R.style.Theme_APlayer;
    }
  }

  public static int getHighLightTextColor() {
    int primaryColor = getMaterialPrimaryColor();
    if (ColorUtil.isColorCloseToWhite(primaryColor) && isLightTheme()) {
      primaryColor = getTextColorPrimary();
    }
    if (ColorUtil.isColorCloseToBlack(primaryColor) && isBlackTheme()) {
      primaryColor = getTextColorPrimary();
    }
    return primaryColor;
  }

  public static void saveMaterialPrimaryColor(@ColorInt int color) {
    SPUtil.putValue(App.getContext(), NAME, KEY_PRIMARY_COLOR, color);
  }

  @ColorInt
  public static int getMaterialPrimaryColor() {
    //纯白需要处理下
    int primaryColor = SPUtil
        .getValue(App.getContext(), NAME, KEY_PRIMARY_COLOR, Color.parseColor("#698cf6"));
//    if (ColorUtil.isColorCloseToWhite(primaryColor)) {
//      primaryColor = ColorUtil.getColor(R.color.accent_gray_color);
//    }
    return primaryColor;

  }

  @ColorInt
  public static int getMaterialPrimaryDarkColor() {
//        return SPUtil.getValue(App.getContext(), NAME, KEY_PRIMARY_DARK_COLOR, Color.parseColor("#5c7ee4"));
    return ColorUtil.darkenColor(getMaterialPrimaryColor());
  }

  @ColorInt
  public static int getAccentColor() {
    //纯白需要处理下
    int accentColor = SPUtil
        .getValue(App.getContext(), NAME, KEY_ACCENT_COLOR, Color.parseColor("#698cf6"));
    if (ColorUtil.isColorCloseToWhite(accentColor)) {
      accentColor = ColorUtil.getColor(R.color.accent_gray_color);
    }
    return accentColor;
  }

//    @ColorInt
//    public static int getOriginalAccentColor(){
//        return SPUtil.getValue(App.getContext(), NAME, KEY_ACCENT_COLOR, Color.parseColor("#ffb61e"));
//    }

  public static void saveAccentColor(@ColorInt int color) {
    SPUtil.putValue(App.getContext(), NAME, KEY_ACCENT_COLOR, color);
  }

  @ColorInt
  public static int getNavigationBarColor() {
    return getMaterialPrimaryColor();
  }

  @ColorInt
  public static int getStatusBarColor() {
    return sImmersiveMode ? getMaterialPrimaryColor() : getMaterialPrimaryDarkColor();
  }

  @ColorInt
  public static int getTextColorPrimary() {
    return ColorUtil.getColor(
        isLightTheme() ? R.color.light_text_color_primary : R.color.dark_text_color_primary);
  }

  @ColorInt
  public static int getMaterialPrimaryColorReverse() {
    return ColorUtil.getColor(!isMDColorCloseToWhite() ? R.color.white : R.color.black);
  }

  @ColorInt
  public static int getTextColorPrimaryReverse() {
    return ColorUtil.getColor(!isMDColorCloseToWhite() ? R.color.dark_text_color_primary
        : R.color.light_text_color_primary);
  }

  @ColorInt
  public static int getTextColorSecondary() {
    return ColorUtil.getColor(
        isLightTheme() ? R.color.light_text_color_secondary : R.color.dark_text_color_secondary);
  }

  @ColorInt
  public static int getBackgroundColorMain(Context context) {
    return resolveColor(context, R.attr.background_color_main);
  }

  @ColorInt
  public static int getBackgroundColorDialog(Context context) {
    return resolveColor(context, R.attr.background_color_dialog);
  }

  @ColorInt
  public static int getRippleColor() {
    return ColorUtil
        .getColor(isLightTheme() ? R.color.light_ripple_color : R.color.dark_ripple_color);
  }

  @ColorInt
  public static int getSelectColor() {
    return ColorUtil
        .getColor(isLightTheme() ? R.color.light_select_color : R.color.dark_select_color);
  }

  @ColorInt
  public static int getDividerColor() {
    return ColorUtil
        .getColor(isLightTheme() ? R.color.light_divider_color : R.color.dark_divider_color);
  }

  @ColorInt
  public static int getPlayerBtnColor() {
    return Color.parseColor(isLightTheme() ? "#6c6a6c" : "#6b6b6b");
  }

  @ColorInt
  public static int getPlayerTitleColor() {
    return Color.parseColor(isLightTheme() ? "#333333" : "#e5e5e5");
  }

  @ColorInt
  public static int getPlayerProgressColor() {
    return Color.parseColor(isLightTheme() ? "#efeeed" : "#343438");
  }

  @ColorInt
  public static int getBottomBarBtnColor() {
    return Color.parseColor(isLightTheme() ? "#323334" : "#ffffff");
  }

  @ColorInt
  public static int getLibraryBtnColor() {
    return Color.parseColor(isLightTheme() ? "#6c6a6c" : "#ffffff");
  }

  @ColorInt
  public static int getPlayerNextSongBgColor() {
    return Color.parseColor(isLightTheme() ? "#fafafa" : "#343438");
  }

  @ColorInt
  public static int getPlayerNextSongTextColor() {
    return Color.parseColor(isLightTheme() ? "#a8a8a8" : "#e5e5e5");
  }

  @DrawableRes
  public static int getDefaultAlbumRes() {
    return isLightTheme() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night;
  }

  @DrawableRes
  public static int getDefaultArtistRes() {
    return isLightTheme() ? R.drawable.artist_empty_bg_day : R.drawable.artist_empty_bg_night;
  }

  @ColorInt
  public static int getDrawerEffectColor() {
    return ColorUtil.getColor(isLightTheme() ? R.color.drawer_effect_light :
        isBlackTheme() ? R.color.drawer_effect_black : R.color.drawer_effect_dark);
  }

  @ColorInt
  public static int getDrawerDefaultColor() {
    return ColorUtil.getColor(isLightTheme() ? R.color.drawer_default_light :
        isBlackTheme() ? R.color.drawer_default_black : R.color.drawer_default_dark);
  }

  @ColorInt
  public static int getFloatLyricTextColor() {
    final int temp = SPUtil
        .getValue(App.getContext(), NAME, KEY_FLOAT_LYRIC_TEXT_COLOR, getMaterialPrimaryColor());

    return ColorUtil.isColorCloseToWhite(temp) ? Color.parseColor("#F9F9F9") : temp;
  }

  public static void saveFloatLyricTextColor(@ColorInt int color) {
    SPUtil.putValue(App.getContext(), NAME, KEY_FLOAT_LYRIC_TEXT_COLOR, color);
  }

  public static com.afollestad.materialdialogs.Theme getMDDialogTheme() {
    return isMDColorLight() ? com.afollestad.materialdialogs.Theme.LIGHT
        : com.afollestad.materialdialogs.Theme.DARK;
  }

  public static boolean isMDColorLight() {
    return ColorUtil.isColorLight(getMaterialPrimaryColor());
  }

  public static boolean isMDColorCloseToWhite() {
    return ColorUtil.isColorCloseToWhite(getMaterialPrimaryColor());
  }

  public static boolean isLightTheme() {
    return getThemeRes() == R.style.Theme_APlayer;
  }

  public static boolean isBlackTheme() {
    return getThemeRes() == R.style.Theme_APlayer_Black;
  }

}
