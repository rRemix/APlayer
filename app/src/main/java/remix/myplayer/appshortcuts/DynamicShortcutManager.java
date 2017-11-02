package remix.myplayer.appshortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import remix.myplayer.appshortcuts.shortcuttype.LastAddedShortcutType;
import remix.myplayer.appshortcuts.shortcuttype.MyLoveShortcutType;
import remix.myplayer.appshortcuts.shortcuttype.ShuffleShortcutType;

/**
 * Created by Remix on 2017/11/1.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class DynamicShortcutManager extends ContextWrapper {
    private Context mContext;
    private ShortcutManager mShortcutManger;

    public DynamicShortcutManager(Context base) {
        super(base);
        mContext = base;
        mShortcutManger = getSystemService(ShortcutManager.class);
    }

    public void setUpShortcut(){
        if(mShortcutManger.getDynamicShortcuts().size() == 0){
            mShortcutManger.setDynamicShortcuts(getDefaultShortcut());
        }
    }

    @NonNull
    private List<ShortcutInfo> getDefaultShortcut() {
        return Arrays.asList(new ShuffleShortcutType(mContext).getShortcutInfo(),
                new MyLoveShortcutType(mContext).getShortcutInfo(),
                new LastAddedShortcutType(mContext).getShortcutInfo());
    }

}
