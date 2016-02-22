package remix.myplayer.listeners;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import remix.myplayer.R;

/**
 * Created by Remix on 2015/11/29.
 */
public class ViewPagerListener implements ViewPager.OnPageChangeListener {
    private ImageView mImage= null;
    private int mIndex = 0;
    private int mTabWidth = 0;
    private int mImageWidth = 0;
    private int mOffSetX;

    public ViewPagerListener(Context context,ImageView image,int index) {
        mImage = image;
        mIndex = index;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;// 获取分辨率宽度
        mImageWidth = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.indicator).getWidth();// 获取图片宽度
        mTabWidth = screenW / 4;
        if (mImageWidth > mTabWidth) {
            mImage.getLayoutParams().width = mTabWidth;
            mImageWidth = mTabWidth;
        }
        mOffSetX = (mTabWidth - mImageWidth) / 2;
        mImage.setX(mOffSetX);
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mIndex = position;
        Animation animation = new TranslateAnimation(mTabWidth * mIndex, mTabWidth * position, 0, 0);
        animation.setDuration(2000);
        animation.setFillAfter(true);
        mImage.startAnimation(animation);
    }
    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
