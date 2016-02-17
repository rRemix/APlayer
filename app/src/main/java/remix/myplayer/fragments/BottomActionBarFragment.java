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
import remix.myplayer.utils.MP3Info;

/**
 * Created by Remix on 2015/12/1.
 */
public class BottomActionBarFragment extends Fragment{
    private ImageButton prevButton;
    private ImageButton playButton;
    private ImageButton nextButton;
    private TextView title;
    private TextView artist;
    private BottomActionBar mBottomActionBar;
    public static BottomActionBarFragment mInstance;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        //注册recevier
//        bottomReceiver = new BottomReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(CommonUtil.UPDATE_ACTION);
//        getContext().registerReceiver(bottomReceiver,filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        getContext().unregisterReceiver(bottomReceiver);
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

//    @Override
//    public void onClick(View v) {
//        Intent intent = new Intent(CommonUtil.CTL_ACTION);
//        switch (v.getId())
//        {
//            case R.id.bottom_actionbar_previous:
//                intent.putExtra("Control", CommonUtil.PREV);
//                break;
//            case R.id.bottom_actionbar_next:
//                intent.putExtra("Control", CommonUtil.NEXT);
//                break;
//            case R.id.bottom_actionbar_play:
//                intent.putExtra("Control", CommonUtil.PLAY);
//                break;
//        }
//        getContext().sendBroadcast(intent);
//    }

    public void UpdateBottomStatus(MP3Info mp3Info,boolean isPlaying)
    {
            //更新底部信息
//            boolean isPlaying = intent.getBooleanExtra("Status",false);
//            String strtitle = intent.getStringExtra("Title");
//            String strartist = intent.getStringExtra("Artist");
//            String album = intent.getStringExtra("Album");
//
        if(mp3Info != null)
        {
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
