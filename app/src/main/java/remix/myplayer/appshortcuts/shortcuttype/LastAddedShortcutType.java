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
public class LastAddedShortcutType extends BaseShortcutType {
    public LastAddedShortcutType(Context context) {
        super(context);
    }

    @Override
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(mContext,ID_PREFIX + "last_added")
                .setShortLabel(mContext.getString(R.string.recently))
                .setLongLabel(mContext.getString(R.string.recently))
                .setIcon(Icon.createWithResource(mContext, R.mipmap.ic_launcher))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_LAST_ADDED))
                .build();
    }
}
