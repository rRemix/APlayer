package remix.myplayer.application;

import android.content.Context;
import android.content.Intent;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

import cn.bmob.v3.Bmob;
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
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.PermissionUtil;
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
        //日志
        LogUtil.isDebug = true;
        //bomb
        Bmob.initialize(this, "0c070110fffa9e88a1362643fb9d4d64");
        //禁止默认的页面统计方式
        MobclickAgent.openActivityDurationTrack(false);
        //初始化友盟推送
        PushAgent mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {
            @Override
            public void onSuccess(String deviceToken) {
                //注册成功会返回device token
                LogUtil.d("DeviceToken","DeviceToker:" + deviceToken);
            }
            @Override
            public void onFailure(String s, String s1) {
            }
        });

//        UmengNotificationClickHandler notificationClickHandler = new UmengNotificationClickHandler() {
//            @Override
//            public void dealWithCustomAction(Context context, UMessage msg) {
//                try {
//                    final UpdateInfo info = new UpdateInfo();
//                    JSONObject json = new JSONObject(msg.custom);
//                    JSONArray jsonlogs = json.getJSONArray("update_log");
//                    for(int i = 0; i < jsonlogs.length();i++){
//                        info.Logs.add(jsonlogs.getString(i));
//                    }
//                    info.ApkUrl = json.getString("apk_url");
//                    info.MD5 = json.getString("md5");
//                    info.VersionName = json.getString("version");
//                    info.Size = json.getString("target_size");
//
//                    if(MainActivity.mInstance == null){
//                        CommonUtil.openUrl(info.ApkUrl);
//                    } else {
//                        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.mInstance)
//                                .title("发现新版本")
//                                .titleColor(ThemeStore.getTextColorPrimary())
//                                .backgroundColor(ThemeStore.getBackgroundColor3())
//                                .customView(R.layout.dialog_update,true)
//                                .positiveText("立即更新")
//                                .positiveColor(ThemeStore.getMaterialColorPrimaryColor())
//                                .onPositive(new MaterialDialog.SingleButtonCallback() {
//                                    @Override
//                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
//                                        CommonUtil.openUrl(info.ApkUrl);
//                                    }
//                                })
//                                .negativeText("以后再说")
//                                .negativeColor(ThemeStore.getTextColorPrimary())
//                                .build();
//                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_version))).setText(info.VersionName);
//                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_size))).setText(info.Size);
//                        String logs = "";
//                        for(int i = 0 ; i < info.Logs.size();i++){
//                            logs += (i + 1 + "." + info.Logs.get(i) + "\n");
//                        }
//                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_log))).setText(logs);
//                        materialDialog.show();
//                    }
//
//                } catch (JSONException e) {
//                    LogUtil.d("Application","创建更新对话框错误:" + e.toString());
//                    Toast.makeText(context,"创建更新对话框错误:" + e.toString(),Toast.LENGTH_SHORT).show();
//                    e.printStackTrace();
//                }
//            }
//        };
//        mPushAgent.setNotificationClickHandler(notificationClickHandler);

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
        startService(new Intent(this, MusicService.class));
        //定时
        startService(new Intent(this, TimerService.class));
        //监听锁屏
        new LockScreenListener(getApplicationContext()).beginListen();
        //异常捕获
        MobclickAgent.setCatchUncaughtExceptions(true);
        MobclickAgent.setDebugMode(true);
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
                Global.mPlayingList = XmlUtil.getPlayingList();
                Global.setPlayingList(Global.mPlayingList == null || Global.mPlayingList.size() == 0 ?
                                        Global.mAllSongList : Global.mPlayingList);
                //读取播放列表
                Global.mPlaylist = XmlUtil.getPlayList("playlist.xml");
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
