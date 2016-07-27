package remix.myplayer.fragment;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.thumb.SearchCover;

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
    private MP3Item mInfo;
    private TranslateAnimation mLeftAnimation;
    private ScaleAnimation mScaleAnimation;
    private TranslateAnimation mRightAnimation;
    private static final int COVERINDB = 0;
    private static final int COVERINCACHE =1;
    private static final int NOCOVER = 2;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mImage.setImageURI((Uri)msg.obj);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (MP3Item)getArguments().getSerializable("MP3Item");
        View rootView = inflater.inflate(R.layout.fragment_cover,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

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
//                    setImage();
                    mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
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
    public void UpdateCover(MP3Item info, boolean withAnim){
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
//            setImage();
            mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
        }
    }

    /**
     * 判断该专辑id在本地数据库是否有封面
     * 没有则查找缓存目录是否有下载
     * 没有则下载
     */
    private void setImage(){
        try {
            final Message msg = new Message();
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId());

            boolean exist = CommonUtil.isAlbumThumbExistInDB(uri);
            if(false){
                mImage.setImageURI(uri);
            } else {
                //查找缓存目录是否有，没有则去下载
                mImage.setImageURI(Uri.parse(new SearchCover(mInfo.getDisplayname(),mInfo.getArtist(),SearchCover.COVER).getImgUrl()));
//                String coverCahcePath = CommonUtil.getCoverInCache(mInfo.getAlbumId());
//                if(coverCahcePath != null && !coverCahcePath.equals("")){
////                    msg.obj = Uri.parse("file://" + coverCahcePath);
////                    mHandler.sendMessage(msg);
//                    mImage.setImageURI(Uri.parse("file://" + coverCahcePath));
//                    return;
//                }
//                //下载
//                new Thread(){
//                    @Override
//                    public void run() {
//                        String coverPath = CommonUtil.downAlbumCover(mInfo.getDisplayname(),mInfo.getArtist(),mInfo.getAlbumId());
//                        msg.obj = Uri.parse("file://" + coverPath);
//                        mHandler.sendMessage(msg);
//                    }
//                }.start();

            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }

}
