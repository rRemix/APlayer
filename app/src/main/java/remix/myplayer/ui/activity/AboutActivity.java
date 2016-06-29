package remix.myplayer.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import remix.myplayer.R;
import remix.myplayer.inject.ViewInject;

/**
 * Created by Remix on 2016/3/26.
 */
public class AboutActivity extends ToolbarActivity {
    @ViewInject(R.id.toolbar)
    private Toolbar mToolBar;

    @Override
    public int getLayoutId() {
        return R.layout.activity_about;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
