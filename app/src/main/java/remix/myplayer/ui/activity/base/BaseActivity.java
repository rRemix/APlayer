package remix.myplayer.ui.activity.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;

import remix.myplayer.R;
import remix.myplayer.misc.manager.ActivityManager;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.util.Util.sendLocalBroadcast;

/**
 * Created by Remix on 2016/3/16.
 */


@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {
    protected Context mContext;
    protected boolean mIsDestroyed;
    protected boolean mIsForeground;
    protected boolean mHasPermission;
    public static final String[] EXTERNAL_STORAGE_PERMISSIONIS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    protected <T extends View> T findView(int id) {
        return (T) findViewById(id);
    }

    /**
     * 设置主题
     */
    protected void setUpTheme() {
        if (ThemeStore.THEME_MODE == ThemeStore.NIGHT) {
            setTheme(R.style.NightTheme);
            return;
        }
        switch (ThemeStore.THEME_COLOR) {
            case ThemeStore.THEME_RED:
                setTheme(R.style.DayTheme_Red);
                break;
            case ThemeStore.THEME_BROWN:
                setTheme(R.style.DayTheme_Brown);
                break;
            case ThemeStore.THEME_NAVY:
                setTheme(R.style.DayTheme_Navy);
                break;
            case ThemeStore.THEME_GREEN:
                setTheme(R.style.DayTheme_Green);
                break;
            case ThemeStore.THEME_YELLOW:
                setTheme(R.style.DayTheme_Yellow);
                break;
            case ThemeStore.THEME_PURPLE:
                setTheme(R.style.DayTheme_Purple);
                break;
            case ThemeStore.THEME_INDIGO:
                setTheme(R.style.DayTheme_Indigo);
                break;
            case ThemeStore.THEME_PLUM:
                setTheme(R.style.DayTheme_Plum);
                break;
            case ThemeStore.THEME_BLUE:
                setTheme(R.style.DayTheme_Blue);
                break;
            case ThemeStore.THEME_WHITE:
                setTheme(R.style.DayTheme_White);
                break;
            case ThemeStore.THEME_PINK:
                setTheme(R.style.DayTheme_Pink);
                break;
            default:
                throw new IllegalArgumentException("No Available Theme");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONIS);
        //严格模式
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
//                    .detectNetwork()
//                    .detectCustomSlowCalls()
//                    .penaltyLog()
//                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .penaltyDropBox()
//                    .build());
//        }

        setUpTheme();
        super.onCreate(savedInstanceState);
        //静止横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //将该activity添加到ActivityManager,用于退出程序时关闭
        ActivityManager.AddActivity(this);
        setNavigationBarColor();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setStatusBar();
    }

    /**
     * 设置状态栏颜色
     */
    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getStatusBarColor());
    }

    /**
     * 设置导航栏颜色
     */
    protected void setNavigationBarColor() {
        //导航栏变色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.COLOR_NAVIGATION, false)) {
            final int navigationColor = ThemeStore.getNavigationBarColor();
            getWindow().setNavigationBarColor(navigationColor);
            Theme.setLightNavigationbar(this,ColorUtil.isColorLight(navigationColor));
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? super.isDestroyed() : mIsDestroyed;
    }


    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        mIsForeground = true;
        new RxPermissions(this)
                .request(EXTERNAL_STORAGE_PERMISSIONIS)
                .subscribe(has -> {
                    if (has != mHasPermission) {
                        Intent intent = new Intent(MusicService.PERMISSION_CHANGE);
                        intent.putExtra("permission", has);
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
    protected void onStop() {
        super.onStop();
    }
}
