package remix.myplayer.fragments;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 *
 */
public class CoverFragment extends Fragment {
    private SimpleDraweeView mImage;
    private MP3Info mInfo;
    private TranslateAnimation mLeftAnimation;
    private ScaleAnimation mScaleAnimation;
    private TranslateAnimation mRightAnimation;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (MP3Info)getArguments().getSerializable("MP3Info");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mImage = (SimpleDraweeView)rootView.findViewById(R.id.cover_image);
        if(mInfo != null)
            mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));

        if(mLeftAnimation == null || mScaleAnimation == null || mRightAnimation == null) {
            //往左侧消失的动画
            mLeftAnimation = (TranslateAnimation) AnimationUtils.loadAnimation(getContext(),R.anim.cover_left_out);
            //往右侧消失的动画
            mRightAnimation = (TranslateAnimation)AnimationUtils.loadAnimation(getContext(),R.anim.cover_right_out);
            //中心方法的动画
            mScaleAnimation = (ScaleAnimation) AnimationUtils.loadAnimation(getContext(),R.anim.cover_center_in);

            Animation.AnimationListener listener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    //当消失的动画播放完毕后，设置新的封面背景，并播放中心放大的动画
                    mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
//                    if(mNewBitmap != null)
//                        mImage.setImageBitmap(mNewBitmap);
//                    else
//                        mImage.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.no_art_normal));
                    mImage.startAnimation(mScaleAnimation);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            };
            mLeftAnimation.setAnimationListener(listener);
            mRightAnimation.setAnimationListener(listener);

        }
        return rootView;

    }

    /**
     * 操作为上一首歌曲时，显示往左侧消失的动画
     *       下一首歌曲时，显示往右侧消失的动画
     * @param info 需要更新的歌曲
     * @param withAnim 是否需要动画
     */
    public void UpdateCover(MP3Info info,boolean withAnim){
        if(!isAdded())
            return;
        if (mImage == null)
            return;
        if((mInfo = info) == null)
            return;

        if(withAnim){
            //根据操作是上一首还是下一首播放动画
            int operation = Global.getOperation();
            if(operation == Constants.PREV)
                mImage.startAnimation(mRightAnimation);
            else if (operation == Constants.NEXT || operation == Constants.PLAYSELECTEDSONG)
                mImage.startAnimation(mLeftAnimation);
        } else {
            //如果不需要动画，直接设置背景
            mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
        }
    }
}
