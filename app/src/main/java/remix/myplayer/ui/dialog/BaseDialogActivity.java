package remix.myplayer.ui.dialog;

import android.os.Bundle;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.BaseActivity;

/**
 * Created by Remix on 2016/3/16.
 */


public abstract class BaseDialogActivity extends BaseActivity {
    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void setUpTheme() {
        setTheme(ThemeStore.isDay() ? R.style.Dialog_DayTheme : R.style.Dialog_NightTheme);
    }

    @Override
    protected void setStatusBar() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
    }

}
