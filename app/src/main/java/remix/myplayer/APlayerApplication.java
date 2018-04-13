package remix.myplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.leakcanary.LeakCanary;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.Config;
import com.umeng.socialize.PlatformConfig;
import com.umeng.socialize.UMShareAPI;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.update.BmobUpdateAgent;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
import remix.myplayer.bean.Category;
import remix.myplayer.db.DBManager;
import remix.myplayer.db.DBOpenHelper;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CrashHandler;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PermissionUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 16-3-16.
 */

public class APlayerApplication extends MultiDexApplication{
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        initUtil();
        initTheme();

        //友盟
        UMConfigure.init(this,null,null,UMConfigure.DEVICE_TYPE_PHONE,null);
        MobclickAgent.setCatchUncaughtExceptions(true);
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);
        //友盟
        UMShareAPI.get(this);
        Config.DEBUG = BuildConfig.DEBUG;
        //bomb
        Bmob.initialize(this, "0c070110fffa9e88a1362643fb9d4d64");
        BmobUpdateAgent.setUpdateOnlyWifi(false);
        BmobUpdateAgent.update(this);
        //禁止默认的页面统计方式
        MobclickAgent.openActivityDurationTrack(false);
        //异常捕获
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        //检测内存泄漏
        if(!LeakCanary.isInAnalyzerProcess(this)){
            LeakCanary.install(this);
        }
        //AppShortcut
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            new DynamicShortcutManager(this).setUpShortcut();

//        if(SPUtil.getValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"Temp",true)){
//            SPUtil.putValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"Temp",false);
//            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,"");
//        }
        //兼容性
        if(/**SPUtil.getValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"Temp",true)*/true){
            SPUtil.putValue(this,SPUtil.SETTING_KEY.SETTING_NAME,"Temp",false);

//            String categoryJson = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,"");
            String categoryJson = "[{\"mIndex\":0,\"mTitle\":\"歌曲\"},{\"mIndex\":1,\"mTitle\":\"专辑\"},{\"mIndex\":2,\"mTitle\":\"艺术家\"},{\"mIndex\":3,\"mTitle\":\"播放列表\"},{\"mIndex\":4,\"mTitle\":\"文件夹\"}]";
            List<Category> oldCategories = TextUtils.isEmpty(categoryJson) ? new ArrayList<>() : new Gson().fromJson(categoryJson,new TypeToken<List<Category>>(){}.getType());
            if(oldCategories == null || oldCategories.size() == 0){
                return;
            }
            List<Category> newCategories = new ArrayList<>();
            for(Category category : oldCategories){
                final String title = category.getTitle();
                final int resId = title.equalsIgnoreCase(getString(R.string.tab_song)) ? R.string.tab_song :
                        title.equalsIgnoreCase(getString(R.string.tab_album)) ? R.string.tab_album :
                        title.equalsIgnoreCase(getString(R.string.tab_artist)) ? R.string.tab_artist :
                        title.equalsIgnoreCase(getString(R.string.tab_playlist)) ? R.string.tab_playlist : R.string.tab_folder;
                newCategories.add(new Category(resId));
            }
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,new Gson().toJson(newCategories,new TypeToken<List<Category>>(){}.getType()));
        }
    }

    private void initUtil() {
        //初始化工具类
        DBManager.initialInstance(new DBOpenHelper(this));
        PermissionUtil.setContext(this);
        MediaStoreUtil.setContext(this);
        Util.setContext(this);
        ImageUriUtil.setContext(this);
        DiskCache.init(this);
        ColorUtil.setContext(this);
        PlayListUtil.setContext(this);
        final int cacheSize = (int)(Runtime.getRuntime().maxMemory() / 8);
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(() -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE,cacheSize,Integer.MAX_VALUE, 2 * ByteConstants.MB))
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .setDownsampleEnabled(true)
                .build();
        Fresco.initialize(this,config);
    }

    /**
     * 初始化主题
     */
    private void initTheme() {
        ThemeStore.THEME_MODE = ThemeStore.loadThemeMode();
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();

        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
    }

    public static Context getContext(){
        return mContext;
    }

    static {
        PlatformConfig.setWeixin("wx10775467a6664fbb","8a64ff1614ffe8d8dd4f8cc794f3c4f1");
    }
}
