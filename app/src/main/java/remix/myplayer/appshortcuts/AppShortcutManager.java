package remix.myplayer.appshortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.ui.activity.MainActivity;

/**
 * Created by Remix on 2017/10/16.
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
public class AppShortcutManager {
    private ShortcutManager mShortcutManager;
    private Context mContext;
    static final String ID_PREFIX = "com.remix.myplayer.appshortcuts.id.";

    private AppShortcutManager(){
        mShortcutManager = APlayerApplication.getContext().getSystemService(ShortcutManager.class);
        mContext = APlayerApplication.getContext();
    }

    public static AppShortcutManager getInstance(){
        return Holder.mInstance;
    }

    public void setUpShortcuts(){
        List<ShortcutInfo> infos = new ArrayList<>();
        String[] titles = new String[]{"最近添加","我的收藏","随机播放"};
        int[] icons = new int[]{R.mipmap.ic_launcher,R.mipmap.ic_launcher,R.mipmap.ic_launcher};
        for(int i = 0 ; i < 3;i++){
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra("Type",i);

            ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(mContext,ID_PREFIX + i)
                    .setShortLabel(titles[i])
                    .setLongLabel(titles[i])
                    .setIcon(Icon.createWithResource(mContext, icons[i]))
                    .setIntent(intent)
                    .build();
            infos.add(shortcutInfo);
        }
        mShortcutManager.setDynamicShortcuts(infos);
    }

    private static class Holder{
        private static AppShortcutManager mInstance = new AppShortcutManager();
    }
}
