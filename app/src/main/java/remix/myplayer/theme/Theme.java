package remix.myplayer.theme;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;

import remix.myplayer.R;
import remix.myplayer.application.Application;
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
     */
    public static void TintDrawable(View view,Drawable drawable,@ColorInt int color){
        if(view instanceof ImageView){
            ((ImageView)view).setImageDrawable(TintDrawable(drawable,color));
        } else {
            view.setBackground(TintDrawable(drawable,color));
        }
    }

    /**
     * 着色v背景
     * @param view
     * @param res
     * @param color
     */
    public static void TintDrawable(View view, @DrawableRes int res,@ColorInt int color){
        if(view instanceof ImageView){
            ((ImageView)view).setImageDrawable(TintDrawable(Application.getContext().getResources().getDrawable(res),color));
        } else {
            view.setBackground(TintDrawable(Application.getContext().getResources().getDrawable(res),color));
        }
    }

    /**
     * * 根据当前主题生成背景色为md_color_material的圆角矩形
     * @param alpah
     * @param corner
     * @param stroke
     * @return
     */
    public static GradientDrawable getMaterialBgCorner(@FloatRange(from=0.0D, to=1.0D) float alpah, float corner, int stroke){
        return getBgCorner(alpah,corner,stroke,ThemeStore.getMaterialColorPrimaryColor());
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
     *
     * @param ignoreNight
     * @return
     */
    public static int getTheme(boolean ignoreNight){
        if (!ignoreNight && ThemeStore.THEME_MODE == ThemeStore.NIGHT) {
            return R.style.NightTheme;
        }
        switch (ThemeStore.THEME_COLOR) {
            case ThemeStore.THEME_RED:
                return R.style.DayTheme_Red;
            case ThemeStore.THEME_BROWN:
                return R.style.DayTheme_Brown;
            case ThemeStore.THEME_NAVY:
                return R.style.DayTheme_Navy;
            case ThemeStore.THEME_GREEN:
                return R.style.DayTheme_Green;
            case ThemeStore.THEME_YELLOW:
                return R.style.DayTheme_Yellow;
            case ThemeStore.THEME_PURPLE:
                return R.style.DayTheme_Purple;
            case ThemeStore.THEME_INDIGO:
                return R.style.DayTheme_Indigo;
            case ThemeStore.THEME_PLUM:
                return R.style.DayTheme_Plum;
            default:return -1;
        }
    }

    /**
     *
     * @return
     */
    @StyleRes
    public static int getTheme() {
       return getTheme(false);
    }

    /**
     * 根据当前主题获得popupmenu风格
     * @return
     */
    @StyleRes
    public static int getPopupMenuStyle(){
        return ThemeStore.isDay() ? R.style.PopupMenuDayStyle : R.style.PopupMenuNightStyle;
    }

    /**
     * 为seekbar着色
     * @param seekBar
     * @param color
     */
    public static void setTint(@NonNull SeekBar seekBar, @ColorInt int color) {
        ColorStateList s1 = ColorStateList.valueOf(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setThumbTintList(s1);
            seekBar.setProgressTintList(s1);
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            Drawable progressDrawable = DrawableCompat.wrap(seekBar.getProgressDrawable());
            seekBar.setProgressDrawable(progressDrawable);
            DrawableCompat.setTintList(progressDrawable, s1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
                DrawableCompat.setTintList(thumbDrawable, s1);
                seekBar.setThumb(thumbDrawable);
            }
        } else {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mode = PorterDuff.Mode.MULTIPLY;
            }
            if (seekBar.getIndeterminateDrawable() != null)
                seekBar.getIndeterminateDrawable().setColorFilter(color, mode);
            if (seekBar.getProgressDrawable() != null)
                seekBar.getProgressDrawable().setColorFilter(color, mode);
        }
    }

    /**
     * 修改edittext光标颜色
     * @param editText
     * @param color
     */
    public static void setTinit(EditText editText, int color,boolean underline) {
        try {
            final Field drawableResField = TextView.class.getDeclaredField("mCursorDrawableRes");
            drawableResField.setAccessible(true);
            final Drawable drawable = getDrawable(editText.getContext(), drawableResField.getInt(editText));
            if (drawable == null) {
                return;
            }
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            final Object drawableFieldOwner;
            final Class<?> drawableFieldClass;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                drawableFieldOwner = editText;
                drawableFieldClass = TextView.class;
            } else {
                final Field editorField = TextView.class.getDeclaredField("mEditor");
                editorField.setAccessible(true);
                drawableFieldOwner = editorField.get(editText);
                drawableFieldClass = drawableFieldOwner.getClass();
            }
            final Field drawableField = drawableFieldClass.getDeclaredField("mCursorDrawable");
            drawableField.setAccessible(true);
            drawableField.set(drawableFieldOwner, new Drawable[] {drawable, drawable});

            if(underline){
                editText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 修改edittext光标与下划线颜色
     * @param context
     * @param id
     * @return
     */
    public static Drawable getDrawable(Context context, int id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(id);
        } else {
            return context.getDrawable(id);
        }
    }

    public static StateListDrawable getPressAndSelectedStateListDrawalbe(Context context,@DrawableRes int resId){
        return getPressAndSelectedStateListDrawalbe(context,resId,ThemeStore.getMaterialColorPrimaryColor());
    }

    public static StateListDrawable getPressAndSelectedStateListDrawalbe(Context context,@DrawableRes int resId,@ColorInt int color){
        StateListDrawable stateListDrawable = new StateListDrawable();
        Drawable drawable1 =  Theme.TintDrawable(Theme.getDrawable(context,resId), color);
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, drawable1);
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, drawable1);
        stateListDrawable.addState(new int[]{}, Theme.getDrawable(context,resId));

        return stateListDrawable;
    }

    /**
     * 侧滑菜单点击效果
     * @param context
     * @param resId
     * @param color
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static StateListDrawable getPressAndSelectedStateListRippleDrawalbe(Context context,@DrawableRes int resId,@ColorInt int color){
        StateListDrawable stateListDrawable = new StateListDrawable();
        Drawable selectedDrawable = Theme.TintDrawable(context.getResources().getDrawable(R.drawable.bg_list_default_day),color);
        Drawable oriDrawable = context.getResources().getDrawable(resId);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(color), oriDrawable,null);
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, selectedDrawable);
            stateListDrawable.addState(new int[]{}, rippleDrawable);
            return stateListDrawable;
        } else {
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, selectedDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed},selectedDrawable);
            stateListDrawable.addState(new int[]{}, oriDrawable);
            return stateListDrawable;
        }

    }
}
