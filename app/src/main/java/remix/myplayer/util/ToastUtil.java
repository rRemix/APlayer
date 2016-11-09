package remix.myplayer.util;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/17 14:10
 */
public class ToastUtil {
    private ToastUtil() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    public static boolean isShow = true;

    /**
     * 短时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showShort(Context context, CharSequence message) {
        if (isShow)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 短时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showShort(Context context, int message) {
        if (isShow)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showLong(Context context, CharSequence message) {
        if (isShow)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 长时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void showLong(Context context, int message) {
        if (isShow)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param context
     * @param message
     * @param duration
     */
    public static void show(Context context, CharSequence message, int duration) {
        if (isShow)
            Toast.makeText(context, message, duration).show();
    }

    /**
     * 自定义显示Toast时间
     *
     * @param context
     * @param message
     * @param duration
     */
    public static void show(Context context, int message, int duration) {
        if (isShow) {
            Toast toast = Toast.makeText(context, message, duration);
            toast.getView().setAlpha(0.8f);
            toast.show();
        }
    }

    public static void show(Context context, int message) {
        if (isShow) {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.getView().setAlpha(0.8f);
            toast.show();
        }
    }

    public static void show(Context context, CharSequence message) {
        if (isShow) {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.getView().setAlpha(0.8f);
            toast.show();
        }
    }

    public static void show(Context context, @StringRes int resId, Object... formatArgs){
        if(isShow) {
            show(context, context.getString(resId, formatArgs));
        }
    }

}
