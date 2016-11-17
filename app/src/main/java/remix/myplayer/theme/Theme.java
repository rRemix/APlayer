package remix.myplayer.theme;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
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
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;


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

    public static Drawable TintListDrawable(Drawable oriDrawable,@ColorInt int color){
        final Drawable wrappedDrawable = DrawableCompat.wrap(oriDrawable.mutate());
        DrawableCompat.setTint(oriDrawable,color);
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
     * @param alpha
     * @param corner
     * @param stroke
     * @return
     */
    public static GradientDrawable getMDCorner(@FloatRange(from=0.0D, to=1.0D) float alpha, float corner, int stroke){
        return getCorner(alpha,corner,stroke,ThemeStore.getMaterialPrimaryColor());
    }

    /**
     * 根据当前主题生成背景色为md_color_material的圆角矩形
     * @param corner
     * @return
     */
    public static GradientDrawable getMDCorner(float corner){
        return getMDCorner(1.0f,corner,0);
    }

    /**
     * 获得圆角矩形背景
     * @param alpha
     * @param corner
     * @param stroke
     * @param color
     * @return
     */
    public static GradientDrawable getCorner(@FloatRange(from=0.0D, to=1.0D)float alpha, float corner, int stroke, @ColorInt int color){
        return getShape(GradientDrawable.RECTANGLE,color,corner,stroke,color,0,0,alpha);
    }

    /**
     *
     * @param shape
     * @param corner
     * @param color
     * @param strokeSize
     * @param strokeColor
     * @param width
     * @param height
     * @return
     */
    public static GradientDrawable getShape(int shape,@ColorInt int color,float corner,int strokeSize,@ColorInt int strokeColor,int width,int height,@FloatRange(from=0.0D, to=1.0D)float alpha){
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ColorUtil.adjustAlpha(color,alpha));
        bg.setShape(shape);
        if(corner > 0)
            bg.setCornerRadius(corner);
        if(strokeSize > 0)
            bg.setStroke(strokeSize,strokeColor);

        if(width > 0 && height > 0)
            bg.setSize(width,height);

        return bg;
    }

    /**
     * 生成圆形或者矩形背景
     * @param shape
     * @param color
     * @param width
     * @param height
     * @return
     */
    public static GradientDrawable getShape(int shape,@ColorInt int color,int width,int height){
        return getShape(shape,color,0,0,color,width,height,1);
    }

    /**
     * 生成圆形或者矩形背景
     * @param shape
     * @param color
     * @return
     */
    public static GradientDrawable getShape(int shape,@ColorInt int color){
        return getShape(shape,color,0,0,color,0,0,1);
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
    public static void setTint(EditText editText, int color, boolean underline) {
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

    /**
     *
     * @param context
     * @param resId
     * @return
     */
    public static StateListDrawable getPressAndSelectedStateListDrawalbe(Context context,@DrawableRes int resId){
        return getPressAndSelectedStateListDrawalbe(context,resId,ThemeStore.getMaterialPrimaryColor());
    }

    /**
     *
     * @param context
     * @param resId
     * @param color
     * @return
     */
    public static StateListDrawable getPressAndSelectedStateListDrawalbe(Context context,@DrawableRes int resId,@ColorInt int color){
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_selected}, TintDrawable(context.getResources().getDrawable(resId), color));
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, TintDrawable(context.getResources().getDrawable(resId), color));
        stateListDrawable.addState(new int[]{}, getDrawable(context,resId));

        return stateListDrawable;
    }

    /**
     * 按下与选中触摸效果
     * @param context
     * @param selectDrawable
     * @param defaultDrawable
     * @return
     */
    public static StateListDrawable getPressAndSelectedStateListRippleDrawable(Context context,
                                                                               Drawable selectDrawable,
                                                                               Drawable defaultDrawable,
                                                                               @ColorInt int rippleColor){
        StateListDrawable stateListDrawable = new StateListDrawable();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor), defaultDrawable,null);
            stateListDrawable.addState(new int[]{android.R.attr.state_selected},selectDrawable);
            stateListDrawable.addState(new int[]{}, rippleDrawable);
            return stateListDrawable;
        } else {
            stateListDrawable.addState(new int[]{android.R.attr.state_selected},selectDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed},selectDrawable);
            stateListDrawable.addState(new int[]{}, defaultDrawable);
            return stateListDrawable;
        }
    }

    /**
     * 按下与选中触摸效果
     * @param context
     * @param selectDrawable
     * @param defaultDrawable
     * @return
     */
    public static StateListDrawable getPressAndSelectedStateListRippleDrawable(Context context,
                                                                               Drawable selectDrawable,
                                                                               Drawable defaultDrawable){
        return getPressAndSelectedStateListRippleDrawable(context,selectDrawable,defaultDrawable,ThemeStore.getRippleColor());
    }

    /**
     *
     * @param context
     * @return
     */
    public static StateListDrawable getPressAndSelectedStateListRippleDrawable(int model,Context context){
        int defaultColor = ThemeStore.isDay() ?
                ThemeStore.getBackgroundColorMain() :
                ColorUtil.getColor(model == Constants.LIST_MODEL ? R.color.night_background_color_main : R.color.night_background_color_2);

        return getPressAndSelectedStateListRippleDrawable(context,
                model == Constants.GRID_MODEL ? getCorner(1, DensityUtil.dip2px(context,2),0,ThemeStore.getSelectColor()) : getShape(GradientDrawable.RECTANGLE,ThemeStore.getSelectColor()),
                model == Constants.GRID_MODEL ? getCorner(1, DensityUtil.dip2px(context,2),0,defaultColor) : getShape(GradientDrawable.RECTANGLE,defaultColor));
    }

    /**
     *
     * @param color
     * @param contentDrawable
     * @param maskDrawable
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static RippleDrawable getRippleDrawable(@ColorInt int color, Drawable contentDrawable, Drawable maskDrawable){
        return new RippleDrawable(ColorStateList.valueOf(color),contentDrawable,maskDrawable);
    }

    /**
     * 按下触摸效果
     * @param defaultDrawable
     * @param effectDrawable
     * @param rippleColor
     * @param contentDrawable
     * @param maskDrawable
     * @return
     */
    public static Drawable getPressDrawable(Drawable defaultDrawable,Drawable effectDrawable,@ColorInt int rippleColor,Drawable contentDrawable,Drawable maskDrawable){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            return  new RippleDrawable(ColorStateList.valueOf(rippleColor),
                    contentDrawable,
                    maskDrawable);
        } else {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed},effectDrawable);
            stateListDrawable.addState(new int[]{}, defaultDrawable);
            return stateListDrawable;
        }
    }

    public static ColorStateList getPressedColorSelector(int normalColor, int pressedColor) {
        return new ColorStateList(
                new int[][]
                        {
                                new int[]{android.R.attr.state_pressed},
                                new int[]{android.R.attr.state_focused},
                                new int[]{android.R.attr.state_activated},
                                new int[]{}
                        },
                new int[]
                        {
                                pressedColor,
                                pressedColor,
                                pressedColor,
                                normalColor
                        }
        );
    }
}
