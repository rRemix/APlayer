package remix.myplayer.fragment;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.MediaStoreUtil;

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
    SimpleDraweeView mCover;

    //保存封面位置信息
    private static Rect mCoverRect;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        mBottomActionBar = (RelativeLayout)rootView.findViewById(R.id.bottom_action_bar);
        //点击打开播放界面
        mBottomActionBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MusicService.getCurrentMP3() == null)
                    return;
                Intent intent = new Intent(v.getContext(), PlayerActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("MP3Item",MusicService.getCurrentMP3());
                intent.putExtras(bundle);
                intent.putExtra("isPlay",MusicService.isPlay());
                intent.putExtra("FromActivity",true);
                intent.putExtra("Rect",mCoverRect);

                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(getActivity(), mCover, "image");
                ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
            }
        });
        //播放按钮
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        mPlayButton.setOnClickListener(listener);

        //获取封面位置信息
        mCover.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mCover.getViewTreeObserver().removeOnPreDrawListener(this);
                mCoverRect = new Rect();
                mCover.getGlobalVisibleRect(mCoverRect);
                //数据失效重新获取位置信息
                if(mCoverRect == null || mCoverRect.width() <= 0 || mCoverRect.height() <= 0){
                    mCover.getGlobalVisibleRect(mCoverRect);
                }
                return true;
            }
        });
        return rootView;
    }
    //更新界面
    public void UpdateBottomStatus(MP3Item mp3Item, boolean isPlaying) {
        if(mp3Item == null)
            return;
        //歌曲名 艺术家
        if(mTitle != null)
            mTitle.setText(mp3Item.getTitle());
        if(mArtist != null)
            mArtist.setText(mp3Item.getArtist());
        //封面
        MediaStoreUtil.setImageUrl(mCover,mp3Item.getAlbumId());
        //设置按钮着色
        if(mPlayButton == null)
            return;
        if(isPlaying) {
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_stop,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));
        } else{
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_play,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));
        }
    }

    public static Rect getCoverRect(){
        return mCoverRect;
    }

}
