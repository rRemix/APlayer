package remix.myplayer.ui.activity;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.EQSeekBar;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-4-13.
 */
public class EQActivity extends ToolbarActivity {
    private final static String TAG = "EQActivity";
    private static Equalizer mEqualizer;
    public static EQActivity mInstance;
    private static short mBandNumber = -1;
    private static short mMaxEQLevel = -1;
    private static short mMinEQLevel = -1;
    private static ArrayList<Integer> mCenterFres = new ArrayList<>();
    private static HashMap<String,Short> mPreSettings = new HashMap<>();
    private ArrayList<EQSeekBar> mEQSeekBars = new ArrayList<>();
    private static ArrayList<Short> mBandLevels = new ArrayList<>();

    @BindView(R.id.eq_switch)
    SwitchCompat mSwitch;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.eq_reset)
    Button mReset;

    private static ArrayList<Short> mBandFrequencys = new ArrayList<>();
    private static boolean mEnable = false;
    private static boolean mInitialEnable = false;
    private static BassBoost mBassBoost;
    private static Virtualizer mVirtualizer;


    private static short mBassBoostLevel;
    private static short mVirtualizeLevel;
    private static boolean mHasInitial = false;

    public static void Init(){
        new Thread(){
            @Override
            public void run() {
                int AudioSessionId = MusicService.getMediaPlayer().getAudioSessionId();
                LogUtil.d(TAG,"AudioSessionId:" + AudioSessionId);
                if(AudioSessionId  == 0) {
                    ToastUtil.show(APlayerApplication.getContext(),R.string.eq_initial_failed);
                    return;
                }
                //是否启用音效设置
                mEnable = SPUtil.getValue(APlayerApplication.getContext(),"Setting","EnableEQ",false) & Global.getHeadsetOn();
                mInitialEnable = SPUtil.getValue(APlayerApplication.getContext(),"Setting","InitialEnableEQ",false);

                //EQ
                mEqualizer = new Equalizer(0, AudioSessionId);
                mEqualizer.setEnabled(mEnable);
//                //重低音
//                mBassBoost = new BassBoost(0,AudioSessionId);
//                mBassBoost.setEnabled(mEnable);
//                mBassBoostLevel = (short)SPUtil.getValue(APlayerApplication.getContext(),"setting","BassBoostLevel",0);
//                if(mEnable && mBassBoost.getStrengthSupported()){
//                    mBassBoost.setStrength(mBassBoostLevel);
//                }
//                //环绕音效
//                mVirtualizer = new Virtualizer(0,AudioSessionId);
//                mVirtualizeLevel = (short)SPUtil.getValue(APlayerApplication.getContext(),"setting","VirtualizeLevel",0);
//                mVirtualizer.setEnabled(mEnable);
//                if(mEnable && mVirtualizer.getStrengthSupported()){
//                    mVirtualizer.setStrength(mVirtualizeLevel);
//                }

                //得到当前Equalizer引擎所支持的控制频率的标签数目。
                mBandNumber = mEqualizer.getNumberOfBands();

                //得到之前存储的每个频率的db值
                for(short i = 0 ; i < mBandNumber; i++){
                    short temp = (short)(SPUtil.getValue(APlayerApplication.getContext(),"Setting","Band" + i,0));
                    mBandFrequencys.add(temp);
                    if (mEnable){
                        mEqualizer.setBandLevel(i,temp);
                    }
                }

                //得到的最小频率
                mMinEQLevel = mEqualizer.getBandLevelRange()[0];
                //得到的最大频率
                mMaxEQLevel = mEqualizer.getBandLevelRange()[1];
                for (short i = 0; i < mBandNumber; i++) {
                    //通过标签可以顺次的获得所有支持的频率的名字比如 60Hz 230Hz
                    mCenterFres.add(mEqualizer.getCenterFreq(i) / 1000);
                }

                //获得所有预设的音效
                for(short i = 0 ; i < mEqualizer.getNumberOfPresets() ; i++){
                    mPreSettings.put(mEqualizer.getPresetName(i),i);
                }

                //获得所有频率值
                short temp = (short) ((mMaxEQLevel - mMinEQLevel) / 30);
                for(short i = 0 ; i < 31; i++){
                    mBandLevels.add((short)(1500 - (i * temp)));
                }
                //初始化完成
                mHasInitial = true;
            }

        }.start();

    }

    public void onResume() {
        MobclickAgent.onPageStart(EQActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(EQActivity.class.getSimpleName());
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!mHasInitial){
            ToastUtil.show(this,R.string.eq_initial_failed);
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eq);
        ButterKnife.bind(this);

        mInstance = this;

        setUpToolbar(mToolBar,getString(R.string.use_eq));

        //初始化switch
        ContextThemeWrapper ctw = new ContextThemeWrapper(this,Theme.getTheme());
        mSwitch = new SwitchCompat(ctw);
        Toolbar.LayoutParams toolbarLp = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toolbarLp.rightMargin = DensityUtil.dip2px(this,16);
        toolbarLp.gravity = Gravity.END;
        mSwitch.setLayoutParams(toolbarLp);
        mToolBar.addView(mSwitch);

        mSwitch.setChecked(mEnable);
//        Theme.TintDrawable(mLockScreenSwitch.getThumbDrawable(),ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryColorRes() : R.color.purple_782899));
//        Theme.TintDrawable(mLockScreenSwitch.getTrackDrawable(),ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryColorRes() : R.color.purple_782899));
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == mEnable)
                    return;

                if(!Global.getHeadsetOn()){
                    ToastUtil.show(EQActivity.this,R.string.plz_earphone);
                    mSwitch.setChecked(false);
                    return;
                }
                mInitialEnable = isChecked;
                SPUtil.putValue(EQActivity.this,"Setting","InitialEnableEQ",mInitialEnable);
                mEnable = isChecked;
                UpdateEnable(isChecked);
            }
        });

        //初始化重置按钮背景
        mReset.setBackground(Theme.getCorner(1.0f,5,0,ThemeStore.getAccentColor()));

        addEQSeekBar();

        for(int i = 0 ; i < mEQSeekBars.size() ;i++){
            int temp = mBandFrequencys.get(i);
            setSeekBarProgress(mEQSeekBars.get(i),temp);
            mEQSeekBars.get(i).setEnabled(mEnable);
        }
