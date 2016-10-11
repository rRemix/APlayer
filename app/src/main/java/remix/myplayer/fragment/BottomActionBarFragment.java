package remix.myplayer.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.util.ColorUtil;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 底部控制的Fragment
 */
public class BottomActionBarFragment extends BaseFragment{
    //播放与下一首按钮
    @BindView(R.id.playbar_play)
    ImageButton mPlayButton;
    //歌曲名艺术家
    @BindView(R.id.bottom_title)
    TextView mTitle;
    @BindView(R.id.bottom_artist)
    TextView mArtist;
    @BindView(R.id.bottom_action_bar)
    RelativeLayout mBottomActionBar;
    @BindView(R.id.bottom_actionbar_container)
    LinearLayout mRootView;
    @BindView(R.id.bottom_action_bar_cover)
    ImageView mCover;

    public static BottomActionBarFragment mInstance;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        mPageName = BottomActionBarFragment.class.getSimpleName();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_actionbar,container);
        mUnBinder = ButterKnife.bind(this,rootView);

        //设置整个背景着色
        Theme.TintDrawable(rootView,
                R.drawable.commom_playercontrols_bg,
                ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.day_background_color_3 : R.color.night_background_color_3));
        //设置向上按钮着色
        Theme.TintDrawable(mCover,
                R.drawable.home_btn_back,
                ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));

        mBottomActionBar = (RelativeLayout)rootView.findViewById(R.id.bottom_action_bar);
        //点击打开播放界面
        mBottomActionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MusicService.getCurrentMP3() == null)
                    return;
                Intent intent = new Intent(v.getContext(), AudioHolderActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("MP3Item",MusicService.getCurrentMP3());
                intent.putExtras(bundle);
                intent.putExtra("Isplay",MusicService.getIsplay());
                intent.putExtra("FromMainActivity",true);
                getContext().startActivity(intent);
            }
        });
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        mPlayButton.setOnClickListener(listener);

        return rootView;
    }
    //更新界面
    public void UpdateBottomStatus(MP3Item mp3Item, boolean isPlaying) {
        if(mp3Item != null) {
            mTitle.setText(mp3Item.getTitle());
            mArtist.setText(mp3Item.getArtist());
        }
        //设置按钮着色
        if(isPlaying) {
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_stop,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));
        } else {
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_play,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));
        }
    }

    class UpdateUIReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateBottomStatus(MusicService.getCurrentMP3(),MusicService.getIsplay());
        }
    }
}
