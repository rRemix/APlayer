package remix.myplayer.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.EQSeekBar;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by taeja on 16-4-13.
 */
public class EQActivity extends BaseAppCompatActivity {
    private final static String TAG = "EQActivity";
    private Equalizer mEqualizer;
//    private ArrayList<Short> mPreSettings = new ArrayList<>();
    private short mBandNumber;
    private short mMaxEQLevel;
    private short mMinEQLevel;
    private ArrayList<Integer> mCenterFres = new ArrayList<>();
    private HashMap<String,Short> mPreSettings = new HashMap<>();
    private ArrayList<EQSeekBar> mEQSeekBars = new ArrayList<>();
    private TextView mText;
    private ArrayList<Short> mBandLevels = new ArrayList<>();
    private SwitchCompat mSwitch;
    private ArrayList<Short> mBandFrequencys = new ArrayList<>();
    private boolean mEnable = false;
    private boolean mInitialEnable = false;
    private BassBoost mBassBoost;
    private Virtualizer mVirtualizer;
    private SeekBar mSeekbar1;
    private SeekBar mSeekbar2;
    private short mBassBoostLevel;
    private short mVirtualizeLevel;
    private EnableReceiver mEnableReceiver;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            for(int i = 0 ; i < mEQSeekBars.size() ;i++){
                int temp = mBandFrequencys.get(i);
                setSeekBarProgress(mEQSeekBars.get(i),temp);
                mEQSeekBars.get(i).setEnabled(mEnable);
                if (mEnable){
                    mEqualizer.setBandLevel((short)i,(short)temp);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mEnableReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eq);

        int AudioSessionId = MusicService.getMediaPlayer().getAudioSessionId();
        Log.d(TAG,"AudioSessionId:" + AudioSessionId);
        if(AudioSessionId  == 0) {
            return;
        }

        mEnableReceiver = new EnableReceiver();
        registerReceiver(mEnableReceiver,new IntentFilter(Constants.EQENABLE_ACTION));

        //是否启用音效设置
        mEnable = SharedPrefsUtil.getValue(this,"setting","EnableEQ",false) & Global.getHeadsetOn();
        mInitialEnable = mEnable;
        //EQ
        mEqualizer = new Equalizer(0, AudioSessionId);
        mEqualizer.setEnabled(mEnable);
        //重低音
        mBassBoost = new BassBoost(0,AudioSessionId);
        mBassBoost.setEnabled(mEnable);
        mBassBoostLevel = (short)SharedPrefsUtil.getValue(this,"setting","BassBoostLevel",0);
        if(mEnable && mBassBoost.getStrengthSupported()){
            mBassBoost.setStrength(mBassBoostLevel);
        }
        //环绕音效
        mVirtualizer = new Virtualizer(0,AudioSessionId);
        mVirtualizeLevel = (short)SharedPrefsUtil.getValue(this,"setting","VirtualizeLevel",0);
        mVirtualizer.setEnabled(mEnable);
        if(mEnable && mVirtualizer.getStrengthSupported()){
            mVirtualizer.setStrength(mVirtualizeLevel);
        }

        mSeekbar1 = (SeekBar)findViewById(R.id.bass);
        mSeekbar1.setProgress(mBassBoostLevel);
        mSeekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mBassBoost.getStrengthSupported() && progress > 0 && progress < 1000){
                    mBassBoost.setStrength((short)progress);
                    mBassBoostLevel = (short)progress;
                    SharedPrefsUtil.putValue(EQActivity.this,"setting","BassBoostLevel",progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        mSeekbar2 = (SeekBar)findViewById(R.id.virtualizer);
        mSeekbar2.setProgress(mVirtualizeLevel);
        mSeekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mVirtualizer.getStrengthSupported() && progress > 0 && progress < 1000){
                    mVirtualizer.setStrength((short)progress);
                    mVirtualizeLevel = (short)progress;
                    SharedPrefsUtil.putValue(EQActivity.this,"setting","VirtualizeLevel",progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //得到当前Equalizer引擎所支持的控制频率的标签数目。
        mBandNumber = mEqualizer.getNumberOfBands();


        //得到之前存储的每个频率的db值
        for(int i = 0 ; i < mBandNumber; i++){
            mBandFrequencys.add((short)(SharedPrefsUtil.getValue(this,"setting","Band" + i,0)));
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

        mText = (TextView)findViewById(R.id.text);

        //初始化switch
        mSwitch = (SwitchCompat)findViewById(R.id.eq_switch);
        mSwitch.setChecked(mEnable);
        mSwitch.setThumbResource(mEnable ? R.drawable.timer_btn_seleted_btn : R.drawable.timer_btn_normal_btn);
        mSwitch.setTrackResource(mEnable ? R.drawable.timer_btn_seleted_focus : R.drawable.timer_btn_normal_focus);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!Global.getHeadsetOn() && isChecked){
                    Toast.makeText(EQActivity.this,"请插入耳机",Toast.LENGTH_SHORT).show();
                    mSwitch.setChecked(false);
                    return;
                }
                UpdateEnable(isChecked);
            }
        });


