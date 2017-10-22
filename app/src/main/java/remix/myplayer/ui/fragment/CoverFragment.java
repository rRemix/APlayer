package remix.myplayer.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.interfaces.OnInflateFinishListener;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 *
 */
public class CoverFragment extends BaseFragment {
    @BindView(R.id.cover_image)
    SimpleDraweeView mImage;
    @BindView(R.id.cover_shadow)
    SimpleDraweeView mShadow;
    @BindView(R.id.cover_container)
    View mCoverContainer;
    private Song mInfo;
    private int mWidth;
    private Uri mUri = Uri.EMPTY;
    private OnInflateFinishListener mInflateFinishListener;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = CoverFragment.class.getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (Song)getArguments().getSerializable("Song");
        mWidth = getArguments().getInt("Width");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        mImage.getHierarchy().setFailureImage(ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night);
        mImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mImage.getViewTreeObserver().removeOnPreDrawListener(this);
                if(mInflateFinishListener != null)
                    mInflateFinishListener.onViewInflateFinish(mImage);
                mShadow.setImageURI(Uri.parse("res://remix.myplayer/" + R.drawable.player_cover_shadow));
                return true;
            }
        });

        return rootView;
    }

    public void setInflateFinishListener(OnInflateFinishListener l){
        mInflateFinishListener = l;
    }

    /**
     * 操作为上一首歌曲时，显示往左侧消失的动画
     *       下一首歌曲时，显示往右侧消失的动画
     * @param info 需要更新的歌曲
     * @param withAnim 是否需要动画
     */
    public void UpdateCover(Song info, Uri uri, boolean withAnim){
        if(!isAdded())
            return;
        if (mImage == null || (mInfo = info) == null)
            return;

        mUri = uri;

        if(withAnim){
            int operation = Global.getOperation();

            int offsetX = (mWidth +  mImage.getWidth()) >> 1;
            final double startValue = 0;
            final double endValue = operation == Constants.PREV ? offsetX : -offsetX;

            //封面移动动画
            final Spring outAnim = SpringSystem.create().createSpring();
            outAnim.addListener(new SimpleSpringListener(){
                @Override
                public void onSpringUpdate(Spring spring) {
                    if(mCoverContainer == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) spring.getCurrentValue());
                }
                @Override
                public void onSpringAtRest(Spring spring) {
                    //显示封面的动画
                    if(mImage == null || spring == null)
                        return;
                    mCoverContainer.setTranslationX((float) startValue);
                    mImage.setImageURI(mUri);
                    final Spring inAnim = SpringSystem.create().createSpring();
                    inAnim.addListener(new SimpleSpringListener(){
                        @Override
                        public void onSpringUpdate(Spring spring) {
                            if(mImage == null || spring == null)
                                return;
                            mCoverContainer.setScaleX((float) spring.getCurrentValue());
                            mCoverContainer.setScaleY((float) spring.getCurrentValue());
                        }
                    });
                    inAnim.setCurrentValue(0.85);
                    inAnim.setEndValue(1);
                }
            });
            outAnim.setOvershootClampingEnabled(true);
            outAnim.setCurrentValue(startValue);
            outAnim.setEndValue(endValue);
        } else {
            mImage.setImageURI(mUri);
//            mShadow.setVisibility(ThemeStore.isDay() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * activity退出时隐藏封面
     */
    public void hideImage(){
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.INVISIBLE);
    }

    /**
     * activity入场动画播放完毕时显示封面
     */
    public void showImage(){
        if(mImage != null)
            mImage.setVisibility(View.VISIBLE);
        if(mCoverContainer != null)
            mCoverContainer.setVisibility(View.VISIBLE);
    }

}
