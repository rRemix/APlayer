package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.umeng.analytics.MobclickAgent;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.listener.BmobUpdateListener;
import cn.bmob.v3.update.BmobUpdateAgent;
import cn.bmob.v3.update.UpdateResponse;
import cn.bmob.v3.update.UpdateStatus;
import remix.myplayer.R;
import remix.myplayer.db.DBOpenHelper;
import remix.myplayer.listener.ShakeDetector;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.ColorChooseDialog;
import remix.myplayer.ui.dialog.FolderChooserDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

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
    @BindView(R.id.setting_lockscreen_text)
    TextView mLockScreenText;
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
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECREATE)
                recreate();
            if(msg.what == CACHESIZE){
                mCache.setText(getString(R.string.cache_szie,mCacheSize / 1024f / 1024));
            }
            if(msg.what == CLEARFINISH){
                ToastUtil.show(SettingActivity.this,getString(R.string.clear_success));
                mCache.setText("0MB");
                mLrcPath.setText(R.string.default_lrc_path);
            }
        }
    };
    private final int[] mScanSize = new int[]{0,500 * ByteConstants.KB,ByteConstants.MB, 2 * ByteConstants.MB};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        setUpToolbar(mToolbar,getString(R.string.setting));

        //读取重启aitivity之前的数据
        if(savedInstanceState != null){
            mNeedRecreate = savedInstanceState.getBoolean("needRecreate");
            mNeedRefresh = savedInstanceState.getBoolean("needRefresh");
            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
        }

        //导航栏是否变色 是否启用摇一摇切歌
        final String[] keyWord = new String[]{"ColorNavigation","Shake","OnlineLrc","FloatLrc"};
        ButterKnife.apply(new SwitchCompat[]{ mNaviSwitch, mShakeSwitch,mLrcPrioritySwitch,mFloatLrcSwitch}, new ButterKnife.Action<SwitchCompat>() {
            @Override
            public void apply(@NonNull SwitchCompat view, final int index) {
                //只有锁屏默认开启，其余默认都关闭
                view.setChecked(SPUtil.getValue(mContext,"Setting",keyWord[index],index == 0));
                //5.0以上才支持变色导航栏
                if(index == 1){
                    view.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                }
                view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        SPUtil.putValue(SettingActivity.this,"Setting",keyWord[index],isChecked);
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
                                break;
                            //桌面歌词
                            case 3:
                                mFloatLrcTip.setText(isChecked ? R.string.opened_float_lrc : R.string.closed_float_lrc);
                                Intent intent = new Intent(Constants.CTL_ACTION);
                                intent.putExtra("FloatLrc",mFloatLrcSwitch.isChecked());
                                intent.putExtra("Control",Constants.TOGGLE_FLOAT_LRC);
                                sendBroadcast(intent);
                                break;
                        }
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
                new ButterKnife.Action<ImageView>(){
                    @Override
                    public void apply(@NonNull ImageView view, int index) {
                        Theme.TintDrawable(view,view.getBackground(),arrowColor);
                    }
                });

        //分根线颜色
        ButterKnife.apply(new View[]{findView(R.id.setting_divider_1),findView(R.id.setting_divider_2),
                findView(R.id.setting_divider_3),findView(R.id.setting_divider_4)},
                new ButterKnife.Action<View>() {
                    @Override
                    public void apply(@NonNull View view, int index) {
                        view.setBackgroundColor(ThemeStore.getDividerColor());
                    }
                });

        //计算缓存大小
        new Thread(){
            @Override
            public void run() {
                mCacheSize = 0;
                mCacheSize += CommonUtil.getFolderSize(getExternalCacheDir());
                mCacheSize += CommonUtil.getFolderSize(getCacheDir());
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
        boolean success = SPUtil.putValue(this,"Setting","LrcSearchPath",folder.getAbsolutePath());
        ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
        mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
    }

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_container,
            R.id.setting_feedback_container,R.id.setting_about_container, R.id.setting_update_container,
            R.id.setting_lockscreen_container,R.id.setting_lrc_priority_container,R.id.setting_lrc_float_container,
            R.id.setting_navigation_container,R.id.setting_shake_container, R.id.setting_eq_container,
            R.id.setting_lrc_path_container,R.id.setting_clear_container,R.id.setting_donate_container})
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
                        .itemsCallbackSingleChoice(position, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                SPUtil.putValue(SettingActivity.this, "Setting", "ScanSize", mScanSize[which]);
                                Constants.SCAN_SIZE = mScanSize[which];
                                return true;
                            }
                        })
                        .theme(ThemeStore.getMDDialogTheme())
                        .positiveText(R.string.confirm)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .show();
                break;
            //桌面歌词
            case R.id.setting_lrc_float_container:
                mFloatLrcSwitch.setChecked(!mFloatLrcSwitch.isChecked());
                break;
            //歌词扫描路径
            case R.id.setting_lrc_path_container:
                new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .allowNewFolder(false,R.string.new_folder)
                        .show();
                break;
            //歌词搜索优先级
            case R.id.setting_lrc_priority_container:
                mLrcPrioritySwitch.setChecked(!mLrcPrioritySwitch.isChecked());
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
                            new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    SPUtil.putValue(SettingActivity.this,"Setting","LockScreenOn",which);
                                    Intent intent = new Intent(Constants.CTL_ACTION);
                                    intent.putExtra("Control",Constants.TOGGLE_MEDIASESSION);
                                    sendBroadcast(intent);
                                    return true;
                                }
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
            case R.id.setting_notify_container:
                try {
                    MobclickAgent.onEvent(this,"NotifyColor");
                    new MaterialDialog.Builder(this)
                            .title(R.string.notify_bg_color)
                            .titleColorAttr(R.attr.text_color_primary)
                            .positiveText(R.string.choose)
                            .positiveColorAttr(R.attr.text_color_primary)
                            .buttonRippleColorAttr(R.attr.ripple_color)
                            .items(new String[]{getString(R.string.use_system_color),getString(R.string.use_black_color)})
                            .itemsCallbackSingleChoice(SPUtil.getValue(SettingActivity.this,"Setting","IsSystemColor",true) ? 0 : 1,
                                    new MaterialDialog.ListCallbackSingleChoice() {
                                        @Override
                                        public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                            SPUtil.putValue(SettingActivity.this,"Setting","IsSystemColor",which == 0);
                                            sendBroadcast(new Intent(Constants.NOTIFY));
                                            return true;
                                        }
                                    })
                            .backgroundColorAttr(R.attr.background_color_3)
                            .itemsColorAttr(R.attr.text_color_primary)
                            .theme(ThemeStore.getMDDialogTheme())
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            //音效设置
            case R.id.setting_eq_container:
                MobclickAgent.onEvent(this,"EQ");
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                if(CommonUtil.isIntentAvailable(this,audioEffectIntent)){
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
                BmobUpdateAgent.setUpdateListener(new BmobUpdateListener() {
                    @Override
                    public void onUpdateReturned(int updateStatus, UpdateResponse updateInfo) {
                        // TODO Auto-generated method stub
                        if(updateStatus == UpdateStatus.No){
                            ToastUtil.show(mContext,getString(R.string.no_update));
                        }else if(updateStatus == UpdateStatus.IGNORED){
                            ToastUtil.show(mContext,getString(R.string.update_ignore));
                        }else if(updateStatus == UpdateStatus.TimeOut){
                            ToastUtil.show(mContext,R.string.updat_error);
                        }
                    }
                });
                BmobUpdateAgent.forceUpdate(this);
                break;
            //捐赠
            case R.id.setting_donate_container:
                new MaterialDialog.Builder(this)
                        .title(R.string.donate)
                        .titleColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.copy_account)
                        .negativeText(R.string.cancel)
                        .content(R.string.donate_tip)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                ClipboardManager clipboardManager = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("text", "lin_kin_p@163.com");
                                clipboardManager.setPrimaryClip(clipData);
                                ToastUtil.show(mContext,getString(R.string.alread_copy));
                            }
                        })
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
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                new Thread(){
                                    @Override
                                    public void run() {
                                        //清除歌词，封面等缓存
                                        //清除配置文件、数据库等缓存
                                        CommonUtil.deleteFilesByDirectory(getCacheDir());
                                        CommonUtil.deleteFilesByDirectory(getExternalCacheDir());
                                        SPUtil.deleteFile(SettingActivity.this,"Setting");
                                        deleteDatabase(DBOpenHelper.DBNAME);
                                        //清除fresco缓存
                                        Fresco.getImagePipeline().clearCaches();
                                        mHandler.sendEmptyMessage(CLEARFINISH);
                                        mNeedRefresh = true;
                                    }
                                }.start();
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
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