        //初始化EqSeekbar
        mEQSeekBars.add((EQSeekBar)findViewById(R.id.EQSeekbar1));
        mEQSeekBars.add((EQSeekBar)findViewById(R.id.EQSeekbar2));
        mEQSeekBars.add((EQSeekBar)findViewById(R.id.EQSeekbar3));
        mEQSeekBars.add((EQSeekBar)findViewById(R.id.EQSeekbar4));
        mEQSeekBars.add((EQSeekBar)findViewById(R.id.EQSeekbar5));

        for(int i = 0 ; i < mEQSeekBars.size() ;i++){
            EQSeekBar eqSeekBar = mEQSeekBars.get(i);
            eqSeekBar.setOnSeekBarChangeListener(new EQSeekbarOnChangeListener());
            eqSeekBar.setMax(mMaxEQLevel - mMinEQLevel);
            eqSeekBar.setTag(mCenterFres.get(i));
            int fre_temp = mCenterFres.get(i);
            String hz = fre_temp > 1000 ?  fre_temp / 1000 + "K" : fre_temp + "";
            eqSeekBar.setFreText(hz);

        }

        new Thread(){
            @Override
            public void run() {
                if(!(mEQSeekBars.size() > 0))
                    return;
                while (!mEQSeekBars.get(mEQSeekBars.size() - 1).isInit()){
                }
                Message msg = new Message();
                mHandler.sendMessage(msg);
            }
        }.start();

    }

    private void UpdateEnable(boolean enable) {

        mSwitch.setChecked(enable);
        mSwitch.setThumbResource(enable ? R.drawable.timer_btn_seleted_btn : R.drawable.timer_btn_normal_btn);
        mSwitch.setTrackResource(enable ? R.drawable.timer_btn_seleted_focus : R.drawable.timer_btn_normal_focus);
        mEnable = enable;
        SharedPrefsUtil.putValue(EQActivity.this,"setting","EnableEQ",enable);

        mBassBoost.setEnabled(mEnable);
        if(mBassBoost.getStrengthSupported()){
            mBassBoost.setStrength(mEnable ? mBassBoostLevel : 0);
        }

        mVirtualizer.setEnabled(mEnable);
        if(mVirtualizer.getStrengthSupported()){
            mVirtualizer.setStrength(mEnable ? mVirtualizeLevel : 0);
        }

        mEqualizer.setEnabled(mEnable);
        for(int i = 0 ; i < mEQSeekBars.size() ;i++){
            mEQSeekBars.get(i).setEnabled(enable);
            mEQSeekBars.get(i).setProgressColor(enable ?  Color.parseColor("#782899") :Color.parseColor("#FFC125"));
            mEqualizer.setBandLevel((short)i,enable ? mBandFrequencys.get(i) : 0);
        }

        mSeekbar1.setEnabled(mEnable);
        mSeekbar2.setEnabled(mEnable);
    }


    private void UpdateText(){
        StringBuilder stringBuilder = new StringBuilder();
        for (short i = 0; i < mEqualizer.getNumberOfBands(); i++) {
            stringBuilder.append((mEqualizer.getCenterFreq(i) / 1000) + "Hz:  " + mEqualizer.getBandLevel(i) + " ");
        }
        mText.setText(stringBuilder.toString());
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
                            Toast.makeText(EQActivity.this,"参数不合法: " + fre ,Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //设置db值
                        mEqualizer.setBandLevel((short)i,temp);
                        //db值存储到sp
                        SharedPrefsUtil.putValue(EQActivity.this,"setting","Band" + i,temp );
                        break;
                    }
                }
                UpdateText();
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


    private void setPreset(View v) {
        if(!mEnable)
            return;
        String tag = v.getTag().toString();
        try {
            if(tag != null && !tag.equals("")) {
                short preset = mPreSettings.get(tag);
                if (preset >= 0 && preset < mPreSettings.size())
                    //应用预设音效
                    mEqualizer.usePreset(preset);
                //设置每个频率的DB值
                for(short i = 0 ; i < mEqualizer.getNumberOfBands(); i++){
                    int temp = mEqualizer.getBandLevel(i);
                    if(temp >= mMinEQLevel && temp <= mMaxEQLevel) {
                        //db值存储到sp
                        SharedPrefsUtil.putValue(EQActivity.this,"setting","Band" + i,temp);
                        setSeekBarProgress(mEQSeekBars.get(i),temp);
                    }
                }
            }
            UpdateText();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setSeekBarProgress(EQSeekBar seekBar,int frequency){
        if(frequency <= mBandLevels.get(mBandLevels.size() / 2))
            frequency = mMaxEQLevel - frequency;
        else
            frequency = Math.abs(mMinEQLevel) - frequency;
        seekBar.setProgress(frequency);
    }

    public void onRock(View v){
        setPreset(v);
    }


    public void onPop(View v){
        setPreset(v);
    }

    public void onClassical(View v){
        setPreset(v);
    }

    public void onBass(View v){
        setPreset(v);
    }

    
    class EnableReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Constants.EQENABLE_ACTION)){
                mInitialEnable = mEnable;
                UpdateEnable(intent.getExtras().getBoolean("IsHeadsetOn") && mInitialEnable);
            }
        }
    }
}
