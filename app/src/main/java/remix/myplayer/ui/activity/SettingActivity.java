package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.umeng.analytics.MobclickAgent;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.update.BmobUpdateAgent;
import cn.bmob.v3.update.UpdateStatus;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.db.DBOpenHelper;
import remix.myplayer.listener.ShakeDetector;
import remix.myplayer.misc.MediaScanner;
import remix.myplayer.misc.floatpermission.FloatWindowManager;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.ColorChooseDialog;
import remix.myplayer.ui.dialog.FolderChooserDialog;
import remix.myplayer.util.AlipayUtil;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
public class SettingActivity extends ToolbarActivity implements FolderChooserDialog.FolderCallback{
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.setting_color_src)
    ImageView mColorSrc;
    @BindView(R.id.setting_lrc_path)
    TextView mLrcPath;
    @BindView(R.id.setting_clear_text)
    TextView mCache;
    @BindView(R.id.setting_navaigation_switch)
    SwitchCompat mNaviSwitch;
    @BindView(R.id.setting_shake_switch)
    SwitchCompat mShakeSwitch;
    @BindView(R.id.setting_lrc_priority_switch)
    SwitchCompat mLrcPrioritySwitch;
    @BindView(R.id.setting_lrc_float_switch)
    SwitchCompat mFloatLrcSwitch;
    @BindView(R.id.setting_lrc_float_tip)
    TextView mFloatLrcTip;
    @BindView(R.id.setting_screen_switch)
    SwitchCompat mScreenSwitch;
    @BindView(R.id.setting_notify_switch)
    SwitchCompat mNotifyStyleSwitch;
    @BindView(R.id.setting_notify_color_container)
    View mNotifyColorContainer;
    @BindView(R.id.setting_album_cover_text)
    TextView mAlbumCoverText;


    //是否需要重建activity
    private boolean mNeedRecreate = false;
    //是否需要刷新adapter
    private boolean mNeedRefresh = false;
    //是否从主题颜色选择对话框返回
    private boolean mFromColorChoose = false;
    //缓存大小
    private long mCacheSize = 0;
    private final int RECREATE = 100;
    private final int CACHESIZE = 101;
    private final int CLEARFINISH = 102;
    private MsgHandler mHandler;
    private final int[] mScanSize = new int[]{0,500 * ByteConstants.KB,ByteConstants.MB, 2 * ByteConstants.MB};
    private String mOriginalAlbumChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        setUpToolbar(mToolbar,getString(R.string.setting));
        mHandler = new MsgHandler(this);

        //读取重启aitivity之前的数据
        if(savedInstanceState != null){
            mNeedRecreate = savedInstanceState.getBoolean("needRecreate");
            mNeedRefresh = savedInstanceState.getBoolean("needRefresh");
            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
        }

        //导航栏是否变色 是否启用摇一摇切歌
        final String[] keyWord = new String[]{"ColorNavigation","Shake",
                "OnlineLrc","FloatLrc", SPUtil.SPKEY.SCREEN_ALWAYS_ON,SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,};
        ButterKnife.apply(new SwitchCompat[]{mNaviSwitch, mShakeSwitch,mLrcPrioritySwitch
                ,mFloatLrcSwitch,mScreenSwitch, mNotifyStyleSwitch}, new ButterKnife.Action<SwitchCompat>() {
            @Override
            public void apply(@NonNull SwitchCompat view, final int index) {
                view.setChecked(SPUtil.getValue(mContext,"Setting",keyWord[index],false));
                //5.0以上才支持变色导航栏
                if(index == 0){
                    view.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                }
                view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        switch (index){
                            //变色导航栏
                            case 0:
                                mNeedRecreate = true;
                                mHandler.sendEmptyMessage(RECREATE);
                                break;
                            //摇一摇
                            case 1:
                                if(isChecked)
                                    ShakeDetector.getInstance(mContext).beginListen();
                                else
                                    ShakeDetector.getInstance(mContext).stopListen();
                                break;
                            //设置歌词搜索优先级
                            case 2:
                                SPUtil.putValue(APlayerApplication.getContext(),"Setting","OnlineLrc",isChecked);
                                break;
                            //桌面歌词
                            case 3:
                                if(isChecked && !FloatWindowManager.getInstance().checkPermission(mContext)){
                                    mFloatLrcSwitch.setOnCheckedChangeListener(null);
                                    mFloatLrcSwitch.setChecked(false);
                                    mFloatLrcSwitch.setOnCheckedChangeListener(this);
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                    ToastUtil.show(mContext,R.string.plase_give_float_permission);
                                    return;
                                }
                                mFloatLrcTip.setText(isChecked ? R.string.opened_float_lrc : R.string.closed_float_lrc);
                                Intent intent = new Intent(MusicService.ACTION_CMD);
                                intent.putExtra("FloatLrc",mFloatLrcSwitch.isChecked());
                                intent.putExtra("Control",Constants.TOGGLE_FLOAT_LRC);
                                sendBroadcast(intent);
                                break;
                            //屏幕常亮
                            case 4:
//                                SPUtil.putValue(mContext,"Setting", SPUtil.SPKEY.SCREEN_ALWAYS_ON,isChecked);
                                break;
                            //通知栏样式
                            case 5:
                                sendBroadcast(new Intent(MusicService.ACTION_CMD)
                                        .putExtra("Control",Constants.TOGGLE_NOTIFY)
                                        .putExtra(SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,isChecked));
                                break;
                        }
                        SPUtil.putValue(SettingActivity.this,"Setting",keyWord[index],isChecked);
                    }
                });
            }
        });

        //歌词搜索路径
        if(!SPUtil.getValue(this,"Setting","LrcSearchPath","").equals("")) {
            mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcSearchPath","")));
        }
        //桌面歌词
        mFloatLrcTip.setText(mFloatLrcSwitch.isChecked() ? R.string.opened_float_lrc : R.string.closed_float_lrc);

        //主题颜色指示器
        ((GradientDrawable)mColorSrc.getDrawable()).setColor(
                ThemeStore.isDay() ? ThemeStore.isLightTheme() ? ColorUtil.getColor(R.color.md_white_primary_dark) : ThemeStore.getMaterialPrimaryColor() : Color.TRANSPARENT);
        //初始化箭头颜色
        final int arrowColor = ThemeStore.getAccentColor();
        ButterKnife.apply( new ImageView[]{findView(R.id.setting_eq_arrow),findView(R.id.setting_feedback_arrow),
                        findView(R.id.setting_about_arrow),findView(R.id.setting_update_arrow),findView(R.id.setting_donate_arrow)},
                (ButterKnife.Action<ImageView>) (view, index) -> Theme.TintDrawable(view,view.getBackground(),arrowColor));

        //分根线颜色
        ButterKnife.apply(new View[]{findView(R.id.setting_divider_1),findView(R.id.setting_divider_2),findView(R.id.setting_divider_6),
                findView(R.id.setting_divider_3),findView(R.id.setting_divider_4),findView(R.id.setting_divider_5)},
                (ButterKnife.Action<View>) (view, index) -> view.setBackgroundColor(ThemeStore.getDividerColor()));

        //封面
        mOriginalAlbumChoice = SPUtil.getValue(mContext,"Setting",SPUtil.SPKEY.AUTO_DOWNLOAD_ALBUM_COVER,mContext.getString(R.string.wifi_only));
        mAlbumCoverText.setText(mOriginalAlbumChoice);

        //计算缓存大小
        new Thread(){
            @Override
            public void run() {
                mCacheSize = 0;
                mCacheSize += Util.getFolderSize(getExternalCacheDir());
                mCacheSize += Util.getFolderSize(getCacheDir());
                mHandler.sendEmptyMessage(CACHESIZE);
            }
        }.start();
    }

    public void onResume() {
        MobclickAgent.onPageStart(SettingActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SettingActivity.class.getSimpleName());
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("needRecreate", mNeedRecreate);
        intent.putExtra("needRefresh",mNeedRefresh);
        setResult(Activity.RESULT_OK,intent);
        finish();
    }

    @Override
    protected void onClickNavigation() {
        onBackPressed();
    }

    @Override
    public void onFolderSelection(@NonNull FolderChooserDialog dialog, @NonNull File folder) {
        String tag = dialog.getTag();
        switch (tag){
            case "Lrc":
                boolean success = SPUtil.putValue(this,"Setting","LrcSearchPath",folder.getAbsolutePath());
                ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
                mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
                break;
            case "Scan":
                new MediaScanner(mContext).scanFiles(folder,"audio/*");
                break;
        }

    }

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_color_container,
            R.id.setting_feedback_container,R.id.setting_about_container, R.id.setting_update_container,
            R.id.setting_lockscreen_container,R.id.setting_lrc_priority_container,R.id.setting_lrc_float_container,
            R.id.setting_navigation_container,R.id.setting_shake_container, R.id.setting_eq_container,
            R.id.setting_lrc_path_container,R.id.setting_clear_container,R.id.setting_donate_container,
            R.id.setting_screen_container,R.id.setting_scan_container,R.id.setting_classic_notify_container,
            R.id.setting_album_cover_container})
    public void onClick(View v){
        switch (v.getId()){
            //文件过滤
            case R.id.setting_filter_container:
                //读取以前设置
                int position = 0;
                for (int i = 0 ; i < mScanSize.length ;i++){
                    position = i;
                    if(mScanSize[i] == Constants.SCAN_SIZE)
                        break;
                }
                new MaterialDialog.Builder(this)
                        .title(R.string.set_filter_size)
                        .titleColorAttr(R.attr.text_color_primary)
                        .items(new String[]{"0K","500K","1MB","2MB"})
                        .itemsColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsCallbackSingleChoice(position, (dialog, itemView, which, text) -> {
                            SPUtil.putValue(SettingActivity.this, "Setting", "ScanSize", mScanSize[which]);
                            Constants.SCAN_SIZE = mScanSize[which];
                            return true;
                        })
                        .theme(ThemeStore.getMDDialogTheme())
                        .positiveText(R.string.confirm)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            //桌面歌词
            case R.id.setting_lrc_float_container:
//                if((!mFloatLrcSwitch.isChecked() && FloatWindowManager.getInstance().checkPermission(this)) || mFloatLrcSwitch.isChecked()){
//                    mFloatLrcSwitch.setChecked(!mFloatLrcSwitch.isChecked());
//                }
                mFloatLrcSwitch.setChecked(!mFloatLrcSwitch.isChecked());
                break;
            //歌词扫描路径
            case R.id.setting_lrc_path_container:
                new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .allowNewFolder(false,R.string.new_folder)
                        .tag("Lrc")
                        .show();
                break;
            //歌词搜索优先级
            case R.id.setting_lrc_priority_container:
                mLrcPrioritySwitch.setChecked(!mLrcPrioritySwitch.isChecked());
                break;
            //屏幕常亮
            case R.id.setting_screen_container:
                mScreenSwitch.setChecked(!mScreenSwitch.isChecked());
                break;
            //手动扫描
            case R.id.setting_scan_container:
                new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .tag("Scan")
                        .allowNewFolder(false,R.string.new_folder)
                        .show();
                break;
            //锁屏显示
            case R.id.setting_lockscreen_container:
                //0:软件锁屏 1:系统锁屏 2:关闭
                new MaterialDialog.Builder(this).title(R.string.lockscreen_show)
                    .titleColorAttr(R.attr.text_color_primary)
                    .positiveText(R.string.choose)
                    .positiveColorAttr(R.attr.text_color_primary)
                    .buttonRippleColorAttr(R.attr.ripple_color)
                    .items(new String[]{getString(R.string.aplayer_lockscreen), getString(R.string.system_lockscreen), getString(R.string.close)})
                    .itemsCallbackSingleChoice(SPUtil.getValue(SettingActivity.this,"Setting","LockScreenOn",Constants.APLAYER_LOCKSCREEN) ,
                            (dialog, view, which, text) -> {
                                SPUtil.putValue(SettingActivity.this,"Setting","LockScreenOn",which);
                                Intent intent = new Intent(MusicService.ACTION_CMD);
                                intent.putExtra("Control",Constants.TOGGLE_MEDIASESSION);
                                sendBroadcast(intent);
                                return true;
                            })
                    .backgroundColorAttr(R.attr.background_color_3)
                    .itemsColorAttr(R.attr.text_color_primary)
                    .theme(ThemeStore.getMDDialogTheme())
                    .show();
                break;
            //导航栏变色
            case R.id.setting_navigation_container:
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    ToastUtil.show(this,getString(R.string.only_lollopop));
                    return;
                }
                mNaviSwitch.setChecked(!mNaviSwitch.isChecked());
                break;
            //摇一摇
            case R.id.setting_shake_container:
                mShakeSwitch.setChecked(!mShakeSwitch.isChecked());
                break;
            //选择主色调
            case R.id.setting_color_container:
                startActivityForResult(new Intent(this, ColorChooseDialog.class),0);
                break;
            //通知栏底色
            case R.id.setting_notify_color_container:
                if(!SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,false)){
                    ToastUtil.show(mContext,R.string.notify_bg_color_warnning);
                    return;
                }
                MobclickAgent.onEvent(this,"NotifyColor");
                new MaterialDialog.Builder(this)
                        .title(R.string.notify_bg_color)
                        .titleColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.choose)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .buttonRippleColorAttr(R.attr.ripple_color)
                        .items(new String[]{getString(R.string.use_system_color),getString(R.string.use_black_color)})
                        .itemsCallbackSingleChoice(SPUtil.getValue(mContext,"Setting","IsSystemColor",true) ? 0 : 1,
                                (dialog, view, which, text) -> {
                                    SPUtil.putValue(mContext,"Setting","IsSystemColor",which == 0);
                                    sendBroadcast(new Intent(MusicService.ACTION_CMD)
                                            .putExtra("Control",Constants.TOGGLE_NOTIFY)
                                            .putExtra(SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,mNotifyStyleSwitch.isChecked()));
                                    return true;
                                })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsColorAttr(R.attr.text_color_primary)
                        .theme(ThemeStore.getMDDialogTheme())
                        .show();
                break;
            //音效设置
            case R.id.setting_eq_container:
                MobclickAgent.onEvent(this,"EQ");
                final int sessionId = MusicService.getMediaPlayer().getAudioSessionId();
                if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
                    Toast.makeText(mContext,getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show();
                    return;
                }
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                if(Util.isIntentAvailable(this,audioEffectIntent)){
                    startActivityForResult(audioEffectIntent, 0);
                } else {
                    startActivity(new Intent(this,EQActivity.class));
                }
                break;
            //意见与反馈
            case R.id.setting_feedback_container:
                startActivity(new Intent(this,FeedBackActivity.class));
                break;
            //关于我们
            case R.id.setting_about_container:
                startActivity(new Intent(this,AboutActivity.class));
                break;
            //检查更新
            case R.id.setting_update_container:
                MobclickAgent.onEvent(this,"CheckUpdate");
                BmobUpdateAgent.setUpdateListener((updateStatus, updateInfo) -> {
                    // TODO Auto-generated method stub
                    if(updateStatus == UpdateStatus.No){
                        ToastUtil.show(mContext,getString(R.string.no_update));
                    }else if(updateStatus == UpdateStatus.IGNORED){
                        ToastUtil.show(mContext,getString(R.string.update_ignore));
                    }else if(updateStatus == UpdateStatus.TimeOut){
                        ToastUtil.show(mContext,R.string.updat_error);
                    }
                });
                BmobUpdateAgent.forceUpdate(this);
                break;
            //捐赠
            case R.id.setting_donate_container:
                new MaterialDialog.Builder(this)
                        .title(R.string.donate)
                        .titleColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.jump_alipay_account)
                        .negativeText(R.string.cancel)
                        .content(R.string.donate_tip)
                        .onPositive((dialog, which) -> AlipayUtil.startAlipayClient((Activity) mContext,"FKX01908X8ECOECIQZIL43"))
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            //清除缓存
            case R.id.setting_clear_container:
                new MaterialDialog.Builder(this)
                        .content(R.string.confirm_clear_cache)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .onPositive((dialog, which) -> new Thread(){
                            @Override
                            public void run() {
                                //清除歌词，封面等缓存
                                //清除配置文件、数据库等缓存
                                Util.deleteFilesByDirectory(getCacheDir());
                                Util.deleteFilesByDirectory(getExternalCacheDir());
                                SPUtil.deleteFile(SettingActivity.this,"Setting");
                                deleteDatabase(DBOpenHelper.DBNAME);
                                //清除fresco缓存
                                Fresco.getImagePipeline().clearCaches();
                                mHandler.sendEmptyMessage(CLEARFINISH);
                                mNeedRefresh = true;
                            }
                        }.start())
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            case R.id.setting_classic_notify_container:
                mNotifyStyleSwitch.setChecked(!mNotifyStyleSwitch.isChecked());
                break;
            //专辑与艺术家封面自动下载
            case R.id.setting_album_cover_container:
                final String choice =  SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.AUTO_DOWNLOAD_ALBUM_COVER,mContext.getString(R.string.wifi_only));
                final int selected = mContext.getString(R.string.wifi_only).equals(choice) ? 1 : mContext.getString(R.string.always).equals(choice) ? 0 : 2;
                new MaterialDialog.Builder(this)
                        .title(R.string.auto_download_album_cover)
                        .titleColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.choose)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .buttonRippleColorAttr(R.attr.ripple_color)
                        .items(new String[]{getString(R.string.always),getString(R.string.wifi_only),getString(R.string.never)})
                        .itemsCallbackSingleChoice(selected,
                                (dialog, view, which, text) -> {
                                    mAlbumCoverText.setText(text);
                                    //仅从从不改变到仅在wifi下或者总是的情况下，才刷新Adapter
                                    mNeedRefresh |= (mContext.getString(R.string.wifi_only).equals(text) & !mOriginalAlbumChoice.equals(text));
                                    ImageUriRequest.AUTO_DOWNLOAD_ALBUM = text.toString();
                                    SPUtil.putValue(mContext,"Setting",
                                            SPUtil.SPKEY.AUTO_DOWNLOAD_ALBUM_COVER,
                                            text.toString());
                                    return true;
                                })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsColorAttr(R.attr.text_color_primary)
                        .theme(ThemeStore.getMDDialogTheme())
                        .show();
                break;
        }
    }

    @OnHandleMessage
    public void handleInternal(Message msg){
        if(msg.what == RECREATE)
            recreate();
        if(msg.what == CACHESIZE){
            mCache.setText(getString(R.string.cache_szie,mCacheSize / 1024f / 1024));
        }
        if(msg.what == CLEARFINISH){
            ToastUtil.show(SettingActivity.this,getString(R.string.clear_success));
            mCache.setText(R.string.zero_size);
            mLrcPath.setText(R.string.default_lrc_path);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("needRecreate", mNeedRecreate);
        outState.putBoolean("fromColorChoose",mFromColorChoose);
        outState.putBoolean("needRefresh",mNeedRefresh);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.remove();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && data != null){
            mNeedRecreate = data.getBooleanExtra("needRecreate",false);
            mFromColorChoose = data.getBooleanExtra("fromColorChoose",false);
            if(mNeedRecreate){
                mHandler.sendEmptyMessage(RECREATE);
            }
        }
    }

}
