package remix.myplayer.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.listeners.CtrlButtonListener;
import remix.myplayer.ui.BottomActionBar;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2015/12/1.
 */
public class BottomActionBarFragment extends Fragment{
    private ImageButton playButton;
    private TextView title;
    private TextView artist;
    private BottomActionBar mBottomActionBar;
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
        mBottomActionBar = (BottomActionBar)rootView.findViewById(R.id.bottom_action_bar);
        //初始化底部三个按钮
        playButton = (ImageButton)rootView.findViewById(R.id.playbar_play);
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        playButton.setOnClickListener(listener);
        //初始化底部标题与歌手
        title = (TextView)rootView.findViewById(R.id.bottom_title);
        artist = (TextView)rootView.findViewById(R.id.bottom_artist);
        return rootView;
    }
    public void UpdateBottomStatus(MP3Info mp3Info,boolean isPlaying) {
        if(mp3Info != null) {
            String strtitle = mp3Info.getDisplayname();
            String strartist = mp3Info.getArtist();
            String stralbum = mp3Info.getAlbum();
            title.setText(strtitle);
            artist.setText(strartist);
        }
        if(isPlaying)
            playButton.setImageResource(R.drawable.bf_btn_stop);
        else
            playButton.setImageResource(R.drawable.bf_but_play);
    }
}
