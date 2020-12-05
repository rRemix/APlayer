package remix.myplayer.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.StringRes;
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
        showInternal(context,message,duration);
      } else {
        mainHandler.post(() -> {
          showInternal(context,message,duration);
        });
      }
    }
  }

  private static void showInternal(Context context, CharSequence message, int duration){
    if(context instanceof Activity){
      if(((Activity) context).isFinishing() || ((Activity) context).isDestroyed()){
        return;
      }
    }
    Toast toast = Toast.makeText(context, message, duration);
    toast.show();
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
