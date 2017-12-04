package remix.myplayer.theme;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

import java.util.ArrayList;
import java.util.List;

import remix.myplayer.APlayerApplication;
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
    public static final int DAY = 0;
    public static final int NIGHT = 1;

    /** 当前主题模式 0:白天 1:夜间 */
    public static int THEME_MODE = NIGHT;

    public static final int THEME_RED = 100;
    public static final int THEME_BROWN = 101;
    public static final int THEME_NAVY = 102;
    public static final int THEME_GREEN = 103;
    public static final int THEME_YELLOW = 104;
    public static final int THEME_PURPLE = 105;
    public static final int THEME_INDIGO = 106;
    public static final int THEME_PLUM = 107;
    public static final int THEME_BLUE = 108;
    public static final int THEME_WHITE = 109;

    /** 当前主题颜色 */
    public static int THEME_COLOR = THEME_BLUE;

    public static int STATUS_BAR_ALPHA = 150;
    public static int MATERIAL_COLOR_PRIMARY = R.color.transparent;
    public static int MATERIAL_COLOR_PRIMARY_DARK = R.color.transparent;


    /**
     * 当前是否是白天主题
     * @return
     */
    public static boolean isDay(){
        return THEME_MODE == DAY;
    }

    /**
     * 获取主题的materialPrimaryColor
     * @return
     */
    @ColorRes
    public static int getMaterialPrimaryColorRes(){
        if(THEME_MODE == NIGHT){
            return R.color.md_night_primary;
        }
        int colorRes = -1;
        switch (THEME_COLOR){
            case THEME_RED:
                colorRes =  R.color.md_red_primary;
                break;
            case THEME_BROWN:
                colorRes =  R.color.md_brown_primary;
                break;
            case THEME_NAVY:
                colorRes =  R.color.md_navy_primary;
                break;
            case THEME_GREEN:
                colorRes =  R.color.md_green_primary;
                break;
            case THEME_YELLOW:
                colorRes =  R.color.md_yellow_primary;
                break;
            case THEME_PURPLE:
                colorRes =  R.color.md_purple_primary;
                break;
            case THEME_INDIGO:
                colorRes =  R.color.md_indigo_primary;
                break;
            case THEME_PLUM:
                colorRes =  R.color.md_plum_primary;
                break;
            case THEME_BLUE:
                colorRes = R.color.md_blue_primary;
                break;
            case THEME_WHITE:
                colorRes = R.color.md_white_primary;
        }
        return colorRes;
    }

    /**
     * 获取主题的materialPrimaryDarkColor
     * @return
     */
    @ColorRes
    public static int getMaterialPrimaryDarkColorRes(){
        if(THEME_MODE == NIGHT){
            return R.color.md_night_primary_dark;
        }
        int colorRes = -1;
        switch (THEME_COLOR){
            case THEME_RED:
                colorRes =  R.color.md_red_primary_dark;
                break;
            case THEME_BROWN:
                colorRes =  R.color.md_brown_primary_dark;
                break;
            case THEME_NAVY:
                colorRes =  R.color.md_navy_primary_dark;
                break;
            case THEME_GREEN:
                colorRes =  R.color.md_green_primay_dark;
                break;
            case THEME_YELLOW:
                colorRes =  R.color.md_yellow_primay_dark;
                break;
            case THEME_PURPLE:
                colorRes =  R.color.md_purple_primary_dark;
                break;
            case THEME_INDIGO:
                colorRes =  R.color.md_indigo_primary_dark;
                break;
            case THEME_PLUM:
                colorRes =  R.color.md_plum_primary_dark;
                break;
            case THEME_BLUE:
                colorRes = R.color.md_blue_primary_dark;
                break;
            case THEME_WHITE:
                colorRes = R.color.md_white_primary_dark;
        }
        return colorRes;
    }

    public static int getThemeColor(){
        return THEME_COLOR;
    }

    /**
     * 保存当前主题颜色
     * @param themeColor
     */
    public static void saveThemeColor(int themeColor){
        SPUtil.putValue(APlayerApplication.getContext(),"Setting","ThemeColor",themeColor);
    }

    /**
     * 读取当前主题颜色
     * @return
     */
    public static int loadThemeColor(){
        return SPUtil.getValue(APlayerApplication.getContext(),"Setting","ThemeColor",ThemeStore.THEME_BLUE);
    }

    /**
     * 保存当前主题模式
     * @param mode
     */
    public static void saveThemeMode(int mode){
        SPUtil.putValue(APlayerApplication.getContext(),"Setting","ThemeMode",mode);
    }

    /**
     * 读取当前主题模式
     * @return
     */
    public static int loadThemeMode(){
        return SPUtil.getValue(APlayerApplication.getContext(),"Setting","ThemeMode",DAY);
    }

    @ColorInt
    public static int getAccentColor(){
        return ColorUtil.getColor(isDay() ? (THEME_COLOR != THEME_WHITE ? getMaterialPrimaryColorRes() : R.color.black) : R.color.purple_555393);
    }

    @ColorInt
    public static int getMaterialPrimaryColor(){
        return ColorUtil.getColor(getMaterialPrimaryColorRes());
    }

    @ColorInt
    public static int getMaterialPrimaryDarkColor(){
        return ColorUtil.getColor(getMaterialPrimaryDarkColorRes());
    }


    @ColorInt
    public static int getTextColorPrimary(){
        return ColorUtil.getColor(isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary);
    }

    @ColorInt
    public static int getTextColor(){
        return ColorUtil.getColor(isDay() ? R.color.day_textcolor : R.color.night_textcolor);
    }

    @ColorInt
    public static int getBackgroundColorMain(){
        return ColorUtil.getColor(isDay() ? R.color.day_background_color_main : R.color.night_background_color_main);
    }

    @ColorInt
    public static int getBackgroundColor1(){
        return ColorUtil.getColor(isDay() ? R.color.day_background_color_1 : R.color.night_background_color_1);
    }

    @ColorInt
    public static int getBackgroundColor2(){
        return ColorUtil.getColor(isDay() ? R.color.day_background_color_2 : R.color.night_background_color_2);
    }

    @ColorInt
    public static int getBackgroundColor3(){
        return ColorUtil.getColor(isDay() ? R.color.day_background_color_3 : R.color.night_background_color_3);
    }

    @ColorInt
    public static int getRippleColor(){
        return ColorUtil.getColor(isDay() ? R.color.day_ripple_color : R.color.night_ripple_color);
    }

    @ColorInt
    public static int getSelectColor(){
        return ColorUtil.getColor(isDay() ? R.color.day_selected_color : R.color.night_selected_color);
    }

    @ColorInt
    public static int getDividerColor(){
        return ColorUtil.getColor(isDay() ? R.color.day_list_divider : R.color.night_list_divier);
    }

    @ColorInt
    public static int getDrawerEffectColor(){
        return ColorUtil.getColor(ThemeStore.isDay() ? R.color.drawer_selected_day : R.color.drawer_selected_night);
    }

    @ColorInt
    public static int getDrawerDefaultColor(){
        return ColorUtil.getColor(ThemeStore.isDay() ? R.color.white : R.color.gray_343438);
    }

    public static com.afollestad.materialdialogs.Theme getMDDialogTheme(){
        return isDay() ? com.afollestad.materialdialogs.Theme.LIGHT : com.afollestad.materialdialogs.Theme.DARK;
    }

    public static boolean isLightTheme(){
        return isDay()&& (THEME_COLOR == THEME_WHITE);
//        return ColorUtil.isColorLight(getMaterialPrimaryColor());
    }

    /**
     * 获得所有的主题颜色
     * @return
     */
    public static List<Integer> getAllThemeColor(){
        List<Integer> themeColor = new ArrayList<>();
        themeColor.add(THEME_RED);
        themeColor.add(THEME_BROWN);
        themeColor.add(THEME_NAVY);
        themeColor.add(THEME_GREEN);
        themeColor.add(THEME_YELLOW);
        themeColor.add(THEME_PURPLE);
        themeColor.add(THEME_INDIGO);
        themeColor.add(THEME_PLUM);
        themeColor.add(THEME_BLUE);

        return themeColor;
    }

    /**
     *
     * @param theme
     * @return
     */
    @ColorInt
    public static int getThemeColorInt(int theme){
        int colorRes = -1;
        switch (theme){
            case THEME_RED:
                colorRes =  R.color.md_red_primary;
                break;
            case THEME_BROWN:
                colorRes =  R.color.md_brown_primary;
                break;
            case THEME_NAVY:
                colorRes =  R.color.md_navy_primary;
                break;
            case THEME_GREEN:
                colorRes =  R.color.md_green_primary;
                break;
            case THEME_YELLOW:
                colorRes =  R.color.md_yellow_primary;
                break;
            case THEME_PURPLE:
                colorRes =  R.color.md_purple_primary;
                break;
            case THEME_INDIGO:
                colorRes =  R.color.md_indigo_primary;
                break;
            case THEME_PLUM:
                colorRes =  R.color.md_plum_primary;
                break;
            case THEME_BLUE:
                colorRes = R.color.md_blue_primary;
                break;
            case THEME_WHITE:
                colorRes = R.color.md_white_primary;
                break;
            default:
                return Color.WHITE;
        }
        return ColorUtil.getColor(colorRes);
    }
}
