package remix.myplayer.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

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

    public static int getThemeAttrColor(Context context, int attr) {
        TypedArray a = context.obtainStyledAttributes(null, new int[]{attr});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    static int getThemeAttrColor(Context context, int attr, float alpha) {
        final int color = getThemeAttrColor(context, attr);
        final int originalAlpha = Color.alpha(color);
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha));
    }


    public static ColorStateList getSwitchTrackColorStateList(Context context) {
        ColorStateList mSwitchTrackStateList = null;

        final int[][] states = new int[3][];
        final int[] colors = new int[3];
        int i = 0;

        // Disabled state
        states[i] = DISABLED_STATE_SET;

        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.1f);
        i++;

        states[i] = CHECKED_STATE_SET;
        colors[i] = getThemeAttrColor(context, android.support.v7.appcompat.R.attr.colorControlActivated, 0.3f);
        i++;

        // Default enabled state
        states[i] = EMPTY_STATE_SET;
        colors[i] = getThemeAttrColor(context, android.R.attr.colorForeground, 0.3f);
        i++;

        mSwitchTrackStateList = new ColorStateList(states, colors);

        return mSwitchTrackStateList;
    }
}
