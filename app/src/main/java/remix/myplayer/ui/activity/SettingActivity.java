package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.update.UmengUpdateAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.ColorChooseDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SharedPrefsUtil;

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
public class SettingActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.setting_mode_switch)
    SwitchCompat mModeSwitch;
    @BindView(R.id.setting_color_src)
    ImageView mColorSrc;

    //是否需要刷新
    private boolean mNeedRefresh = false;
    //是否从主题颜色选择对话框返回
    private boolean mFromColorChoose = false;
    private final int RECREATE = 100;
    private Handler mRecreateHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECREATE)
                recreate();
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


        mModeSwitch.setChecked(!ThemeStore.isDay());
        mModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!mFromColorChoose) {
                    setNightMode(isChecked);
                }
                 else {
                    mFromColorChoose = false;
                }
            }
        });


        //初始化颜色
        final int color = ThemeStore.isDay() ? ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY) : ColorUtil.getColor(R.color.purple_782899);
        ((GradientDrawable)mColorSrc.getDrawable()).setColor(color);
        ButterKnife.apply( new ImageView[]{findView(R.id.setting_eq_arrow),findView(R.id.setting_feedback_arrow),
                findView(R.id.setting_about_arrow),findView(R.id.setting_update_arrow)}, new ButterKnife.Action<ImageView>(){
            @Override
            public void apply(@NonNull ImageView view, int index) {
                Drawable imgDrawable = view.getBackground();
                Theme.TintDrawable(imgDrawable,color);
                view.setImageDrawable(imgDrawable);
            }
        });

    }

    private void setNightMode(boolean isNight){
        ThemeStore.THEME_MODE = isNight ? ThemeStore.NIGHT : ThemeStore.DAY;
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColor();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColor();
        ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
        mNeedRefresh = true;
        mRecreateHandler.sendEmptyMessage(RECREATE);
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

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_container,
                R.id.setting_mode_container,R.id.setting_feedback_container,R.id.setting_about_container,
                R.id.setting_update_container,R.id.setting_eq_container})
    public void onClick(View v){
        switch (v.getId()){
            //文件过滤
            case R.id.setting_filter_container:
                startActivity(new Intent(SettingActivity.this,ScanActivity.class));
                break;
            //选择主色调
            case R.id.setting_color_container:
                startActivityForResult(new Intent(SettingActivity.this, ColorChooseDialog.class),0);
                break;
            //通知栏底色
            case R.id.setting_notify_container:
                try {
                    new MaterialDialog.Builder(this)
                            .title("通知栏底色")
                            .items(new String[]{getString(R.string.use_system_color),getString(R.string.use_black_color)})
                            .itemsCallbackSingleChoice(SharedPrefsUtil.getValue(SettingActivity.this,"setting","IsSystemColor",true) ? 0 : 1,
                                    new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    SharedPrefsUtil.putValue(SettingActivity.this,"setting","IsSystemColor",which == 0);
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
            //夜间模式
            case R.id.setting_mode_container:
                mModeSwitch.setChecked(!mModeSwitch.isChecked());
                break;
            //音效设置
            case R.id.setting_eq_container:
                Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                startActivityForResult(i, 0);
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
                UmengUpdateAgent.forceUpdate(SettingActivity.this);
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
                mRecreateHandler.sendEmptyMessage(RECREATE);
                if(mFromColorChoose)
                    mModeSwitch.setChecked(false);
            }

        }
    }

}
