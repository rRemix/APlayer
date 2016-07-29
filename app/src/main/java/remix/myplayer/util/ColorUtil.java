package remix.myplayer.util;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/28 10:53
 */
public class ColorUtil {
    /**
     * 调整颜色透明度
     * @param paramInt
     * @param paramFloat
     * @return
     */
    @ColorInt
    public static int adjustAlpha(@ColorInt int paramInt, @FloatRange(from=0.0D, to=1.0D) float paramFloat) {
        return Color.argb(Math.round(Color.alpha(paramInt) * paramFloat), Color.red(paramInt), Color.green(paramInt), Color.blue(paramInt));
    }

    public static int blendColors(int paramInt1, int paramInt2, @FloatRange(from=0.0D, to=1.0D) float paramFloat) {
        float f1 = 1.0F - paramFloat;
        float f2 = Color.alpha(paramInt1);
        float f3 = Color.alpha(paramInt2);
        float f4 = Color.red(paramInt1);
        float f5 = Color.red(paramInt2);
        float f6 = Color.green(paramInt1);
        float f7 = Color.green(paramInt2);
        float f8 = Color.blue(paramInt1);
        float f9 = Color.blue(paramInt2);
        return Color.argb((int)(f2 * f1 + f3 * paramFloat), (int)(f4 * f1 + f5 * paramFloat), (int)(f6 * f1 + f7 * paramFloat), (int)(f1 * f8 + f9 * paramFloat));
    }

    /**
     * 让颜色更加深
     * @param paramInt
     * @return
     */
    @ColorInt
    public static int darkenColor(@ColorInt int paramInt) {
        return shiftColor(paramInt, 0.9F);
    }

    /**
     * 反转颜色
     * @param paramInt
     * @return
     */
    @ColorInt
    public static int invertColor(@ColorInt int paramInt) {
        int i = Color.red(paramInt);
        int j = Color.green(paramInt);
        int k = Color.blue(paramInt);
        return Color.argb(Color.alpha(paramInt), 255 - i, 255 - j, 255 - k);
    }

    /**
     * 是否是亮色
     * @param paramInt
     * @return
     */
    public static boolean isColorLight(@ColorInt int paramInt) {
        return 1.0D - (0.299D * Color.red(paramInt) + 0.587D * Color.green(paramInt) + 0.114D * Color.blue(paramInt)) / 255.0D < 0.4D;
    }

    /**
     * 让颜色更加明亮
     * @param paramInt
     * @return
     */
    @ColorInt
    public static int lightenColor(@ColorInt int paramInt) {
        return shiftColor(paramInt, 1.1F);
    }

    /**
     * 转换颜色
     * @param paramInt
     * @param paramFloat
     * @return
     */
    @ColorInt
    public static int shiftColor(@ColorInt int paramInt, @FloatRange(from=0.0D, to=2.0D) float paramFloat) {
        if (paramFloat == 1.0F)
            return paramInt;
        int i = Color.alpha(paramInt);
        float[] arrayOfFloat = new float[3];
        Color.colorToHSV(paramInt, arrayOfFloat);
        arrayOfFloat[2] *= paramFloat;
        return (i << 24) + (Color.HSVToColor(arrayOfFloat) & 0xFFFFFF);
    }

    public static int stripAlpha(@ColorInt int paramInt) {
        return 0xFF000000 | paramInt;
    }

    /**
     * 改变颜色透明度
     * @param paramInt
     * @param paramFloat
     * @return
     */
    @ColorInt
    public static int withAlpha(@ColorInt int paramInt, @FloatRange(from=0.0D, to=1.0D) float paramFloat) {
        return (Math.min(255, Math.max(0, (int)(255.0F * paramFloat))) << 24) + (0xFFFFFF & paramInt);
    }
}
