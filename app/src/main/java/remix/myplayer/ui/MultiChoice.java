package remix.myplayer.ui;

import android.app.Activity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;

import remix.myplayer.R;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/20 16:12
 */
public class MultiChoice {
    /** 当前正在操作的activity或者fragment */
    public String TAG = "";
    /** 多选菜单是否正在显示 */
    public boolean ISHOW = false;
    /** 所有选中状态的view */
    public ArrayList<View> mSelectedViews = new ArrayList<>();



    /**
     * 添加或者删除选中的view
     * @param view
     */
    public void RemoveOrAddView(View view){
        if(mSelectedViews.contains(view)){
            mSelectedViews.remove(view);
            setViewSelected(view,false);
        } else {
            mSelectedViews.add(view);
            setViewSelected(view,true);
        }

    }

    /**
     * 添加view
     * @param v
     */
    public void addSelectedView(View v){
        if(!mSelectedViews.contains(v)){
            mSelectedViews.add(v);
            setViewSelected(v,true);
        }
    }

    /**
     *
     */
    public void removeSelectedView(View v){
        if(mSelectedViews.contains(v)){
            mSelectedViews.remove(v);
            setViewSelected(v,false);
        }

    }

    /**
     * 清除所有view的选中状态
     */
    public void cleanSelectedViews(){
        for(View view : mSelectedViews){
            if(view != null)
                setViewSelected(view,false);
        }
        mSelectedViews.clear();
    }

    /**
     * 设置view的选中状态
     * @param v
     * @param selected
     */
    public void setViewSelected(View v,boolean selected){
        if(v != null)
            v.setSelected(selected);
    }

    /**
     * 更新toolbar
     */
//    public void updateOptionsMenu(boolean multiShow, final Activity activity, final Toolbar toolbar, final Object target){
//        ISHOW = multiShow;
//        toolbar.setNavigationIcon(ISHOW ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(ISHOW){
//                    updateOptionsMenu(false,activity,toolbar,target);
//                } else {
//                    if(target instanceof DrawerLayout){
//                        ((DrawerLayout)target)
//                    }
//                    mDrawerLayout.openDrawer(mNavigationView);
//                }
//            }
//        });
//        activity.invalidateOptionsMenu();
//    }
}