//        new Thread(){
//            @Override
//            public void run() {
//                if(!(mEQSeekBars.size() > 0))
//                    return;
//                while (!mEQSeekBars.get(mEQSeekBars.size() - 1).isInit()){
//                }
//                Message msg = new Message();
//                mHandler.sendMessage(msg);
//            }
//        }.startListen();

    }

    /**
     * 根据BandNumber数量，添加EQSeekBar
     */
    private void addEQSeekBar() {
        LinearLayout EQContainer = (LinearLayout)findViewById(R.id.eq_container);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( DensityUtil.dip2px(this,30), ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(DensityUtil.dip2px(this,20),0,DensityUtil.dip2px(this,20),0);
        for(int i = 0 ; i < mBandNumber ;i++){
            EQSeekBar eqSeekBar = new EQSeekBar(this);
            eqSeekBar.setLayoutParams(lp);

            eqSeekBar.setOnSeekBarChangeListener(new EQSeekbarOnChangeListener());
            eqSeekBar.setMax(mMaxEQLevel - mMinEQLevel);
            eqSeekBar.setTag(mCenterFres.get(i));
            int fre_temp = mCenterFres.get(i);
            String hz = fre_temp > 1000 ?  fre_temp / 1000 + "K" : fre_temp + "";
            eqSeekBar.setFreText(hz);
            int temp = mBandFrequencys.get(i);
            setSeekBarProgress(eqSeekBar,temp);
            eqSeekBar.setEnabled(mEnable);
            mEQSeekBars.add(eqSeekBar);
            EQContainer.addView(eqSeekBar);

        }
    }


    public void UpdateEnable(boolean enable) {
        mEnable = enable;
        SPUtil.putValue(EQActivity.this,"Setting","EnableEQ",enable);

        if(mSwitch != null) {
            mSwitch.setChecked(enable);
//            mLockScreenSwitch.setThumbResource(enable ? R.drawable.timer_btn_seleted_btn : R.drawable.timer_btn_normal_btn);
//            mLockScreenSwitch.setTrackResource(enable ? R.drawable.timer_btn_seleted_focus : R.drawable.timer_btn_normal_focus);
        }
//        mBassBoost.setEnabled(mEnable);
//        if(mBassBoost.getStrengthSupported()){
//            mBassBoost.setStrength(mEnable ? mBassBoostLevel : 0);
//        }
//
//        mVirtualizer.setEnabled(mEnable);
//        if(mVirtualizer.getStrengthSupported()){
//            mVirtualizer.setStrength(mEnable ? mVirtualizeLevel : 0);
//        }

        mEqualizer.setEnabled(mEnable);
        for(int i = 0 ; i < mEQSeekBars.size() ;i++){
            mEQSeekBars.get(i).setEnabled(enable);
            mEqualizer.setBandLevel((short)i,enable ? mBandFrequencys.get(i) : 0);
        }
    }

    class EQSeekbarOnChangeListener implements EQSeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(EQSeekBar seekBar, int position, boolean fromUser) {
            if(!seekBar.canDrag())
                return;
            try {
                int fre = Integer.valueOf(seekBar.getTag().toString());
                for(int i = 0 ; i < mCenterFres.size() ; i++){
                    if(fre == mCenterFres.get(i)){
                        short temp = (mBandLevels.get(position));
                        if(temp > mMaxEQLevel || temp < mMinEQLevel){
                            ToastUtil.show(EQActivity.this,getString(R.string.arg_illegal),fre);
                            return;
                        }
                        //设置db值
                        mEqualizer.setBandLevel((short)i,temp);
                        //db值存储到sp
                        SPUtil.putValue(EQActivity.this,"Setting","Band" + i,temp );
                        break;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        @Override
        public void onStartTrackingTouch(EQSeekBar seekBar) {
        }
        @Override
        public void onStopTrackingTouch(EQSeekBar seekBar) {
        }
    }


    private void setSeekBarProgress(EQSeekBar seekBar,int frequency){
        if(frequency <= mBandLevels.get(mBandLevels.size() / 2))
            frequency = mMaxEQLevel - frequency;
        else
            frequency = Math.abs(mMinEQLevel) - frequency;
        seekBar.setProgress(frequency);
    }

    //重置音效设置
    public void onReset(View v){
        if(!Global.getHeadsetOn()){
            ToastUtil.show(EQActivity.this,R.string.plz_earphone);
            return;
        }
        mInitialEnable = true;
        SPUtil.putValue(EQActivity.this,"Setting","InitialEnableEQ",mInitialEnable);
        UpdateEnable(true);
        if(mBandFrequencys != null)
            mBandFrequencys.clear();
        for(short i = 0 ; i < mEQSeekBars.size() ;i++){
            //设置db值
            mEqualizer.setBandLevel(i,(short) 0);
            //db值存储到sp
            SPUtil.putValue(EQActivity.this,"Setting","Band" + i,(short)0 );
            //设置seekbar进度
            setSeekBarProgress(mEQSeekBars.get(i),0);
            //存储每个频率的db值到内存
            mBandFrequencys.add((short)0);
        }
    }

    public static boolean getInitialEnable(){
        return mInitialEnable;
    }

    public static void setEnable(boolean enable){
        mEnable = enable;
    }

//    private void setPreset(View v) {
//        if(!mEnable)
//            return;
//        String tag = v.getTag().toString();
//        try {
//            if(tag != null && !tag.equals("")) {
//                short preset = mPreSettings.get(tag);
//                if (preset >= 0 && preset < mPreSettings.size())
//                    //应用预设音效
//                    mEqualizer.usePreset(preset);
//                //设置每个频率的DB值
//                for(short i = 0 ; i < mEqualizer.getNumberOfBands(); i++){
//                    int temp = mEqualizer.getBandLevel(i);
//                    if(temp >= mMinEQLevel && temp <= mMaxEQLevel) {
//                        //db值存储到SP
//                        SPUtil.putValue(EQActivity.this,"setting","Band" + i,temp);
//                        setSeekBarProgress(mEQSeekBars.get(i),temp);
//                    }
//                }
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }

//    public void onRock(View v){
//        setPreset(v);
//    }
//
//
//    public void onPop(View v){
//        setPreset(v);
//    }
//
//    public void onClassical(View v){
//        setPreset(v);
//    }
//
//    public void onBass(View v){
//        setPreset(v);
//    }

}
