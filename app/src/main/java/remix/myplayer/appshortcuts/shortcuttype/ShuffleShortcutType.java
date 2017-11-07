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
public class ShuffleShortcutType extends BaseShortcutType {
    public ShuffleShortcutType(Context context) {
        super(context);
    }

    @Override
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(mContext,ID_PREFIX + "shuffle")
                .setShortLabel(mContext.getString(R.string.model_random))
                .setLongLabel(mContext.getString(R.string.model_random))
                .setIcon(Icon.createWithResource(mContext, R.drawable.icon_appshortcut_shuffle))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_SHUFFLE_ALL))
                .build();
    }
}
