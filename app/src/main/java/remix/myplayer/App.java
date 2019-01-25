package remix.myplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import io.reactivex.plugins.RxJavaPlugins;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
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
  public void onCreate() {
    super.onCreate();
    mContext = getApplicationContext();

    if (!BuildConfig.DEBUG) {
      IS_GOOGLEPLAY = "google".equalsIgnoreCase(Util.getAppMetaData("BUGLY_APP_CHANNEL"));
    }

    setUp();

//    //异常捕获
//    CrashHandler.getInstance().init(this);

    //检测内存泄漏
    if (!LeakCanary.isInAnalyzerProcess(this)) {
      LeakCanary.install(this);
    }

    //AppShortcut
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      new DynamicShortcutManager(this).setUpShortcut();
    }

    //加载第三方库
    loadLibrary();

    //处理 RxJava2 取消订阅后，抛出的异常无法捕获，导致程序崩溃
    if(!BuildConfig.DEBUG){
      RxJavaPlugins.setErrorHandler(throwable -> {
        Timber.v(throwable);
        CrashReport.postCatchedException(throwable);
      });
    }

  }

  private void setUp() {
    DiskCache.init(this);
  }


  public static Context getContext() {
    return mContext;
  }

  private void loadLibrary() {
    //bugly
//        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(this);
//        strategy.setCrashHandleCallback(new CrashReport.CrashHandleCallback() {
//            public Map<String, String> onCrashHandleStart(int crashType, String errorType,
//                                                          String errorMessage, String errorStack) {
//                LinkedHashMap<String, String> map = new LinkedHashMap<>();
//                map.put("Key", "Value");
//                return map;
//            }
//
//        });
    CrashReport.initCrashReport(this, BuildConfig.BUGLY_APPID, BuildConfig.DEBUG);
    CrashReport.setIsDevelopmentDevice(this, BuildConfig.DEBUG);

    //fresco
    final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setBitmapMemoryCacheParamsSupplier(
            () -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE, cacheSize, Integer.MAX_VALUE,
                2 * ByteConstants.MB))
        .setBitmapsConfig(Bitmap.Config.RGB_565)
        .setDownsampleEnabled(true)
        .build();
    Fresco.initialize(this, config);

    //timer
    Timber.plant(new Timber.DebugTree());
  }
}
