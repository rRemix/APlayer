package remix.myplayer.fragment;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.umeng.analytics.MobclickAgent;

import butterknife.Unbinder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
public class BaseFragment extends Fragment {
    protected Unbinder mUnBinder;
    protected static int LOADER_ID = 1;
    protected String mPageName = BaseFragment.class.getSimpleName();
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mUnBinder != null)
            mUnBinder.unbind();
    }


    public RecyclerView.Adapter getAdapter(){
        return null;
    }

    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(mPageName); //统计页面，"MainScreen"为页面名称，可自定义
    }

    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(mPageName);
    }
}
