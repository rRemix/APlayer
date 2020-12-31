package remix.myplayer.util;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
import static android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.drawerlayout.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.widget.StatusBarView;

/**
 * Created by Remix on 2016/7/28.
 */
public class StatusBarUtil {

  /**
   * 设置状态栏颜色
   *
   * @param activity 需要设置的 activity
   * @param color 状态栏颜色值
   */
  public static void setColor(Activity activity, int color) {
    setColor(activity, color, ThemeStore.STATUS_BAR_ALPHA);
  }

  /**
   * 设置状态栏颜色
   *
   * @param activity 需要设置的activity
   * @param color 状态栏颜色值
   * @param statusBarAlpha 状态栏透明度
   */
  public static void setColor(Activity activity, int color, int statusBarAlpha) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }
    //非miui 非魅族 非6.0以上 需要改变颜色
    if (!Build.MANUFACTURER.equalsIgnoreCase("Meizu") && !Build.MANUFACTURER
        .equalsIgnoreCase("Xiaomi") &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (ThemeStore.isMDColorLight()) {
        color = ColorUtil.getColor(R.color.accent_gray_color);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setColorForLollipop(activity, color, statusBarAlpha);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      setColorForKitkat(activity, color, statusBarAlpha);
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private static void setColorForLollipop(Activity activity, int color, int statusBarAlpha) {
    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    activity.getWindow().setStatusBarColor(calculateStatusColor(color, statusBarAlpha));
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static void setColorForKitkat(Activity activity, int color, int statusBarAlpha) {
    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
    int count = decorView.getChildCount();
    if (count > 0 && decorView.getChildAt(count - 1) instanceof StatusBarView) {
      decorView.getChildAt(count - 1)
          .setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
    } else {
      StatusBarView statusView = createStatusBarView(activity, color, statusBarAlpha);
      decorView.addView(statusView);
    }
    setRootView(activity);
  }

  public static void setStatusBarMode(Activity activity, int color) {
    if (activity == null) {
      return;
    }
    boolean isDarkMode = StatusBarUtil.MeizuStatusbar.toGrey(color) >= 254;
    //获得miui版本
    String miui = "";
    int miuiVersion = 0;
    if (Build.MANUFACTURER.equals("Xiaomi")) {
      try {
        Class<?> c = Class.forName("android.os.SystemProperties");
        Method get = c.getMethod("get", String.class, String.class);
        miui = (String) (get.invoke(c, "ro.miui.ui.version.name", "unknown"));
        if (!TextUtils.isEmpty(miui) && miui.length() >= 2 && TextUtils
            .isDigitsOnly(miui.substring(1, 2))) {
          miuiVersion = Integer.valueOf(miui.substring(1, 2));
        }
      } catch (Exception e) {
      }
    }
    if (Build.MANUFACTURER.equals("Meizu")) {
      MeizuStatusbar.setStatusBarDarkIcon(activity, isDarkMode);
    } else if (Build.MANUFACTURER.equals("Xiaomi") && miuiVersion >= 6 && miuiVersion < 9) {
      XiaomiStatusbar.setStatusBarDarkMode(isDarkMode, activity);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int systemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
      if (isDarkMode) {
        systemUiVisibility |= SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      } else {
        systemUiVisibility &= ~SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      activity.getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);
    }
  }

  public static void setStatusBarModeAuto(Activity activity) {
    if (activity == null) {
      return;
    }
    setStatusBarMode(activity, getMaterialPrimaryColor());
  }

  /**
   * 设置状态栏纯色 不加半透明效果
   *
   * @param activity 需要设置的 activity
   * @param color 状态栏颜色值
   */
  public static void setColorNoTranslucent(Activity activity, int color) {
    setColor(activity, color, 0);
  }

  /**
   * 设置状态栏全透明
   *
   * @param activity 需要设置的activity
   */
  public static void setTransparent(Activity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || activity == null) {
      return;
    }
    Window window = activity.getWindow();

    //4.4 全透明状态栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
    //5.0 全透明实现
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      int systemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
      systemUiVisibility |= SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
      systemUiVisibility |= SYSTEM_UI_FLAG_LAYOUT_STABLE;
      window.getDecorView().setSystemUiVisibility(systemUiVisibility);
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      window.setStatusBarColor(Color.TRANSPARENT);
    }
  }


  /**
   * 为DrawerLayout 布局设置状态栏变色
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   * @param color 状态栏颜色值
   */
  public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout,
      int color) {
    setColorForDrawerLayout(activity, drawerLayout, color, ThemeStore.STATUS_BAR_ALPHA);
  }

  /**
   * 为DrawerLayout 布局设置状态栏颜色,纯色
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   * @param color 状态栏颜色值
   */
  public static void setColorNoTranslucentForDrawerLayout(Activity activity,
      DrawerLayout drawerLayout, int color) {
    setColorForDrawerLayout(activity, drawerLayout, color, 0);
  }

  /**
   * 为DrawerLayout 布局设置状态栏变色
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   * @param color 状态栏颜色值
   * @param statusBarAlpha 状态栏透明度
   */
  public static void setColorForDrawerLayout(Activity activity, DrawerLayout drawerLayout,
      int color, int statusBarAlpha) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }
    //非miui 非魅族 非6.0以上 需要改变颜色
    if (!Build.MANUFACTURER.equalsIgnoreCase("Meizu") && !Build.MANUFACTURER
        .equalsIgnoreCase("Xiaomi") &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (ThemeStore.isMDColorLight()) {
        color = ColorUtil.getColor(R.color.accent_gray_color);
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    } else {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
    // 生成一个状态栏大小的矩形
    // 添加 statusBarView 到布局中
    ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
    if (contentLayout.getChildCount() > 0 && contentLayout.getChildAt(0) instanceof StatusBarView) {
      contentLayout.getChildAt(0).setBackgroundColor(calculateStatusColor(color, statusBarAlpha));
    } else {
      StatusBarView statusBarView = createStatusBarView(activity, color);
      contentLayout.addView(statusBarView, 0);
    }
    // 内容布局不是 LinearLayout 时,设置padding top
    if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
      contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
    }

    // 设置属性
    ViewGroup drawer = (ViewGroup) drawerLayout.getChildAt(1);
    drawerLayout.setFitsSystemWindows(false);
    contentLayout.setFitsSystemWindows(false);
    contentLayout.setClipToPadding(true);
    drawer.setFitsSystemWindows(false);
    // 侧滑添加statusbarView
    LinearLayout headerContainer = drawer.findViewById(R.id.header);
    if (headerContainer != null) {
      headerContainer.addView(createStatusBarView(activity, color), 0);
    }

//        addTranslucentView(activity, statusBarAlpha);

  }


  /**
   * 为 DrawerLayout 布局设置状态栏透明
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   */
  public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
    setTranslucentForDrawerLayout(activity, drawerLayout, ThemeStore.STATUS_BAR_ALPHA);
  }

  /**
   * 为 DrawerLayout 布局设置状态栏透明
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   */
  public static void setTranslucentForDrawerLayout(Activity activity, DrawerLayout drawerLayout,
      int statusBarAlpha) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }
    setTransparentForDrawerLayout(activity, drawerLayout);
    addTranslucentView(activity, statusBarAlpha);
  }

  /**
   * 为 DrawerLayout 布局设置状态栏透明
   *
   * @param activity 需要设置的activity
   * @param drawerLayout DrawerLayout
   */
  public static void setTransparentForDrawerLayout(Activity activity, DrawerLayout drawerLayout) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
      return;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    } else {
      activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }

    ViewGroup contentLayout = (ViewGroup) drawerLayout.getChildAt(0);
    // 内容布局不是 LinearLayout 时,设置padding top
    if (!(contentLayout instanceof LinearLayout) && contentLayout.getChildAt(1) != null) {
      contentLayout.getChildAt(1).setPadding(0, getStatusBarHeight(activity), 0, 0);
    }

    // 设置属性
    ViewGroup drawer = (ViewGroup) drawerLayout.getChildAt(1);
    drawerLayout.setFitsSystemWindows(false);
    contentLayout.setFitsSystemWindows(false);
    contentLayout.setClipToPadding(true);
    drawer.setFitsSystemWindows(false);
  }

  /**
   * 添加半透明矩形条
   *
   * @param activity 需要设置的 activity
   * @param statusBarAlpha 透明值
   */
  private static void addTranslucentView(Activity activity, int statusBarAlpha) {
    ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
    if (contentView.getChildCount() > 1) {
      contentView.getChildAt(1).setBackgroundColor(Color.argb(statusBarAlpha, 0, 0, 0));
    } else {
      contentView.addView(createTranslucentStatusBarView(activity, statusBarAlpha));
    }
  }

  /**
   * 生成一个和状态栏大小相同的彩色矩形条
   *
   * @param activity 需要设置的 activity
   * @param color 状态栏颜色值
   * @return 状态栏矩形条
   */
  private static StatusBarView createStatusBarView(Activity activity, int color) {
    // 绘制一个和状态栏一样高的矩形
    StatusBarView statusBarView = new StatusBarView(activity);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            getStatusBarHeight(activity));
    statusBarView.setLayoutParams(params);
    statusBarView.setBackgroundColor(color);
    return statusBarView;
  }

  /**
   * 生成一个和状态栏大小相同的半透明矩形条
   *
   * @param activity 需要设置的activity
   * @param color 状态栏颜色值
   * @param alpha 透明值
   * @return 状态栏矩形条
   */
  private static StatusBarView createStatusBarView(Activity activity, int color, int alpha) {
    // 绘制一个和状态栏一样高的矩形
    StatusBarView statusBarView = new StatusBarView(activity);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            getStatusBarHeight(activity));
    statusBarView.setLayoutParams(params);
    statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
    return statusBarView;
  }

  /**
   * 设置根布局参数
   */
  private static void setRootView(Activity activity) {
    ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content))
        .getChildAt(0);
    rootView.setFitsSystemWindows(true);
    rootView.setClipToPadding(true);
  }


  /**
   * 创建半透明矩形 View
   *
   * @param alpha 透明值
   * @return 半透明 View
   */
  private static StatusBarView createTranslucentStatusBarView(Activity activity, int alpha) {
    // 绘制一个和状态栏一样高的矩形
    StatusBarView statusBarView = new StatusBarView(activity);
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            getStatusBarHeight(activity));
    statusBarView.setLayoutParams(params);
    statusBarView.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    return statusBarView;
  }

  /**
   * 获取状态栏高度
   *
   * @param context context
   * @return 状态栏高度
   */
  public static int getStatusBarHeight(Context context) {
    // 获得状态栏高度
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    return context.getResources().getDimensionPixelSize(resourceId);
  }

  /**
   * 计算状态栏颜色
   *
   * @param color color值
   * @param alpha alpha值
   * @return 最终的状态栏颜色
   */
  private static int calculateStatusColor(int color, int alpha) {
//        return color;
    float a = 1 - alpha / 255f;
    int red = color >> 16 & 0xff;
    int green = color >> 8 & 0xff;
    int blue = color & 0xff;
    red = (int) (red * a + 0.5);
    green = (int) (green * a + 0.5);
    blue = (int) (blue * a + 0.5);
    return 0xff << 24 | red << 16 | green << 8 | blue;
  }

  /**
   * 魅族状态栏工具类
   */
  public static class MeizuStatusbar {

    private static Method mSetStatusBarColorIcon;
    private static Method mSetStatusBarDarkIcon;
    private static Field mStatusBarColorFiled;

    static {
      try {
        mSetStatusBarColorIcon = Activity.class.getMethod("setStatusBarDarkIcon", int.class);
      } catch (NoSuchMethodException e) {
//        e.printStackTrace();
      }
      try {
        mSetStatusBarDarkIcon = Activity.class.getMethod("setStatusBarDarkIcon", boolean.class);
      } catch (NoSuchMethodException e) {
//        e.printStackTrace();
      }
      try {
        mStatusBarColorFiled = WindowManager.LayoutParams.class.getField("statusBarColor");
      } catch (NoSuchFieldException e) {
//        e.printStackTrace();
      }
    }

    /**
     * 判断颜色是否偏黑色
     *
     * @param color 颜色
     * @param level 级别
     */
    public static boolean isBlackColor(int color, int level) {
      int grey = toGrey(color);
      return grey < level;
    }

    /**
     * 颜色转换成灰度值
     *
     * @param rgb 颜色
     * @return　灰度值
     */
    public static int toGrey(int rgb) {
      int blue = rgb & 0x000000FF;
      int green = (rgb & 0x0000FF00) >> 8;
      int red = (rgb & 0x00FF0000) >> 16;
      return (red * 38 + green * 75 + blue * 15) >> 7;
    }

    /**
     * 设置状态栏字体图标颜色
     *
     * @param activity 当前activity
     * @param color 颜色
     */
    public static void setStatusBarDarkIcon(Activity activity, int color) {
      if (mSetStatusBarColorIcon != null) {
        try {
          mSetStatusBarColorIcon.invoke(activity, color);
        } catch (IllegalAccessException e) {
//          e.printStackTrace();
        } catch (InvocationTargetException e) {
//          e.printStackTrace();
        }
      } else {
        boolean whiteColor = isBlackColor(color, 50);
        if (mStatusBarColorFiled != null) {
          setStatusBarDarkIcon(activity, whiteColor, whiteColor);
          setStatusBarDarkIcon(activity.getWindow(), color);
        } else {
          setStatusBarDarkIcon(activity, whiteColor);
        }
      }
    }

    /**
     * 设置状态栏字体图标颜色(只限全屏非activity情况)
     *
     * @param window 当前窗口
     * @param color 颜色
     */
    public static void setStatusBarDarkIcon(Window window, int color) {
      try {
        setStatusBarColor(window, color);
        if (Build.VERSION.SDK_INT > 22) {
          setStatusBarDarkIcon(window.getDecorView(), true);
        }
      } catch (Exception e) {
//        e.printStackTrace();
      }
    }

    /**
     * 设置状态栏字体图标颜色
     *
     * @param activity 当前activity
     * @param dark 是否深色 true为深色 false 为白色
     */
    public static void setStatusBarDarkIcon(Activity activity, boolean dark) {
      setStatusBarDarkIcon(activity, dark, true);
    }

    private static boolean changeMeizuFlag(WindowManager.LayoutParams winParams, String flagName,
        boolean on) {
      try {
        Field f = winParams.getClass().getDeclaredField(flagName);
        f.setAccessible(true);
        int bits = f.getInt(winParams);
        Field f2 = winParams.getClass().getDeclaredField("meizuFlags");
        f2.setAccessible(true);
        int meizuFlags = f2.getInt(winParams);
        int oldFlags = meizuFlags;
        if (on) {
          meizuFlags |= bits;
        } else {
          meizuFlags &= ~bits;
        }
        if (oldFlags != meizuFlags) {
          f2.setInt(winParams, meizuFlags);
          return true;
        }
      } catch (NoSuchFieldException e) {
//        e.printStackTrace();
      } catch (IllegalAccessException e) {
//        e.printStackTrace();
      } catch (IllegalArgumentException e) {
//        e.printStackTrace();
      } catch (Throwable e) {
//        e.printStackTrace();
      }
      return false;
    }

    /**
     * 设置状态栏颜色
     */
    private static void setStatusBarDarkIcon(View view, boolean dark) {
      int oldVis = view.getSystemUiVisibility();
      int newVis = oldVis;
      if (dark) {
        newVis |= 0x00002000;
      } else {
        newVis &= 0x00002000;
      }
      if (newVis != oldVis) {
        view.setSystemUiVisibility(newVis);
      }
    }

    /**
     * 设置状态栏颜色
     */
    private static void setStatusBarColor(Window window, int color) {
      WindowManager.LayoutParams winParams = window.getAttributes();
      if (mStatusBarColorFiled != null) {
        try {
          int oldColor = mStatusBarColorFiled.getInt(winParams);
          if (oldColor != color) {
            mStatusBarColorFiled.set(winParams, color);
            window.setAttributes(winParams);
          }
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }

    /**
     * 设置状态栏字体图标颜色(只限全屏非activity情况)
     *
     * @param window 当前窗口
     * @param dark 是否深色 true为深色 false 为白色
     */
    public static void setStatusBarDarkIcon(Window window, boolean dark) {
      if (Build.VERSION.SDK_INT < 23) {
        changeMeizuFlag(window.getAttributes(), "MEIZU_FLAG_DARK_STATUS_BAR_ICON", dark);
      } else {
        View decorView = window.getDecorView();
        if (decorView != null) {
          setStatusBarDarkIcon(decorView, dark);
          setStatusBarColor(window, 0);
        }
      }
    }

    private static void setStatusBarDarkIcon(Activity activity, boolean dark, boolean flag) {
      if (mSetStatusBarDarkIcon != null) {
        try {
          mSetStatusBarDarkIcon.invoke(activity, dark);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      } else {
        if (flag) {
          setStatusBarDarkIcon(activity.getWindow(), dark);
        }
      }
    }

    public static void setStatusBarDarkMode(boolean darkmode, Activity activity) {
      Class<? extends Window> clazz = activity.getWindow().getClass();
      try {
        int darkModeFlag = 0;
        Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
        Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
        darkModeFlag = field.getInt(layoutParams);
        Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
        extraFlagField.invoke(activity.getWindow(), darkmode ? darkModeFlag : 0, darkModeFlag);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class XiaomiStatusbar {

    public static void setStatusBarDarkMode(boolean darkmode, Activity activity) {
      Class<? extends Window> clazz = activity.getWindow().getClass();
      try {
        int darkModeFlag = 0;
        Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
        Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
        darkModeFlag = field.getInt(layoutParams);
        Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
        extraFlagField.invoke(activity.getWindow(), darkmode ? darkModeFlag : 0, darkModeFlag);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
