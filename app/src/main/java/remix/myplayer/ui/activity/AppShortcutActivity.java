package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import remix.myplayer.service.MusicService;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2017/11/1.
 */

public class AppShortcutActivity extends BaseActivity {
    public static final int SHORTCUT_TYPE_SHUFFLE_ALL = 0;
    public static final int SHORTCUT_TYPE_MY_LOVE = 1;
    public static final int SHORTCUT_TYPE_LAST_ADDED = 2;

    public static final String KEY_SHORTCUT_TYPE = "com.remix.myplayer.appshortcuts.ShortcutType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int type = getIntent() != null ? getIntent().getIntExtra(KEY_SHORTCUT_TYPE,-1) : -1;
        startService(type);
        ToastUtil.show(mContext,"进入AppShortcutActivity:" + type);
        finish();
    }

    private void startService(int type){
        Intent intent = new Intent(this, MusicService.class);
        switch (type){
            case SHORTCUT_TYPE_LAST_ADDED:
                intent.setAction(MusicService.ACTION_SHORTCUT_LASTADDED);
                break;
        }
        startService(intent);
    }
}
