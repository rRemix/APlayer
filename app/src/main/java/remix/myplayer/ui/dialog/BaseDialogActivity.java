package remix.myplayer.ui.dialog;

import android.os.Bundle;
import android.view.View;
import android.view.Window;

import remix.myplayer.ui.activity.BaseAppCompatActivity;

/**
 * Created by Remix on 2016/3/16.
 */


public abstract class BaseDialogActivity extends BaseAppCompatActivity {
    protected <T extends View> T findView(int id){
        return (T)findViewById(id);
    }

    @Override
    protected void setUpTheme() {
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
