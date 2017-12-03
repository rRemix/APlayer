package remix.myplayer.ui.fragment;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.request.AlbumUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.ColorUtil;

import static remix.myplayer.request.ImageUriRequest.LIST_IMAGE_SIZE;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 底部控制的Fragment
 */
public class BottomActionBarFragment extends BaseFragment{
    //播放与下一首按钮
    @BindView(R.id.playbar_play)
    ImageView mPlayButton;
    @BindView(R.id.playbar_next)
    ImageView mPlayNext;
    //歌曲名艺术家
    @BindView(R.id.bottom_title)
    TextView mTitle;
    @BindView(R.id.bottom_artist)
    TextView mArtist;
    @BindView(R.id.bottom_action_bar)
    RelativeLayout mBottomActionBar;
    @BindView(R.id.bottom_actionbar_root)
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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_actionbar,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        //设置整个背景着色
        Theme.TintDrawable(rootView,
                R.drawable.commom_playercontrols_bg,
                ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.day_background_color_3 : R.color.night_background_color_3));
        Theme.TintDrawable(mPlayNext,
                R.drawable.bf_btn_next,
                ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_323335 : R.color.white));

        //点击打开播放界面
        mBottomActionBar.setOnClickListener(v -> {
            if(MusicService.getCurrentMP3() == null)
                return;
            Intent intent = new Intent(v.getContext(), PlayerActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("Song",MusicService.getCurrentMP3());
            intent.putExtras(bundle);
            intent.putExtra("isPlay",MusicService.isPlay());
            intent.putExtra("FromActivity",true);
            intent.putExtra("Rect",mCoverRect);

            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(getActivity(), mCover, "image");
            ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
        });
        //播放按钮
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        mPlayButton.setOnClickListener(listener);
        mPlayNext.setOnClickListener(listener);

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
    public void updateBottomStatus(Song song, boolean isPlaying) {
        if(song == null)
            return;
        //歌曲名 艺术家
        if(mTitle != null)
            mTitle.setText(song.getTitle());
        if(mArtist != null)
            mArtist.setText(song.getArtist());
        //封面
        if(mCover != null)
            new AlbumUriRequest(mCover,
                    new Album(song.getAlbumId(),song.getAlbum(),0,song.getArtist()),
                    new RequestConfig.Builder(LIST_IMAGE_SIZE,LIST_IMAGE_SIZE).build()).load();
        //设置按钮着色
        if(mPlayButton == null)
            return;
        if(isPlaying) {
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_stop,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_323335 : R.color.white));
        } else{
            Theme.TintDrawable(mPlayButton,
                    R.drawable.bf_btn_play,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_323335 : R.color.white));
        }
    }

    public static Rect getCoverRect(){
        return mCoverRect;
    }

}
