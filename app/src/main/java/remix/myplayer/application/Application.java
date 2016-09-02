package remix.myplayer.application;

import android.content.Context;
import android.content.Intent;
import android.util.Xml;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import remix.myplayer.listener.LockScreenListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.service.TimerService;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.CrashHandler;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.ErrUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PermissionUtil;
import remix.myplayer.util.SharedPrefsUtil;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-3-16.
 */

/**
 * 错误收集与上报
 */
public class Application extends android.app.Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        startService(new Intent(this, MusicService.class));
        //定时
        startService(new Intent(this, TimerService.class));
        //监听锁屏
        new LockScreenListener(getApplicationContext()).beginListen();
        //检查更新
        UmengUpdateAgent.update(this);
        MobclickAgent.setCatchUncaughtExceptions(true);
        initUtil();
        loadSong();
    }

    /**
     * 读取歌曲id列表与正在播放列表
     */
    public void loadSong() {
        new Thread() {
            @Override
            public void run() {
                //读取sd卡歌曲id
                Global.mAllSongList = DBUtil.getAllSongsId();
                //读取正在播放列表
                boolean isFirst = SharedPrefsUtil.getValue(mContext, "Setting", "First", true);
                if(isFirst){
                    //添加我的收藏列表
                    XmlUtil.addPlaylist("我的收藏");
                    Global.setPlayingList(Global.mAllSongList);
                } else {
                    Global.mPlayingList = XmlUtil.getPlayingList();
                    if (Global.mPlayingList == null || Global.mPlayingList.size() == 0)
                        Global.mPlayingList = Global.mAllSongList;
                }

            }
        }.start();

    }

    private void initUtil() {
        //初始化工具类
        PermissionUtil.setContext(mContext);
        XmlUtil.setContext(mContext);
        DBUtil.setContext(mContext);
        CommonUtil.setContext(mContext);
        ErrUtil.setContext(mContext);
        DiskCache.init(mContext);
        ColorUtil.setContext(mContext);
        final int CacheSize = (int)(Runtime.getRuntime().maxMemory() / 8);
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(new Supplier<MemoryCacheParams>() {
                    @Override
                    public MemoryCacheParams get() {
                        return new MemoryCacheParams(CacheSize, Integer.MAX_VALUE,CacheSize, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    }
                })
                .build();
        Fresco.initialize(this,config);
    }

    public static Context getContext(){
        return mContext;
    }
}
