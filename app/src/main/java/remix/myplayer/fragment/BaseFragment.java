package remix.myplayer.fragment;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import butterknife.BindView;
import butterknife.Unbinder;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
public class BaseFragment extends Fragment {
    protected Unbinder mUnBinder;
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(mUnBinder != null)
            mUnBinder.unbind();
    }

    public RecyclerView.Adapter getAdapter(){
        return null;
    }

    protected void cleanSelectedViews(){

    }

    protected void setViewSelected(View v,boolean selected){
        if(v != null)
            v.setSelected(selected);
    }

}
