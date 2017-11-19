package remix.myplayer.appshortcuts.shortcuttype;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;
import android.os.Build;

import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.AppShortcutActivity;

/**
 * Created by Remix on 2017/11/16.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class ContinuePlayShortcutType extends BaseShortcutType {
    public ContinuePlayShortcutType(Context context) {
        super(context);
    }

    @Override
    public ShortcutInfo getShortcutInfo() {
        return new ShortcutInfo.Builder(mContext,ID_PREFIX + "continue_play")
                .setShortLabel(mContext.getString(MusicService.isPlay() ?  R.string.pause_play : R.string.continue_play))
                .setLongLabel(mContext.getString(MusicService.isPlay() ?  R.string.pause_play : R.string.continue_play))
                .setIcon(Icon.createWithResource(mContext, MusicService.isPlay() ? R.drawable.icon_appshortcut_pause : R.drawable.icon_appshortcut_play))
                .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_CONTINUE_PLAY))
                .build();
    }
}
