package remix.myplayer.ui.activity;

import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.umeng.update.UmengUpdateAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SharedPrefsUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
public class SettingActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.setting_mode_switch)
    SwitchCompat mModeSwitch;


    private ImageView mSystem;
    private ImageView mBlack;
    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        initToolbar(mToolbar,"设置");


    }

    @OnClick ({R.id.setting_filter_container,R.id.setting_color_container,R.id.setting_notify_container,
                R.id.setting_feedback_container,R.id.setting_about_container,R.id.setting_update_container,
                R.id.setting_eq_container})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.setting_filter_container:
                //文件过滤
                startActivity(new Intent(SettingActivity.this,ScanActivity.class));
                break;
            case R.id.setting_color_container:
                //选择主色调
                break;
            case R.id.setting_notify_container:
                //通知栏底色
                try {
                    View notifycolor = LayoutInflater.from(SettingActivity.this).inflate(R.layout.popup_notifycolor,null);
                    boolean isSystem = SharedPrefsUtil.getValue(SettingActivity.this,"setting","IsSystemColor",true);
                    mSystem = (ImageView)notifycolor.findViewById(R.id.popup_notify_image_system);
                    mBlack = (ImageView)notifycolor.findViewById(R.id.popup_notify_image_black);
                    if(mSystem != null)
                        mSystem.setVisibility(isSystem ? View.VISIBLE : View.INVISIBLE);
                    if(mBlack != null)
                        mBlack.setVisibility(isSystem ? View.INVISIBLE : View.VISIBLE);

                    ColorListener listener = new ColorListener();
                    notifycolor.findViewById(R.id.notifycolor_system).setOnClickListener(listener);
                    notifycolor.findViewById(R.id.notifycolor_black).setOnClickListener(listener);

                    mAlertDialog = new AlertDialog.Builder(SettingActivity.this).setView(notifycolor).create();
                    mAlertDialog.show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case R.id.setting_eq_container:
                //音效设置
                Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                startActivityForResult(i, 0);
                break;
            case R.id.setting_feedback_container:
                //意见与反馈
                startActivity(new Intent(SettingActivity.this,FeedBakActivity.class));
                break;
            case R.id.setting_about_container:
                //关于我们
                startActivity(new Intent(SettingActivity.this,AboutActivity.class));
                break;
            case R.id.setting_update_container:
                //检查更新
                UmengUpdateAgent.forceUpdate(SettingActivity.this);
        }
    }

    class ColorListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            boolean isSystem = v.getId() == R.id.notifycolor_system;
            if(mSystem != null)
                mSystem.setVisibility(isSystem ? View.VISIBLE : View.INVISIBLE);
            if(mBlack != null)
                mBlack.setVisibility(isSystem ? View.INVISIBLE : View.VISIBLE);
            SharedPrefsUtil.putValue(SettingActivity.this,"setting","IsSystemColor",isSystem);
            //更新通知栏
            sendBroadcast(new Intent(Constants.NOTIFY));
            if(mAlertDialog != null && mAlertDialog.isShowing()){
                mAlertDialog.dismiss();
            }
        }
    }
}
