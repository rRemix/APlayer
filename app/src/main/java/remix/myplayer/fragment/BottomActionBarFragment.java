package remix.myplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.model.MP3Item;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.service.MusicService;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 底部控制的Fragment
 */
public class BottomActionBarFragment extends Fragment{
    //播放与下一首按钮
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    //歌曲名艺术家
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
        //点击打开播放界面
        mBottomActionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AudioHolderActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("MP3Item",MusicService.getCurrentMP3());
                intent.putExtras(bundle);
                intent.putExtra("Isplay",MusicService.getIsplay());
                intent.putExtra("FromMainActivity",true);
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
    //更新界面
    public void UpdateBottomStatus(MP3Item mp3Item, boolean isPlaying) {
        if(mp3Item != null) {

            mTitle.setText(mp3Item.getDisplayname());
            mArtist.setText(mp3Item.getArtist());
        }
        if(isPlaying)
            mPlayButton.setImageResource(R.drawable.bf_btn_stop);
        else
            mPlayButton.setImageResource(R.drawable.bf_but_play);
    }
}
