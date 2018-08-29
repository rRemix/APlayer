package remix.myplayer.appshortcuts;

import android.content.Intent;
import android.os.Bundle;

import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.base.BaseMusicActivity;

/**
 * Created by Remix on 2017/11/1.
 */

public class AppShortcutActivity extends BaseMusicActivity {
    public static final int SHORTCUT_TYPE_SHUFFLE_ALL = 0;
    public static final int SHORTCUT_TYPE_MY_LOVE = 1;
    public static final int SHORTCUT_TYPE_LAST_ADDED = 2;
    public static final int SHORTCUT_TYPE_CONTINUE_PLAY = 3;

    public static final String KEY_SHORTCUT_TYPE = "com.remix.myplayer.appshortcuts.ShortcutType";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int type = getIntent() != null ? getIntent().getIntExtra(KEY_SHORTCUT_TYPE,-1) : -1;
        startService(type);
        finish();
    }

    private void startService(int type){
        Intent intent = new Intent(this, MusicService.class);
        switch (type){
            case SHORTCUT_TYPE_LAST_ADDED:
                intent.setAction(MusicService.ACTION_SHORTCUT_LASTADDED);
                break;
            case SHORTCUT_TYPE_SHUFFLE_ALL:
                intent.setAction(MusicService.ACTION_SHORTCUT_SHUFFLE);
                break;
            case SHORTCUT_TYPE_MY_LOVE:
                intent.setAction(MusicService.ACTION_SHORTCUT_MYLOVE);
                break;
            case SHORTCUT_TYPE_CONTINUE_PLAY:
                intent.setAction(MusicService.ACTION_SHORTCUT_CONTINUE_PLAY);
                break;
        }
        startService(intent);
    }
}
