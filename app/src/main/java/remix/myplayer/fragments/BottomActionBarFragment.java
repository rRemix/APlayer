package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.CtrlButtonListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.BottomActionBar;

/**
 * Created by Remix on 2015/12/1.
 */
public class BottomActionBarFragment extends Fragment{
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private TextView mTitle;
    private TextView mArtist;
    private RelativeLayout mBottomActionBar;
    public static BottomActionBarFragment mInstance;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_actionbar,container);
        mBottomActionBar = (RelativeLayout)rootView.findViewById(R.id.bottom_action_bar);
        mBottomActionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AudioHolderActivity.class);
                Bundle bundle = new Bundle();
                MP3Info temp = MusicService.getCurrentMP3();
                bundle.putSerializable("MP3Info",MusicService.getCurrentMP3());
                intent.putExtras(bundle);
                intent.putExtra("Isplay",MusicService.getIsplay());
                getContext().startActivity(intent);
            }
        });
        //初始化底部三个按钮
        mPlayButton = (ImageButton)rootView.findViewById(R.id.playbar_play);
        mNextButton = (ImageButton)rootView.findViewById(R.id.playbar_next);
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        mPlayButton.setOnClickListener(listener);
        mNextButton.setOnClickListener(listener);
        //初始化底部标题与歌手
        mTitle = (TextView)rootView.findViewById(R.id.bottom_title);
        mArtist = (TextView)rootView.findViewById(R.id.bottom_artist);
        return rootView;
    }
    public void UpdateBottomStatus(MP3Info mp3Info,boolean isPlaying) {
        if(mp3Info != null) {
            String strtitle = mp3Info.getDisplayname();
            String strartist = mp3Info.getArtist();
            String stralbum = mp3Info.getAlbum();
            mTitle.setText(strtitle);
            mArtist.setText(strartist);
        }
        if(isPlaying)
            mPlayButton.setImageResource(R.drawable.bf_btn_stop);
        else
            mPlayButton.setImageResource(R.drawable.bf_but_play);
    }
}
