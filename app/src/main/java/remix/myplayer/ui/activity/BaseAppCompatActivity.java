package remix.myplayer.ui.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;

import remix.myplayer.R;
import remix.myplayer.manager.ActivityManager;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * Created by Remix on 2016/3/16.
 */


public class BaseAppCompatActivity extends AppCompatActivity {
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
            case ThemeStore.THEME_PURPLE:
                setTheme(R.style.DayTheme_Purple);
                break;
            case ThemeStore.THEME_RED:
                setTheme(R.style.DayTheme_Red);
                break;
            case ThemeStore.THEME_PINK:
                setTheme(R.style.DayTheme_Pink);
                break;
            case ThemeStore.THEME_BROWN:
                setTheme(R.style.DayTheme_Brown);
                break;
            case ThemeStore.THEME_INDIGO:
                setTheme(R.style.DayTheme_Ingido);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUpTheme();
        super.onCreate(savedInstanceState);
        //友盟推送
        PushAgent.getInstance(this).onAppStart();
        //静止横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //将该activity添加到ActivityManager,用于退出程序时关闭
        ActivityManager.AddActivity(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setStatusBar();
    }

    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucent(this, ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
//        StatusBarUtil.setColor(this, ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
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
