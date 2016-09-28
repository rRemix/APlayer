package remix.myplayer.ui;

import android.view.View;

import java.util.ArrayList;

import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.FolderFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/20 16:12
 */
public class MultiChoice {
    /** 当前正在操作的activity或者fragment */
    public static String TAG = "";
    public static int TYPE = -1;

    /** 多选菜单是否正在显示 */
    private boolean mIsShow = false;

    /** 所有选中状态的view */
    public ArrayList<View> mSelectedViews = new ArrayList<>();

    /** 所有选中view的position */
    public ArrayList<MultiPosition> mSelectedPosition = new ArrayList<>();

    /** 所有选中view对应的参数 包括歌曲id 专辑id 艺术家id 文件夹名 播放列表名 */
    public ArrayList<Object> mSelectedArg = new ArrayList<>();

    /** 更新optionmenu */
    private onUpdateOptionMenuListener mUpdateOptionMenuListener;

    public boolean isShow(){
        return mIsShow;
    }

    public void setShowing(boolean showing){
        mIsShow = showing;
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
     * @param position
     * @param arg
     * @param tag
     * @return
     */
    public boolean itemAddorRemoveWithClick(View view,int position,Object arg,String tag){
        if(mIsShow && TAG.equals(tag)){
            mIsShow = true;
            RemoveOrAddView(view);
            RemoveOrAddPosition(position);
            RemoveOrAddArg(arg);
            return true;
        }
        return false;
    }

    /**
     *
     * @param view
     * @param position
     * @param arg
     * @param tag
     */
    public void itemAddorRemoveWithLongClick(View view,int position,Object arg,String tag){
        //当前没有处于多选状态
        if(!mIsShow && TAG.equals("")){
            TAG = tag;
            TYPE = getType(TAG);
            mIsShow = true;
            if(mUpdateOptionMenuListener != null)
                mUpdateOptionMenuListener.onUpdate(true);
        }
        RemoveOrAddView(view);
        RemoveOrAddPosition(position);
        RemoveOrAddArg(arg);

    }

    public void UpdateOptionMenu(boolean multishow){
        if(mUpdateOptionMenuListener != null)
            mUpdateOptionMenuListener.onUpdate(multishow);
    }

    public void AddView(View view){
        if(!mSelectedViews.contains(view)) {
            mSelectedViews.add(view);
        }
        setViewSelected(view, true);
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

    /**
     * 添加或者删除选中view在adapter中的position
     * @param position
     */
    public void RemoveOrAddPosition(int position){
        MultiPosition pos = new MultiPosition(position);
        if(mSelectedPosition.contains(pos))
            mSelectedPosition.remove(pos);
        else {
            mSelectedPosition.add(pos);
        }
    }

    /**
     * 添加或者删除选中的view对应的参数，如歌曲id
     * @param arg
     */
    public void RemoveOrAddArg(Object arg){
        if(mSelectedArg.contains(arg)){
            mSelectedArg.remove(arg);
        } else {
            mSelectedArg.add(arg);
        }
    }

    /**
     * 重置
     */
    public void clear(){
        clearSelectedViews();
        mSelectedViews.clear();
        mSelectedPosition.clear();
        mSelectedArg.clear();
        TAG = "";
        TYPE = -1;
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

    public static int getType(String tag){

        if(tag.equals(SongFragment.TAG))
            return Constants.SONG;
        else if(tag.equals(AlbumFragment.TAG))
            return Constants.ALBUM;
        else if (tag.equals(ArtistFragment.TAG))
            return Constants.ARTIST;
        else if(tag.equals(FolderFragment.TAG))
            return Constants.FOLDER;
        else if(tag.equals(PlayListActivity.TAG))
            return Constants.PLAYLIST;
        else
            return -1;
    }

}
