package remix.myplayer.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;


/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/9 14:55
 */
public class Theme {
    static final int[] DISABLED_STATE_SET = new int[]{-android.R.attr.state_enabled};
    static final int[] FOCUSED_STATE_SET = new int[]{android.R.attr.state_focused};
    static final int[] ACTIVATED_STATE_SET = new int[]{android.R.attr.state_activated};
    static final int[] PRESSED_STATE_SET = new int[]{android.R.attr.state_pressed};
    static final int[] CHECKED_STATE_SET = new int[]{android.R.attr.state_checked};
    static final int[] SELECTED_STATE_SET = new int[]{android.R.attr.state_selected};
    static final int[] NOT_PRESSED_OR_FOCUSED_STATE_SET = new int[]{
            -android.R.attr.state_pressed, -android.R.attr.state_focused};
    static final int[] EMPTY_STATE_SET = new int[0];
    /**
     * 为drawable着色
     * @param oriDrawable
     * @param colorStateList
     * @return
     */
    public static Drawable TintDrawable(Drawable oriDrawable, ColorStateList colorStateList){
        final Drawable wrappedDrawable = DrawableCompat.wrap(oriDrawable.mutate());
        DrawableCompat.setTintList(wrappedDrawable,colorStateList);
        return wrappedDrawable;
    }

    /**
     * 为drawale着色
     * @param view
     * @param colorStateList
     * @return
     */
    public static Drawable TintDrawable(View view,ColorStateList colorStateList){
        return TintDrawable(view.getBackground(),colorStateList);
    }

    /**
     * * 根据当前主题生成背景色为md_color_material的圆角矩形
     * @param alpah
     * @param corner
     * @param stroke
     * @return
     */
    public static GradientDrawable getMaterialBgCorner(float alpah,float corner,int stroke){
        return getBgCorner(alpah,corner,stroke,ColorUtil.getColor(ThemeStore.getMaterialPrimaryColor()));
    }

    /**
     * 根据当前主题生成背景色为md_color_material的圆角矩形
     * @param corner
     * @return
     */
    public static GradientDrawable getMaterialBgCorner(float corner){
        return getMaterialBgCorner(1.0f,corner,0);
    }

    /**
     * 获得圆角矩形背景
     * @param alpha
     * @param corner
     * @param stroke
     * @param color
     * @return
     */
    public static GradientDrawable getBgCorner(float alpha,float corner,int stroke,int color){
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ColorUtil.withAlpha(color,alpha));
        bg.setCornerRadius(corner);
        bg.setStroke(stroke,color);
        bg.setShape(GradientDrawable.RECTANGLE);
        return bg;
    }



    /**
     * 设置主题
     */
    @StyleRes
    public static int getTheme() {
        if (ThemeStore.THEME_MODE == ThemeStore.NIGHT) {
            return R.style.NightTheme;
        }
        switch (ThemeStore.THEME_COLOR) {
            case ThemeStore.THEME_PURPLE:
                return R.style.PurpleTheme;
            case ThemeStore.THEME_RED:
                return R.style.RedTheme;
            case ThemeStore.THEME_PINK:
                return R.style.PinkTheme;
            case ThemeStore.THEME_BROWN:
                return R.style.BrownTheme;
            case ThemeStore.THEME_INDIGO:
                return R.style.IngidoTheme;
        }
        return -1;
    }

}
