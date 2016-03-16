package remix.myplayer.activities;

import android.app.Activity;

import com.umeng.analytics.MobclickAgent;

/**
 * Created by Remix on 2016/3/16.
 */
public class BaseActivity extends Activity {

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
