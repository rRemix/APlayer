package remix.myplayer.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.umeng.analytics.MobclickAgent;

import butterknife.Unbinder;
import remix.myplayer.helper.DeleteHelper;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(this instanceof DeleteHelper.Callback)
            DeleteHelper.addCallback((DeleteHelper.Callback) this);
//        if(this instanceof LoaderManager.LoaderCallbacks){
//            getLoaderManager().initLoader(++LOADER_ID, null, (LoaderManager.LoaderCallbacks) this);
//        }
        return super.onCreateView(inflater,container,savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mUnBinder != null)
            mUnBinder.unbind();
        if(this instanceof DeleteHelper.Callback)
            DeleteHelper.removeCallback((DeleteHelper.Callback) this);

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
