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
    
//    /**
//     *
//     * @param multiChoice
//     * @param view
//     * @param tag
//     */
//    protected boolean itemAddorRemoveWithClick(MultiChoice multiChoice,View view,String tag){
//        if(multiChoice.mIsShow && getUserVisibleHint() && multiChoice.TAG.equals(tag)){
//            multiChoice.mIsShow = true;
//            multiChoice.RemoveOrAddView(view);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     *
//     * @param multiChoice
//     * @param view
//     * @param tag
//     */
//    protected void itemAddorRemoveWithLongClick(MultiChoice multiChoice,View view,String tag){
//        if(!multiChoice.mIsShow && getUserVisibleHint() && multiChoice.TAG.equals("")){
//            multiChoice.RemoveOrAddView(view);
//            multiChoice.TAG = tag;
//            multiChoice.mIsShow = true;
////            multiChoice.updateOptionsMenu(true);
//
//            return;
//        }
//        if(multiChoice.mIsShow && getUserVisibleHint() && multiChoice.TAG.equals(tag)){
//            multiChoice.RemoveOrAddView(view);
//        }
//
//    }
}
