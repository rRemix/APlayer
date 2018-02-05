package remix.myplayer.ui.activity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import remix.myplayer.BuildConfig;
import remix.myplayer.R;
import remix.myplayer.misc.manager.ActivityManager;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * Created by Remix on 2016/3/16.
 */


public class BaseActivity extends AppCompatActivity {
    protected Context mContext;

    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    /**
     * 设置主题
     */
    protected void setUpTheme(){
        if(ThemeStore.THEME_MODE == ThemeStore.NIGHT) {
            setTheme(R.style.NightTheme);
            return;
        }
        switch (ThemeStore.THEME_COLOR){
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
        //严格模式
        if(BuildConfig.DEBUG){
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDropBox()
                    .build());

        }

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
    protected void setNavigationBarColor(){
        //导航栏变色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SPUtil.getValue(this,"Setting", SPUtil.SPKEY.COLOR_NAVIGATION,false)) {
            getWindow().setNavigationBarColor(ThemeStore.getAccentColor());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityManager.RemoveActivity(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
