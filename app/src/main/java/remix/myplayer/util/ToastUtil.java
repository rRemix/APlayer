package remix.myplayer.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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

  private static Handler mainHandler = new Handler(Looper.getMainLooper());
  public static boolean isShow = true;

  /**
   * 长时间显示Toast
   */
  public static void showLong(Context context, CharSequence message) {
    show(context, message, Toast.LENGTH_LONG);
  }

  /**
   * 长时间显示Toast
   */
  public static void showLong(Context context, @StringRes int message) {
    show(context, context.getString(message), Toast.LENGTH_LONG);
  }

  /**
   * 自定义显示Toast时间
   */
  public static void show(Context context, CharSequence message, int duration) {
    if (isShow) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        Toast toast = Toast.makeText(context, message, duration);
//        toast.getView().setAlpha(0.8f);
        toast.show();
      } else {
        mainHandler.post(() -> {
          Toast toast = Toast.makeText(context, message, duration);
//          toast.getView().setAlpha(0.8f);
          toast.show();
        });
      }
    }
  }

  /**
   * 自定义显示Toast时间
   */
  public static void show(Context context, @StringRes int message, int duration) {
    show(context, context.getString(message), duration);
  }

  public static void show(Context context, @StringRes int message) {
    show(context, context.getString(message));
  }

  public static void show(Context context, CharSequence message) {
    show(context, message, Toast.LENGTH_SHORT);
  }

  public static void show(Context context, @StringRes int resId, Object... formatArgs) {
    show(context, context.getString(resId, formatArgs));
  }

  /**
   * 长时间显示Toast
   */
  public static void showLong(Context context, @StringRes int resId, Object... formatArgs) {
    show(context, context.getString(resId, formatArgs), Toast.LENGTH_LONG);
  }

}
