package remix.myplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;
import android.os.Build;

import remix.myplayer.R;
import remix.myplayer.ui.activity.AppShortcutActivity;

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class MyLoveShortcutType extends BaseShortcutType {
    public MyLoveShortcutType(Context context) {
        super(context);
    }

    @Override
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(mContext,ID_PREFIX + "my_love")
                .setShortLabel(mContext.getString(R.string.my_favorite))
                .setLongLabel(mContext.getString(R.string.my_favorite))
                .setIcon(Icon.createWithResource(mContext, R.drawable.icon_appshortcut_my_love))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_MY_LOVE))
                .build();
    }
}
