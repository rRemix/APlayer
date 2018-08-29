package remix.myplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.misc.AnimationUrl;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.menu.CtrlButtonListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.ui.activity.base.BaseActivity;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 底部控制的Fragment
 */
public class BottomActionBarFragment extends BaseMusicFragment {
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
    private Rect mCoverRect;
    //图片路径
    private AnimationUrl mAnimUrl = new AnimationUrl();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = BottomActionBarFragment.class.getSimpleName();
    }

    @SuppressLint("ClickableViewAccessibility")
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

        //手势检测
        mGestureDetector = new GestureDetector(mContext,new GestureListener(this));
        mBottomActionBar.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));

        //播放按钮
        CtrlButtonListener listener = new CtrlButtonListener(getContext());
        mPlayButton.setOnClickListener(listener);
        mPlayNext.setOnClickListener(listener);

        //获取封面位置信息
        mCover.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mCover.getViewTreeObserver().removeOnPreDrawListener(this);
                //数据失效重新获取位置信息
                if(mCoverRect == null || mCoverRect.width() <= 0 || mCoverRect.height() <= 0){
                    mCoverRect = new Rect();
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
            new LibraryUriRequest(mCover,
                    getSearchRequestWithAlbumType(song),
                    new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()){
                @Override
                public void onSuccess(String result) {
                    super.onSuccess(result);
                    mAnimUrl.setAlbumId(song.getAlbumId());
                    mAnimUrl.setUrl(result);
                }

                @Override
                public void onError(String errMsg) {
                    super.onError(errMsg);
                    mAnimUrl.setAlbumId(-1);
                    mAnimUrl.setUrl("");
                }
            }.load();
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


    public void startPlayerActivity(){
        if(MusicService.getCurrentMP3() == null)
            return;
        Intent intent = new Intent(mContext, PlayerActivity.class);
        Bundle bundle = new Bundle();
        intent.putExtras(bundle);
        intent.putExtra("FromActivity",true);
        intent.putExtra("Rect",mCoverRect);
        intent.putExtra("AnimUrl",mAnimUrl);

        Activity activity = getActivity();
        if (activity != null && !((BaseActivity)activity).isDestroyed() ) {
            ActivityOptionsCompat options = ActivityOptionsCompat
                    .makeSceneTransitionAnimation(getActivity(), mCover, "image");
            ActivityCompat.startActivity(mContext, intent, options.toBundle());
        }
    }

    private GestureDetector mGestureDetector;
    private static final String TAG = "GestureListener";
    static class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final WeakReference<BottomActionBarFragment> mReference;
        GestureListener(BottomActionBarFragment fragment) {
            super();
            mReference = new WeakReference<>(fragment);
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            LogUtil.e(TAG, "onDoubleTap");
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            LogUtil.e(TAG, "onDoubleTapEvent");
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            LogUtil.e(TAG, "onSingleTapConfirmed");
            if(mReference.get() != null)
                mReference.get().startPlayerActivity();
            return true;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            LogUtil.e(TAG, "onContextClick");
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            LogUtil.e(TAG, "onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            LogUtil.e(TAG, "onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            LogUtil.e(TAG, "onSingleTapUp");
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            LogUtil.e(TAG, "onScroll");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            LogUtil.e(TAG, "onLongPress");
        }

        private static final int Y_THRESHOLD = DensityUtil.dip2px(App.getContext(),10);
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            LogUtil.e(TAG, "onFling  " + "Y1: " + e1.getY() + " Y2: " + e2.getY());
            if(mReference.get() != null && velocityY < 0 && e1.getY() - e2.getY() > Y_THRESHOLD)
                mReference.get().startPlayerActivity();
            return true;
        }
    }

}
