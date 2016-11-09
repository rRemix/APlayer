package remix.myplayer.ui.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import remix.myplayer.BuildConfig;
import remix.myplayer.R;
import remix.myplayer.manager.ActivityManager;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * Created by Remix on 2016/3/16.
 */


public class BaseActivity extends AppCompatActivity {
    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    /**
     * 设置主题
     */
    protected void setUpTheme(){
//        setTheme(ThemeStore.isDay() ? R.style.DayTheme : R.style.NightTheme);
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
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //严格模式
        if(BuildConfig.DEBUG){
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
//                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .penaltyFlashScreen()
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
        //友盟推送
//        PushAgent.getInstance(this).onAppStart();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setStatusBar();
    }

    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucent(this, ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
//        StatusBarUtil.setNormalColor(this, ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityManager.RemoveActivity(this);
        if(this instanceof MusicService.Callback){
            MusicService.removeCallback((MusicService.Callback) this);
        }
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
