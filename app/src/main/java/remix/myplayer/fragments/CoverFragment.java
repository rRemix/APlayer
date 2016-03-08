package remix.myplayer.fragments;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;


import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2015/12/2.
 */
public class CoverFragment extends Fragment {

    private ImageView mImage;
    private Bitmap mBitmap;
    private MP3Info mInfo;
    private TranslateAnimation mLeftAnimation;
    private ScaleAnimation mScaleAnimation;
    private TranslateAnimation mRightAnimation;
    private Bitmap mNewBitmap;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (MP3Info)getArguments().getSerializable("MP3Info");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mImage = (ImageView)rootView.findViewById(R.id.cover_image);
        if(mInfo != null && (mBitmap = DBUtil.CheckBitmapBySongId((int)mInfo.getId(),false)) != null)
            mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
        if(mLeftAnimation == null || mScaleAnimation == null || mRightAnimation == null)
        {
            mLeftAnimation = (TranslateAnimation) AnimationUtils.loadAnimation(getContext(),R.anim.cover_left_out);
            mRightAnimation = (TranslateAnimation)AnimationUtils.loadAnimation(getContext(),R.anim.cover_right_out);
            mScaleAnimation = (ScaleAnimation) AnimationUtils.loadAnimation(getContext(),R.anim.cover_center_in);

            Animation.AnimationListener listener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    if(mNewBitmap != null)
                        mImage.setImageBitmap(mNewBitmap);
                    else
                        mImage.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.no_art_normal));
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
    public void UpdateCover(Bitmap bitmap)
    {
        if(!isAdded())
            return;
        if (mImage == null)
            return;
        mNewBitmap = bitmap;
        if(AudioHolderActivity.mOperation == Constants.PREV)
            mImage.startAnimation(mRightAnimation);
        else if (AudioHolderActivity.mOperation == Constants.NEXT)
            mImage.startAnimation(mLeftAnimation);
    }
}
