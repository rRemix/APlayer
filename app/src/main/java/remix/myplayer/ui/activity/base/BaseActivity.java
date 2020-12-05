package remix.myplayer.ui.activity.base;

import static remix.myplayer.theme.ThemeStore.sColoredNavigation;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;
import remix.myplayer.BuildConfig;
import remix.myplayer.helper.LanguageHelper;
import remix.myplayer.misc.manager.ActivityManager;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2016/3/16.
 */


@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

  protected Context mContext;
  protected boolean mIsDestroyed;
  protected boolean mIsForeground;
  protected boolean mHasPermission;
  public static final String[] EXTERNAL_STORAGE_PERMISSIONS = new String[]{
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};


  /**
   * 设置主题
   */
  protected void setUpTheme() {
    setTheme(ThemeStore.getThemeRes());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mContext = this;
    mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS);
    //严格模式
    if (BuildConfig.DEBUG) {
//      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//          .detectDiskReads()
//          .detectDiskWrites()
//          .detectNetwork()
//          .detectCustomSlowCalls()
//          .penaltyLog()
//          .build());
//      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//          .detectAll()
//          .penaltyLog()
//          .penaltyDropBox()
//          .build());
    }

    setUpTheme();
    super.onCreate(savedInstanceState);
//    //除PlayerActivity外静止横屏
//    if (!this.getClass().getSimpleName().equals(PlayerActivity.class.getSimpleName())) {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//    } else{
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//    }
    //将该activity添加到ActivityManager,用于退出程序时关闭
    ActivityManager.AddActivity(this);
    setNavigationBarColor();
  }

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
    setStatusBarColor();
    setStatusBarMode();
  }

  protected void setStatusBarMode() {
    StatusBarUtil.setStatusBarModeAuto(this);
  }

  /**
   * 设置状态栏颜色
   */
  protected void setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getStatusBarColor());
  }

  /**
   * 设置导航栏颜色
   */
  protected void setNavigationBarColor() {
    //导航栏变色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sColoredNavigation) {
      final int navigationColor = ThemeStore.getNavigationBarColor();
      getWindow().setNavigationBarColor(navigationColor);
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor));
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ActivityManager.RemoveActivity(this);
    mIsDestroyed = true;
  }

  @Override
  public boolean isDestroyed() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? super.isDestroyed()
        : mIsDestroyed;
  }


  @SuppressLint("CheckResult")
  @Override
  protected void onResume() {
    super.onResume();
    mIsForeground = true;
    new RxPermissions(this)
        .request(EXTERNAL_STORAGE_PERMISSIONS)
        .subscribe(has -> {
          if (has != mHasPermission) {
            Intent intent = new Intent(MusicService.PERMISSION_CHANGE);
            intent.putExtra(BaseMusicActivity.EXTRA_PERMISSION, has);
            sendLocalBroadcast(intent);
          }
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    mIsForeground = false;
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(LanguageHelper.setLocal(newBase));

  }

}
