package remix.myplayer.theme;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StyleRes;

import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * @ClassName
 * @Description 保存和主题相关的数据
 * @Author Xiaoborui
 * @Date 2016/8/1 09:50
 */
public class ThemeStore {
    private static final String NAME = "aplayer-theme";

    private static final String LIGHT = "light";
    private static final String DARK = "dark";
    private static final String BLACK = "black";
    private static final String KEY_THEME = "theme";
    private static final String KEY_PRIMARY_COLOR = "primary_color";
    private static final String KEY_PRIMARY_DARK_COLOR = "primary_dark_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";

    public static int STATUS_BAR_ALPHA = 150;

    public static boolean IMMERSIVE_MODE = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.IMMERSIVE_MODE, false);



    public static void setGeneralTheme(int pos){
        SPUtil.putValue(App.getContext(),NAME,KEY_THEME,
                pos == 0 ? LIGHT : pos == 1 ? DARK : BLACK);
    }

    public static String getThemeText(){
        String theme = SPUtil.getValue(App.getContext(), NAME, KEY_THEME, LIGHT);
        switch (theme) {
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
        String theme = SPUtil.getValue(App.getContext(), NAME, KEY_THEME, LIGHT);
        switch (theme) {
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

    public static void saveMaterialPrimaryColor(@ColorInt int color){
        SPUtil.putValue(App.getContext(),NAME,KEY_PRIMARY_COLOR,color);
        SPUtil.putValue(App.getContext(),NAME,KEY_PRIMARY_DARK_COLOR,ColorUtil.darkenColor(color));
    }

    @ColorInt
    public static int getMaterialPrimaryColor() {
        return SPUtil.getValue(App.getContext(), NAME, KEY_PRIMARY_COLOR, Color.parseColor("#698cf6"));
    }

    @ColorInt
    public static int getMaterialPrimaryDarkColor() {
        return SPUtil.getValue(App.getContext(), NAME, KEY_PRIMARY_DARK_COLOR, Color.parseColor("#5c7ee4"));
    }

    @ColorInt
    public static int getAccentColor() {
        return SPUtil.getValue(App.getContext(), NAME, KEY_ACCENT_COLOR, Color.parseColor("#ffb61e"));
    }

    public static void saveAccentColor(@ColorInt int color){
        SPUtil.putValue(App.getContext(),NAME,KEY_ACCENT_COLOR,color);
    }

    @ColorInt
    public static int getNavigationBarColor() {
        return getMaterialPrimaryColor();
    }

    @ColorInt
    public static int getStatusBarColor() {
        return IMMERSIVE_MODE ? getMaterialPrimaryColor() : getMaterialPrimaryDarkColor();
    }

    @ColorInt
    public static int getTextColorPrimary() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_text_color_primary : R.color.dark_text_color_primary);
    }

    @ColorInt
    public static int getMaterialPrimaryColorReverse(){
        return ColorUtil.getColor(!isMDColorLight() ? R.color.white : R.color.black);
    }

    @ColorInt
    public static int getTextColorprimaryReverse(){
        return ColorUtil.getColor(!isMDColorLight() ? R.color.dark_text_color_primary : R.color.light_text_color_primary);
    }

    @ColorInt
    public static int getTextColorSecondary() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_text_color_primary : R.color.dark_text_color_primary);
    }

    @ColorInt
    public static int getBackgroundColorMain() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_background_color_main : R.color.dark_background_color_main);
    }

    @ColorInt
    public static int getBackgroundColorDialog() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_background_color_dialog : R.color.dark_background_color_dialog);
    }

    @ColorInt
    public static int getRippleColor() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_ripple_color : R.color.dark_ripple_color);
    }

    @ColorInt
    public static int getSelectColor() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_select_color : R.color.dark_select_color);
    }

    @ColorInt
    public static int getDividerColor() {
        return ColorUtil.getColor(isLightTheme() ? R.color.light_divider_color : R.color.dark_divider_color);
    }

    @DrawableRes
    public static int getDefaultAlbumRes() {
        return isLightTheme() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night;
    }

    @DrawableRes
    public static int getDefaultArtistRes() {
        return isLightTheme() ? R.drawable.artist_empty_bg_day : R.drawable.artist_empty_bg_night;
    }


//    @ColorInt
//    public static int getDrawerEffectColor() {
//        return ColorUtil.getColor(ThemeStore.isLightTheme()()() ? R.color.drawer_selected_day : R.color.drawer_selected_night);
//    }
//
//    @ColorInt
//    public static int getDrawerDefaultColor() {
//        return ColorUtil.getColor(ThemeStore.isLightTheme()() ? R.color.white : R.color.gray_343438);
//    }

    public static com.afollestad.materialdialogs.Theme getMDDialogTheme() {
        return isMDColorLight() ? com.afollestad.materialdialogs.Theme.LIGHT : com.afollestad.materialdialogs.Theme.DARK;
    }

    public static boolean isMDColorLight() {
        return ColorUtil.isColorLight(getMaterialPrimaryColor());
    }

    public static boolean isLightTheme() {
        return getThemeRes() == R.style.Theme_APlayer;
    }

}
