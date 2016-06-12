package remix.myplayer.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.umeng.analytics.MobclickAgent;

import remix.myplayer.R;
import remix.myplayer.managers.ActivityManager;

/**
 * Created by Remix on 2016/3/16.
 */


public class BaseActivity extends Activity {
    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //静止横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //将该activity添加到ActivityManager,用于退出程序时关闭
        ActivityManager.AddActivity(this);

//        //4.4 全透明状态栏
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//        }
//        //5.0 全透明实现
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            Window window = getWindow();
//            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            window.setStatusBarColor(Color.TRANSPARENT);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityManager.RemoveActivity(this);
    }

    @Nullable
    @Override
    public CharSequence onCreateDescription() {
        return super.onCreateDescription();
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
