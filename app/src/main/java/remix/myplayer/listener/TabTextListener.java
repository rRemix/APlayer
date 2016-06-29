package remix.myplayer.listener;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Created by Remix on 2015/11/29.
 */
public class TabTextListener implements View.OnClickListener {
    private int mIndex = 0;
    private ViewPager mPager = null;
    public TabTextListener(ViewPager pager,int index) {
        mIndex = index;
        mPager = pager;
    }
    @Override
    public void onClick(View v) {
        mPager.setCurrentItem(mIndex);
    }
}
