package remix.myplayer.listeners;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import remix.myplayer.R;
import remix.myplayer.fragments.MainFragment;

/**
 * Created by Remix on 2015/11/29.
 */

/**
 * 监听ViewPager
 */
public class ViewPagerListener implements ViewPager.OnPageChangeListener {
    private ImageView mImage= null;
    private int mIndex = 0;
    private int mTabWidth = 0;
    private int mImageWidth = 0;
    private int mOffSetX;
    private Context mContext;
    public ViewPagerListener(Context context,ImageView image,int index,int tabcount) {
        mContext = context;
        mImage = image;
        mIndex = index;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;// 获取分辨率宽度
        mImageWidth = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.indicator).getWidth();// 获取图片宽度
        mTabWidth = screenW / tabcount;
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
        //根据当前索引，移动ImageView
        mIndex = position;
        for(int i = 0 ; i < MainFragment.mTextViews.length ;i++){
            if(i == position)
                MainFragment.mTextViews[i].setTextColor(mContext.getResources().getColor(R.color.text_allsong_sel));
            else
                MainFragment.mTextViews[i].setTextColor(mContext.getResources().getColor(R.color.text_allsong_unsel));
        }
        Animation animation = new TranslateAnimation(mTabWidth * mIndex, mTabWidth * position, 0, 0);
        animation.setDuration(2000);
        animation.setFillAfter(true);
        mImage.startAnimation(animation);
    }
    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
