package remix.myplayer.theme;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
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
    /**
     * 为drawable着色
     * @param oriDrawable
     * @param color
     * @return
     */
    public static Drawable TintDrawable(Drawable oriDrawable, @ColorInt int color,@FloatRange(from=0.0D, to=1.0D) float alpha){
        final Drawable wrappedDrawable = DrawableCompat.wrap(oriDrawable.mutate());
        DrawableCompat.setTintList(wrappedDrawable,ColorStateList.valueOf(ColorUtil.adjustAlpha(color,alpha)));
        return wrappedDrawable;
    }

    /**
     * 为drawable着色
     * @param oriDrawable
     * @param color
     * @return
     */
    public static Drawable TintDrawable(Drawable oriDrawable, @ColorInt int color){
        return TintDrawable(oriDrawable,color,1.0f);
    }

    /**
     * 为drawale着色
     * @param view
     * @param color
     * @return
     */
    public static Drawable TintDrawable(View view,@ColorInt int color){
        return TintDrawable(view.getBackground(),color);
    }

    /**
     * * 根据当前主题生成背景色为md_color_material的圆角矩形
     * @param alpah
     * @param corner
     * @param stroke
     * @return
     */
    public static GradientDrawable getMaterialBgCorner(@FloatRange(from=0.0D, to=1.0D) float alpah, float corner, int stroke){
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
    public static GradientDrawable getBgCorner(@FloatRange(from=0.0D, to=1.0D)float alpha,float corner,int stroke,@ColorInt int color){
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ColorUtil.adjustAlpha(color,alpha));
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
