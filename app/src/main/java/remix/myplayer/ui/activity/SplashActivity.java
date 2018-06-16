package remix.myplayer.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.service.MusicService.ACTION_LOAD_FINISH;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashActivity extends BaseActivity {
    private BroadcastReceiver mLoadReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        mLoadReceiver = new LoadFinishReceiver();
        registerReceiver(mLoadReceiver,new IntentFilter(MusicService.ACTION_LOAD_FINISH));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.unregisterReceiver(this,mLoadReceiver);
    }

    private class LoadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent receive) {
            LogUtil.d("StartAPlayer","receiveBroadcast");
            if(ACTION_LOAD_FINISH.equals(receive != null ? receive.getAction() : "")){


                final int sessionId = MusicService.getMediaPlayer().getAudioSessionId();
                if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
                    Toast.makeText(mContext,getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show();
                    return;
                }
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                if(Util.isIntentAvailable(mContext,audioEffectIntent)){
                    Intent mainIntent = new Intent(mContext,MainActivity.class);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Intent[] intents = new Intent[2];
                    intents[0] = mainIntent;
                    intents[1] = audioEffectIntent;
                    startActivities(intents);
                    finish();
//                    startActivityForResult(audioEffectIntent, REQUEST_EQ);
                } else {
                    startActivity(new Intent(mContext,MainActivity.class));
                    finish();
                }

            }
        }
    }

    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    public void onResume() {
        MobclickAgent.onPageStart(SplashActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SplashActivity.class.getSimpleName());
        super.onPause();
    }

}
