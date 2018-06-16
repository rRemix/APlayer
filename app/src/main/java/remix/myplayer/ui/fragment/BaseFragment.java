package remix.myplayer.ui.fragment;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.umeng.analytics.MobclickAgent;

import butterknife.Unbinder;
import remix.myplayer.App;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
public abstract class BaseFragment extends Fragment {
    protected Unbinder mUnBinder;
    protected Context mContext;
    protected String mPageName = BaseFragment.class.getSimpleName();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

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


    protected String getStringSafely(@StringRes int res){
        if(isAdded())
            return getString(res);
        else
            return App.getContext().getString(res);
    }

    protected String getStringSafely(@StringRes int res,Object... args){
        if(isAdded())
            return getString(res,args);
        else
            return App.getContext().getString(res,args);
    }

}
