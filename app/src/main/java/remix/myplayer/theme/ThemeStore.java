package remix.myplayer.theme;

import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.graphics.ColorUtils;

import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;

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

    public static final int THEME_PURPLE = 100;
    public static final int THEME_RED = 101;
    public static final int THEME_PINK = 102;
    public static final int THEME_BROWN = 103;
    public static final int THEME_INDIGO = 104;

    /** 当前主题颜色 */
    public static int THEME_COLOR = THEME_PINK;



    public static int STATUS_BAR_ALPHA = 112;
    public static int STATUS_BAR_COLOR = R.color.material_brown_primary_dark;
    public static int TOOLBAR_COLOR = R.color.material_brown_primary;

    /**
     * 当前是否是白天主题
     * @return
     */
    public static boolean isDay(){
        return THEME_MODE == DAY;
    }

    /**
     * 获取主题的toolbar颜色
     * @param themeColor
     * @return
     */
    @ColorRes
    public static int getThemeToolBarColor(int themeColor){
        int colorRes = -1;
        switch (themeColor){
            case THEME_PURPLE:
                colorRes =  R.color.material_purple_primary;
                break;
            case THEME_RED:
                colorRes =  R.color.material_purple_primary;
                break;
            case THEME_PINK:
                colorRes =  R.color.material_purple_primary;
                break;
            case THEME_BROWN:
                colorRes =  R.color.material_purple_primary;
                break;
            case THEME_INDIGO:
                colorRes =  R.color.material_purple_primary;
                break;
        }
        return colorRes;
    }

    /**
     * 获取主题的statusbar颜色
     * @param themeColor
     * @return
     */
    @ColorRes
    public static int getThemeStatusBarColor(int themeColor){
        int colorRes = -1;
        switch (themeColor){
            case THEME_PURPLE:
                colorRes =  R.color.material_purple_primary_dark;
                break;
            case THEME_RED:
                colorRes =  R.color.material_purple_primary_dark;
                break;
            case THEME_PINK:
                colorRes =  R.color.material_purple_primary_dark;
                break;
            case THEME_BROWN:
                colorRes =  R.color.material_purple_primary_dark;
                break;
            case THEME_INDIGO:
                colorRes =  R.color.material_purple_primary_dark;
                break;
        }
        return colorRes;
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
    public static int getBackgrounfColor1(){
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
}
