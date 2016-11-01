package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.folderselector.FolderChooserDialog;
import com.afollestad.materialdialogs.util.DialogUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.update.BmobUpdateAgent;
import remix.myplayer.R;
import remix.myplayer.db.DBOpenHelper;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.ColorChooseDialog;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
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
    //是否需要刷新
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
                mCache.setText(getString(R.string.cache_szie,1.0 * mCacheSize / 1024 / 1024));
            }
            if(msg.what == CLEARFINISH){
                ToastUtil.show(SettingActivity.this,"清除成功");
                mCache.setText("0MB");
                mLrcPath.setText(R.string.default_lrc_path);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        initToolbar(mToolbar,"设置");

        //读取重启aitivity之前的数据
        if(savedInstanceState != null){
            mNeedRefresh = savedInstanceState.getBoolean("needRefresh");
            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
        }

        if(!SPUtil.getValue(this,"Setting","LrcPath","").equals("")) {
            mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
        }
        //初始化箭头颜色
        final int arrowColor = ThemeStore.isDay() ? ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY) : ColorUtil.getColor(R.color.purple_782899);
        ((GradientDrawable)mColorSrc.getDrawable()).setColor(arrowColor);
        ButterKnife.apply( new ImageView[]{findView(R.id.setting_eq_arrow),findView(R.id.setting_feedback_arrow),
                findView(R.id.setting_about_arrow),findView(R.id.setting_update_arrow)},
                new ButterKnife.Action<ImageView>(){
                    @Override
                    public void apply(@NonNull ImageView view, int index) {
                        Theme.TintDrawable(view,view.getBackground(),arrowColor);
                    }
        });

