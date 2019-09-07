package remix.myplayer;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.bugly.crashreport.CrashReport.UserStrategy;
import io.reactivex.plugins.RxJavaPlugins;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
import remix.myplayer.helper.LanguageHelper;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.util.Util;
import timber.log.Timber;

/**
 * Created by Remix on 16-3-16.
 */

public class App extends MultiDexApplication {

  private static Context mContext;

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
    mContext = getApplicationContext();

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

  }

  private void setUp() {
    DiskCache.init(this);
    LanguageHelper.setApplicationLanguage(this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    LanguageHelper.onConfigurationChanged(getApplicationContext());
  }


  public static Context getContext() {
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

    // timer
    Timber.plant(new Timber.DebugTree());
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    Timber.v("onLowMemory");
    Fresco.getImagePipeline().clearMemoryCaches();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    Timber.v("onTrimMemory");
    Fresco.getImagePipeline().clearMemoryCaches();
  }
}
