package remix.myplayer.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.FolderFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.interfaces.OnMultiItemClickListener;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.XmlUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/20 16:12
 */
public class MultiChoice implements OnMultiItemClickListener {
    private Context mContext;

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
    private OnUpdateOptionMenuListener mUpdateOptionMenuListener;

    /** 构造函数 */
    public MultiChoice(Context context){
        mContext = context;
    }

    public MultiChoice(){}
    public void setContext(Context context){
        mContext = context;
    }


    public boolean isShow(){
        return mIsShow;
    }

    public void setShowing(boolean showing){
        mIsShow = showing;
    }

    @Override
    public void OnAddToPlayingList() {
        int num = 0;
        ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.SONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,TYPE);
                    if(tempList != null && tempList.size() > 0)
                        idList.addAll(DBUtil.getSongIdListByArg(arg,TYPE));
                }
                break;
        }
        num = XmlUtil.addSongsToPlayingList(idList);
        Toast.makeText(mContext, mContext.getResources().getString(R.string.add_song_playinglist_success,num),Toast.LENGTH_SHORT).show();
        UpdateOptionMenu(false);
    }

    @Override
    public void OnAddToPlayList() {

        final ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.SONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,TYPE);
                    if(tempList != null && tempList.size() > 0)
                        idList.addAll(DBUtil.getSongIdListByArg(arg,TYPE));
                }
                break;
        }

        //获得所有播放列表的名字
        Iterator it = Global.mPlaylist.keySet().iterator();
        ArrayList<String> playlistNameList = new ArrayList<>();
        while (it.hasNext()){
            playlistNameList.add(it.next().toString());
        }
        new MaterialDialog.Builder(mContext)
                .title(R.string.add_to_playlist)
                .titleColorAttr(R.attr.text_color_primary)
                .items(playlistNameList.toArray(new CharSequence[playlistNameList.size()]))
                .itemsColorAttr(R.attr.text_color_primary)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        final int num;
                        num = XmlUtil.addSongsToPlayList(text.toString(),DBUtil.getPlayListItemListByIds(idList));
                        Toast.makeText(mContext, mContext.getString(R.string.add_song_playlist_success, num)
                                ,Toast.LENGTH_SHORT).show();
                        UpdateOptionMenu(false);
                    }
                })
                .neutralText(R.string.create_playlist)
                .neutralColorAttr(R.attr.material_color_primary)
                .onNeutral(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        new MaterialDialog.Builder(mContext)
                                .title(R.string.new_playlist)
                                .titleColor(ThemeStore.getTextColorPrimary())
                                .positiveText(R.string.create)
                                .positiveColor(ThemeStore.getMaterialColorPrimaryColor())
                                .negativeText(R.string.cancel)
                                .negativeColor(ThemeStore.getTextColorPrimary())
                                .backgroundColor(ThemeStore.getBackgroundColor3())
                                .content(R.string.input_playlist_name)
                                .contentColor(ThemeStore.getTextColorPrimary())
                                .inputRange(1,15)
                                .input("", "本地歌单" + Global.mPlaylist.size(), new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                        if(!TextUtils.isEmpty(input)){
                                            XmlUtil.addPlaylist(mContext,input.toString());
                                            final int num;
                                            num = XmlUtil.addSongsToPlayList(input.toString(),DBUtil.getPlayListItemListByIds(idList));
                                            Toast.makeText(mContext, mContext.getString(R.string.add_song_playlist_success, num)
                                                    ,Toast.LENGTH_SHORT).show();
                                            UpdateOptionMenu(false);
                                        }
                                    }
                                })
                                .show();
                    }
                })
                .backgroundColorAttr(R.attr.background_color_3).build().show();

    }

    @Override
    public void OnDelete() {
        int num = 0;
        ArrayList<Integer> idList = new ArrayList<>();
        switch (TYPE){
            case Constants.SONG:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer)
                        idList.add((Integer) arg);
                }
                break;
            case Constants.PLAYLIST:
                for(Object arg : mSelectedArg){
                    if (arg instanceof Integer && DBUtil.deleteSong((Integer) arg,Constants.PLAYLIST)){
                        num++;
                    }
                }
                break;
            case Constants.ALBUM:
            case Constants.ARTIST:
            case Constants.FOLDER:
                for(Object arg : mSelectedArg){
                    ArrayList<Integer> tempList = DBUtil.getSongIdListByArg(arg,TYPE);
                    if(tempList != null && tempList.size() > 0)
                        idList.addAll(tempList);
                }
                for(Integer id : idList){
                    if( DBUtil.deleteSong(id,Constants.SONG))
                        num++;
                }
                break;
        }
        if(num > 0){
            Toast.makeText(mContext, mContext.getString(R.string.delete_success),Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext,R.string.delete_error,Toast.LENGTH_SHORT).show();
        }
        UpdateOptionMenu(false);
    }


    public void setOnUpdateOptionMenuListener(OnUpdateOptionMenuListener l){
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