//        //初始化点击效果
//        //默认的颜色
//        final int defaultColor = ThemeStore.isDay() ? ColorUtil.getColor(R.color.white) : ColorUtil.getColor(R.color.night_background_color_3);
//
//        //获得colorcontrolHighlight颜色
//        int effectColor = Color.WHITE;
//        try {
//            effectColor = ThemeStore.isDay() ? DialogUtils.resolveColor(this, R.attr.colorControlHighlight) : ColorUtil.getColor(R.color.night_selected_color);
//        } catch (Exception e){
//            e.printStackTrace();
//        } finally {
//            if(effectColor == Color.WHITE){
//                effectColor = ColorUtil.getColor(R.color.default_control_highlight);
//            }
//        }
//        //设置所有选项点击效果
//        final int[] colors = new int[]{ColorUtil.getColor(R.color.md_plum_primary),ColorUtil.getColor(R.color.md_indigo_primary),ColorUtil.getColor(R.color.md_purple_primary),
//                ColorUtil.getColor(R.color.md_navy_primary),ColorUtil.getColor(R.color.md_brown_primary),ColorUtil.getColor(R.color.md_green_primary),
//                ColorUtil.getColor(R.color.md_plum_primary),ColorUtil.getColor(R.color.md_indigo_primary),ColorUtil.getColor(R.color.md_purple_primary),
//                ColorUtil.getColor(R.color.md_navy_primary),ColorUtil.getColor(R.color.md_brown_primary),ColorUtil.getColor(R.color.md_green_primary)};
//        final ColorDrawable colorDrawable = new ColorDrawable(colors[1]);
//        final Drawable containerDrawable = Theme.getPressDrawable(this,
//                ThemeStore.isDay() ? R.drawable.bg_list_default_day : R.drawable.bg_list_1_default_night,effectColor);
//        ButterKnife.apply(new View[]{findView(R.id.setting_filter_container),findView(R.id.setting_lrc_container),findView(R.id.setting_color_container),
//                        findView(R.id.setting_notify_container),findView(R.id.setting_eq_container),findView(R.id.setting_feedback_container),
//                        findView(R.id.setting_about_container),findView(R.id.setting_update_container),findView(R.id.setting_clear_container)},
//                new ButterKnife.Action<View>() {
//                    @Override
//                    public void apply(@NonNull View view, int index) {
//                        view.setBackground(containerDrawable);
//                    }
//        });

        //计算缓存大小
        new Thread(){
            @Override
            public void run() {
                mCacheSize = 0;
                mCacheSize += CommonUtil.getFolderSize(DiskCache.getDiskCacheDir(SettingActivity.this,"lrc"));
                mCacheSize += CommonUtil.getFolderSize(DiskCache.getDiskCacheDir(SettingActivity.this,"thumbnail"));
                mCacheSize += CommonUtil.getFolderSize(getCacheDir());
                mCacheSize += CommonUtil.getFolderSize(getFilesDir());
                mCacheSize += CommonUtil.getFolderSize(new File("/data/data/" + getPackageName() + "/shared_prefs"));
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
        boolean success = SPUtil.putValue(this,"Setting","LrcPath",folder.getAbsolutePath());
        ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
        mLrcPath.setText(getString(R.string.lrc_tip,SPUtil.getValue(this,"Setting","LrcPath","")));
    }

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_container,
            R.id.setting_feedback_container,R.id.setting_about_container, R.id.setting_update_container,
            R.id.setting_eq_container,R.id.setting_lrc_container,R.id.setting_clear_container})
    public void onClick(View v){
        switch (v.getId()){
            //文件过滤
            case R.id.setting_filter_container:
                startActivity(new Intent(SettingActivity.this,ScanActivity.class));
                break;
            //歌词扫描路径
            case R.id.setting_lrc_container:
                new FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .allowNewFolder(true,R.string.new_folder)
                        .show();
                break;
            //选择主色调
            case R.id.setting_color_container:
                startActivityForResult(new Intent(SettingActivity.this, ColorChooseDialog.class),0);
                break;
            //通知栏底色
            case R.id.setting_notify_container:
                try {
                    MobclickAgent.onEvent(this,"NotifyColor");
                    new MaterialDialog.Builder(this)
                            .title("通知栏底色")
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
                            .positiveColorAttr(R.attr.text_color_primary)
                            .positiveText("选择")
                            .titleColorAttr(R.attr.text_color_primary)
                            .itemsColorAttr(R.attr.text_color_primary)
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
//            //夜间模式
//            case R.id.setting_mode_container:
//                MobclickAgent.onEvent(this,"NightModel");
//                mModeSwitch.setChecked(!mModeSwitch.isChecked());
//                break;
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
                startActivity(new Intent(SettingActivity.this,FeedBakActivity.class));
                break;
            //关于我们
            case R.id.setting_about_container:
                startActivity(new Intent(SettingActivity.this,AboutActivity.class));
                break;
            //检查更新
            case R.id.setting_update_container:
                MobclickAgent.onEvent(this,"CheckUpdate");
                BmobUpdateAgent.forceUpdate(this);
                break;
            //清除缓存
            case R.id.setting_clear_container:
                new MaterialDialog.Builder(SettingActivity.this)
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
                                        CommonUtil.deleteFilesByDirectory(DiskCache.getDiskCacheDir(SettingActivity.this,"lrc"));
                                        CommonUtil.deleteFilesByDirectory(DiskCache.getDiskCacheDir(SettingActivity.this,"thumbnail"));
                                        //清除配置文件、数据库等缓存
                                        CommonUtil.deleteFilesByDirectory(getCacheDir());
                                        CommonUtil.deleteFilesByDirectory(getFilesDir());
                                        CommonUtil.deleteFilesByDirectory(new File("/data/data/" + getPackageName() + "/shared_prefs"));
                                        deleteDatabase(DBOpenHelper.DBNAME);
                                        mHandler.sendEmptyMessage(CLEARFINISH);
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
        outState.putBoolean("needRefresh",mNeedRefresh);
        outState.putBoolean("fromColorChoose",mFromColorChoose);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 0 && data != null){
            mNeedRefresh = data.getBooleanExtra("needRefresh",false);
            mFromColorChoose = data.getBooleanExtra("fromColorChoose",false);
            if(mNeedRefresh){
                mHandler.sendEmptyMessage(RECREATE);
//                if(mFromColorChoose)
//                    mModeSwitch.setChecked(false);
            }

        }
    }

}
