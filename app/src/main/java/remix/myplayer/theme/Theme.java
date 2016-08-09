package remix.myplayer.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

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
}
