package remix.myplayer;

import static remix.myplayer.theme.ThemeStore.KEY_THEME;
import static remix.myplayer.theme.ThemeStore.LIGHT;
import static remix.myplayer.theme.ThemeStore.NAME;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.UserStrategy;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.plugins.RxJavaPlugins;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
import remix.myplayer.helper.LanguageHelper;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.Util;
import timber.log.Timber;

/**
 * Created by Remix on 16-3-16.
 */

public class App extends MultiDexApplication implements ActivityLifecycleCallbacks {

  private static App mContext;

  private int mForegroundActivityCount = 0;
  //是否是googlePlay版本
  public static boolean IS_GOOGLEPLAY;

  @Override
  protected void attachBaseContext(Context base) {
    LanguageHelper.saveSystemCurrentLanguage();
    super.attachBaseContext(LanguageHelper.setLocal(base));
    MultiDex.install(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mContext = this;

    if (!BuildConfig.DEBUG) {
      IS_GOOGLEPLAY = "google".equalsIgnoreCase(Util.getAppMetaData("BUGLY_APP_CHANNEL"));
    }

    setUp();

    // AppShortcut
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      new DynamicShortcutManager(this).setUpShortcut();
    }

    // 加载第三方库
    loadLibrary();

    // 处理 RxJava2 取消订阅后，抛出的异常无法捕获，导致程序崩溃
    RxJavaPlugins.setErrorHandler(throwable -> {
      Timber.v(throwable);
      CrashReport.postCatchedException(throwable);
    });

    registerActivityLifecycleCallbacks(this);
  }

  private void setUp() {
    DiskCache.init(this, "lyric");
    LanguageHelper.setApplicationLanguage(this);

    Completable
        .fromAction(() -> {
          ThemeStore.sImmersiveMode = SPUtil
              .getValue(App.getContext(), SETTING_KEY.NAME, SETTING_KEY.IMMERSIVE_MODE, false);
          ThemeStore.sTheme = SPUtil.getValue(App.getContext(), NAME, KEY_THEME, LIGHT);
          ThemeStore.sColoredNavigation = SPUtil.getValue(App.getContext(), SETTING_KEY.NAME,
              SETTING_KEY.COLOR_NAVIGATION, false);
        })
        .subscribe();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    LanguageHelper.onConfigurationChanged(getApplicationContext());
  }


  public static App getContext() {
    return mContext;
  }

  private void loadLibrary() {
    // bugly
    Context context = getApplicationContext();
    // 获取当前包名
    String packageName = context.getPackageName();
    // 获取当前进程名
    String processName = Util.getProcessName(android.os.Process.myPid());
    // 设置是否为上报进程
    UserStrategy strategy = new UserStrategy(context);
    strategy.setUploadProcess(processName == null || processName.equals(packageName));
    CrashReport.initCrashReport(this, BuildConfig.BUGLY_APPID, BuildConfig.DEBUG, strategy);
    CrashReport.setIsDevelopmentDevice(this, BuildConfig.DEBUG);

    // fresco
    final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setBitmapMemoryCacheParamsSupplier(
            () -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE, cacheSize, Integer.MAX_VALUE,
                2 * ByteConstants.MB))
        .setBitmapsConfig(Bitmap.Config.RGB_565)
        .setDownsampleEnabled(true)
        .build();
    Fresco.initialize(this, config);

  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    Timber.v("onLowMemory");
    Completable
        .fromAction(() -> Fresco.getImagePipeline().clearMemoryCaches())
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    Timber.v("onTrimMemory, %s", level);

    Completable
        .fromAction(() -> {
          switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
              // 释放UI
              break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
              // 释放不需要资源
              Fresco.getImagePipeline().clearMemoryCaches();
              break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
              // 尽可能释放资源
              Timber.v("");
              Fresco.getImagePipeline().clearMemoryCaches();
              break;
            default:
              break;
          }
        })
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  public boolean isAppForeground() {
    return mForegroundActivityCount > 0;
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  @Override
  public void onActivityStarted(Activity activity) {
    mForegroundActivityCount++;
  }

  @Override
  public void onActivityResumed(Activity activity) {

  }

  @Override
  public void onActivityPaused(Activity activity) {

  }

  @Override
  public void onActivityStopped(Activity activity) {
    mForegroundActivityCount--;
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {

  }
}
