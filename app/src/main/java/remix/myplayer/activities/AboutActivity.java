package remix.myplayer.activities;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import remix.myplayer.R;

/**
 * Created by Remix on 2016/3/26.
 */
public class AboutActivity extends ToolbarActivity {
    private Toolbar mToolBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar, getString(R.string.about));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
