package remix.myplayer.application;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialog;
import android.util.JsonToken;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengNotificationClickHandler;
import com.umeng.message.entity.UMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.listener.LockScreenListener;
import remix.myplayer.model.UpdateInfo;
import remix.myplayer.service.MusicService;
import remix.myplayer.service.TimerService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.CrashHandler;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.ErrUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PermissionUtil;
import remix.myplayer.util.SPUtil;
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
        //初始化友盟推送
        PushAgent mPushAgent = PushAgent.getInstance(this);
        //注册推送服务，每次调用register方法都会回调该接口
        mPushAgent.register(new IUmengRegisterCallback() {
            @Override
            public void onSuccess(String deviceToken) {
                //注册成功会返回device token
                Log.d("DeviceToken","DeviceToker:" + deviceToken);
            }
            @Override
            public void onFailure(String s, String s1) {
            }
        });
        UmengNotificationClickHandler notificationClickHandler = new UmengNotificationClickHandler() {
            @Override
            public void dealWithCustomAction(Context context, UMessage msg) {
                try {
                    final UpdateInfo info = new UpdateInfo();
                    JSONObject json = new JSONObject(msg.custom);
                    JSONArray jsonlogs = json.getJSONArray("update_log");
                    for(int i = 0; i < jsonlogs.length();i++){
                        info.Logs.add(jsonlogs.getString(i));
                    }
                    info.ApkUrl = json.getString("apk_url");
                    info.MD5 = json.getString("md5");
                    info.VersionName = json.getString("version");
                    info.Size = json.getString("target_size");

                    if(MainActivity.mInstance == null){
                        CommonUtil.openUrl(info.ApkUrl);
                    } else {
                        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.mInstance)
                                .title("发现新版本")
                                .titleColor(ThemeStore.getTextColorPrimary())
                                .backgroundColor(ThemeStore.getBackgroundColor3())
                                .customView(R.layout.dialog_update,true)
                                .positiveText("立即更新")
                                .positiveColor(ThemeStore.getMaterialColorPrimaryColor())
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        CommonUtil.openUrl(info.ApkUrl);
                                    }
                                })
                                .negativeText("以后再说")
                                .negativeColor(ThemeStore.getTextColorPrimary())
                                .build();
                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_version))).setText(info.VersionName);
                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_size))).setText(info.Size);
                        String logs = "";
                        for(int i = 0 ; i < info.Logs.size();i++){
                            logs += (i + 1 + "." + info.Logs.get(i) + "\n");
                        }
                        ((TextView)(materialDialog.getCustomView().findViewById(R.id.update_log))).setText(logs);
                        materialDialog.show();
                    }

//                    final AppCompatDialog dialog = new AppCompatDialog(MainActivity.mInstance);
//                    dialog.setContentView(R.layout.umeng_update_dialog);
//
//                    ((TextView)dialog.findViewById(R.id.update_version)).setText("最新版本:" + info.VersionName);
//                    ((TextView)dialog.findViewById(R.id.update_size)).setText("新版本大小:" + info.Size);
//                    String logs = "";
//                    for(int i = 0 ; i < info.Logs.size();i++){
//                        logs += (i + "." + info.Logs.get(i) + "\n");
//                    }
//                    ((TextView)dialog.findViewById(R.id.update_log)).setText("更新内容\n" + logs);
//                    dialog.findViewById(R.id.umeng_update_id_cancel).setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            if(dialog != null && !dialog.isShowing())
//                                dialog.dismiss();
//                        }
//                    });
//                    dialog.findViewById(R.id.umeng_update_id_ok).setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            CommonUtil.openUrl(info.ApkUrl);
//                            if(dialog != null && !dialog.isShowing())
//                                dialog.dismiss();
//                        }
//                    });
//                    dialog.show();

                } catch (JSONException e) {
                    Log.d("Application","创建更新对话框错误:" + e.toString());
                    Toast.makeText(context,"创建更新对话框错误:" + e.toString(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        };
        mPushAgent.setNotificationClickHandler(notificationClickHandler);
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
                if (Global.mPlayingList == null || Global.mPlayingList.size() == 0)
                    Global.mPlayingList = Global.mAllSongList;

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
