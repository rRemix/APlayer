package remix.myplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import remix.myplayer.appshortcuts.AppShortcutActivity;

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public abstract class BaseShortcutType {
    static final String ID_PREFIX = "com.remix.myplayer.appshortcuts.id.";
    Context mContext;


    BaseShortcutType(Context context){
        mContext = context;
    }

    Intent getIntent(int type){
        Intent intent = new Intent(mContext, AppShortcutActivity.class);
        intent.putExtra(AppShortcutActivity.KEY_SHORTCUT_TYPE,type);
        intent.setAction(Intent.ACTION_VIEW);
        return intent;
    }

    public abstract ShortcutInfo getShortcutInfo();
}
