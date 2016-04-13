package remix.myplayer.activities;

import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import remix.myplayer.R;
import remix.myplayer.services.MusicService;

/**
 * Created by taeja on 16-4-13.
 */
public class EQActivity extends BaseAppCompatActivity {
    private final static String TAG = "EQActivity";
    private Equalizer mEqualizer;
//    private ArrayList<Short> mPreSettings = new ArrayList<>();
    private short mBands;
    private short mMaxEQLevel;
    private short mMinEQLevel;
    private ArrayList<Integer> mCenterFres = new ArrayList<>();
    private HashMap<String,Short> mPreSettings = new HashMap<>();
    private SeekBar mSeekBar1;
    private SeekBar mSeekBar2;
    private SeekBar mSeekBar3;
    private SeekBar mSeekBar4;
    private SeekBar mSeekBar5;
    private TextView mText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eq);

        int AudioSessionId = MusicService.getMediaPlayer().getAudioSessionId();
        Log.d(TAG,"AudioSessionId:" + AudioSessionId);
        if(AudioSessionId  == 0){
            return;
        }
        mEqualizer = new Equalizer(0, MusicService.getMediaPlayer().getAudioSessionId());
        mEqualizer.setEnabled(true);

        //得到当前Equalizer引擎所支持的控制频率的标签数目。
        mBands = mEqualizer.getNumberOfBands();

        //得到的最小频率
        mMinEQLevel = mEqualizer.getBandLevelRange()[0];
        //得到的最大频率
        mMaxEQLevel = mEqualizer.getBandLevelRange()[1];
        for (short i = 0; i < mBands; i++) {
            //通过标签可以顺次的获得所有支持的频率的名字比如 60Hz 230Hz
            mCenterFres.add(mEqualizer.getCenterFreq(i) / 100);
        }
        ArrayList<String> list = new ArrayList<>();
        for(short i = 0 ; i < mEqualizer.getNumberOfPresets() ; i++){
            String presetname = mEqualizer.getPresetName(i);
            mPreSettings.put(mEqualizer.getPresetName(i),i);
        }

        mText = (TextView)findViewById(R.id.text);

        mSeekBar1 = (SeekBar)findViewById(R.id.seekbar_100);
        mSeekBar2 = (SeekBar)findViewById(R.id.seekbar_500);
        mSeekBar3 = (SeekBar)findViewById(R.id.seekbar_1k);
        mSeekBar4 = (SeekBar)findViewById(R.id.seekbar_4k);
        mSeekBar5 = (SeekBar)findViewById(R.id.seekbar_16k);

        mSeekBar1.setOnSeekBarChangeListener(new MySeekbarListener());
        mSeekBar2.setOnSeekBarChangeListener(new MySeekbarListener());
        mSeekBar3.setOnSeekBarChangeListener(new MySeekbarListener());
        mSeekBar4.setOnSeekBarChangeListener(new MySeekbarListener());
        mSeekBar5.setOnSeekBarChangeListener(new MySeekbarListener());


        mSeekBar1.setMax(mMaxEQLevel - mMinEQLevel);
        mSeekBar2.setMax(mMaxEQLevel - mMinEQLevel);
        mSeekBar3.setMax(mMaxEQLevel - mMinEQLevel);
        mSeekBar4.setMax(mMaxEQLevel - mMinEQLevel);
        mSeekBar5.setMax(mMaxEQLevel - mMinEQLevel);


        mSeekBar1.setTag(mCenterFres.get(0));
        mSeekBar2.setTag(mCenterFres.get(1));
        mSeekBar3.setTag(mCenterFres.get(2));
        mSeekBar4.setTag(mCenterFres.get(3));
        mSeekBar5.setTag(mCenterFres.get(4));

    }

    private void updateText(){
        StringBuilder stringBuilder = new StringBuilder();
        for (short i = 0; i < mEqualizer.getNumberOfBands(); i++) {
            stringBuilder.append((mEqualizer.getCenterFreq(i) / 1000) + " Hz:  " + mEqualizer.getBandLevel(i) + "\n");
        }
        mText.setText(stringBuilder.toString());
    }

    class MySeekbarListener implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int fre = 0;
            try {
                fre = Integer.valueOf(seekBar.getTag().toString());
            } catch (Exception e){
                e.printStackTrace();
            }

            for(int i = 0 ; i < mCenterFres.size() ; i++){
                if(fre == mCenterFres.get(i)){
                    short temp = (short)(progress - Math.abs(mMinEQLevel));
                    if(temp > mMaxEQLevel || temp < mMinEQLevel){
                        Toast.makeText(EQActivity.this,"参数不合法: " + fre ,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mEqualizer.setBandLevel((short)i,temp);
                    break;
                }
            }
            updateText();

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private void setPreset(View v) {
        String tag = v.getTag().toString();
        try {
            if(tag != null && !tag.equals("")) {
                short preset = mPreSettings.get(tag);
                if (preset >= 0 && preset < mPreSettings.size())
                    mEqualizer.usePreset(preset);
            }
            updateText();
        } catch (Exception e){
            e.printStackTrace();
        }
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

}
