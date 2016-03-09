package remix.myplayer.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import remix.myplayer.R;

/**
 * Created by Remix on 2016/3/9.
 */
public class LockScreenActivity extends AppCompatActivity {
    private WindowManager mWmManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attr.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
//        getWindow().addFlags( WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );

    }
}
