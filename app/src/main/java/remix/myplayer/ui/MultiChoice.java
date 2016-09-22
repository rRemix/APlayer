package remix.myplayer.ui;

import android.view.View;

import java.util.ArrayList;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/20 16:12
 */
public class MultiChoice {
    /** 当前正在操作的activity或者fragment */
    private String mTag = "";
    /** 多选菜单是否正在显示 */
    private boolean mIsShow = false;
    /** 所有选中状态的view */
    public ArrayList<View> mSelectedViews = new ArrayList<>();
    public ArrayList<Integer> mSelectedPosition = new ArrayList<>();
    /** 更新optionmenu */
    private onUpdateOptionMenuListener mUpdateOptionMenuListener;

    public boolean isShow(){
        return mIsShow;
    }

    public void setShowing(boolean showing){
        mIsShow = showing;
    }

    public void setTag(String tag){
        mTag = tag;
    }

    public String getTag(){
        return mTag;
    }

    public interface onUpdateOptionMenuListener {
        void onUpdate(boolean multiShow);
    }

    public void setOnUpdateOptionMenuListener(onUpdateOptionMenuListener l){
        mUpdateOptionMenuListener = l;
    }

    /**
     *
     * @param view
     * @param tag
     */
    public boolean itemAddorRemoveWithClick(View view,int position,String tag){
        if(mIsShow && mTag.equals(tag)){
            mIsShow = true;
            RemoveOrAddView(view);
            RemoveOrAddPosition(position);
            return true;
        }
        return false;
    }

    /**
     *
     * @param view
     * @param tag
     */
    public void itemAddorRemoveWithLongClick(View view,int position,String tag){
        if(!mIsShow && mTag.equals("")){
            RemoveOrAddView(view);
            RemoveOrAddPosition(position);
            mTag = tag;
            mIsShow = true;
            if(mUpdateOptionMenuListener != null)
                mUpdateOptionMenuListener.onUpdate(true);
            return;
        }
        if(mIsShow && mTag.equals(tag)){
            RemoveOrAddView(view);
            RemoveOrAddPosition(position);
        }

    }

    public void UpdateOptionMenu(boolean multishow){
        if(mUpdateOptionMenuListener != null)
            mUpdateOptionMenuListener.onUpdate(multishow);
    }

    public void AddView(View view){
        if(!mSelectedViews.contains(view))
            setViewSelected(view,true);
    }

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

    public void RemoveOrAddPosition(int position){
        if(mSelectedPosition.contains(position))
            mSelectedPosition.remove(position);
        else {
            mSelectedPosition.add(position);
        }
    }

    /**
     * 重置
     */
    public void clear(){
        clearSelectedViews();
        mSelectedViews.clear();
        mSelectedPosition.clear();
        mTag = "";
    }

    /**
     * 清除所有view的选中状态
     */
    public void clearSelectedViews(){
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
        if(v != null) {
            v.setSelected(selected);
        }
    }

    /**
     * 更新toolbar
     */
//    public void updateOptionsMenu(boolean multiShow, final Activity activity, final Toolbar toolbar, final Object target){
//        mIsShow = multiShow;
//        toolbar.setNavigationIcon(mIsShow ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mIsShow){
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
