package remix.myplayer.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.XmlUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class PlayListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = PlayListFragment.class.getSimpleName();
    public static PlayListFragment mInstance = null;
    public static int mPlayListIDIndex;
    public static int mPlayListNameIndex;
    private Cursor mCursor;
    @BindView(R.id.playlist_recycleview)
    RecyclerView mRecycleView;

    //列表显示与网格显示切换
    @BindView(R.id.list_model)
    ImageView mListModelBtn;
    @BindView(R.id.grid_model)
    ImageView mGridModelBtn;
    //当前列表模式 1:列表 2:网格
    public static int ListModel = 2;

    private PlayListAdapter mAdapter;
    private MultiChoice mMultiChoice;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        ListModel = SPUtil.getValue(getActivity(),"Setting","PlayListModel",2);
        mRecycleView.setLayoutManager(ListModel == 1 ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }
        mAdapter = new PlayListAdapter(getActivity(),mMultiChoice);
        mAdapter.setOnItemClickLitener(new PlayListAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                mAdapter.notifyDataSetChanged();
                String name = CommonUtil.getMapkeyByPosition(Global.mPlaylist,position);
                if(!TextUtils.isEmpty(name) && !mMultiChoice.itemAddorRemoveWithClick(view,position,position,TAG)){
                    if(Global.mPlaylist.get(name).size() == 0) {
                        Toast.makeText(getActivity(), getString(R.string.list_isempty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", position);
                    intent.putExtra("Title", name);
                    intent.putExtra("Type", Constants.PLAYLIST);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String name = CommonUtil.getMapkeyByPosition(Global.mPlaylist,position);
                if(!TextUtils.isEmpty(name) && !name.equals(getString(R.string.my_favorite)))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,position,TAG);
            }
        });
        mRecycleView.setAdapter(mAdapter);

        mListModelBtn.setImageDrawable(Theme.getPressAndSelectedStateListDrawalbe(getActivity(),R.drawable.btn_list2));
        mListModelBtn.setSelected(ListModel == Constants.LIST_MODEL);

        mGridModelBtn.setImageDrawable(Theme.getPressAndSelectedStateListDrawalbe(getActivity(),R.drawable.btn_list1));
        mGridModelBtn.setSelected(ListModel == Constants.GRID_MODEL);

        return rootView;
    }

    public static synchronized int getModel(){
        return ListModel;
    }

    //打开添加播放列表的Dialog
    @OnClick({R.id.list_model,R.id.grid_model,R.id.add})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.add:
                if(mMultiChoice.isShow())
                    return;
                new MaterialDialog.Builder(getActivity())
                        .title("新建播放列表")
                        .titleColor(ThemeStore.getTextColorPrimary())
                        .positiveText("创建")
                        .positiveColor(ThemeStore.getMaterialColorPrimaryColor())
                        .negativeText("取消")
                        .negativeColor(ThemeStore.getMaterialColorPrimaryColor())
                        .backgroundColor(ThemeStore.getBackgroundColor3())
                        .content(R.string.input_playlist_name)
                        .contentColor(ThemeStore.getTextColorPrimary())
                        .inputRange(1,15)
                        .input("", "本地歌单" + Global.mPlaylist.size(), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                if(!TextUtils.isEmpty(input)){
                                    XmlUtil.addPlaylist(getActivity(),input.toString());
                                }
                            }
                        })
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if(mAdapter != null)
                                    mAdapter.notifyDataSetChanged();
                            }
                        })
                        .show();
                break;
            case R.id.list_model:
            case R.id.grid_model:
                int newModel = v.getId() == R.id.list_model ? Constants.LIST_MODEL : Constants.GRID_MODEL;
                if(newModel == ListModel)
                    return;
                mListModelBtn.setSelected(v.getId() == R.id.list_model);
                mGridModelBtn.setSelected(v.getId() == R.id.grid_model);
                ListModel = newModel;
                mRecycleView.setLayoutManager(ListModel == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
                SPUtil.putValue(getActivity(),"Setting","PlayListModel",ListModel);
                break;

        }
    }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public void UpdateAdapter() {
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Playlists.NAME,MediaStore.Audio.Playlists._ID},null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        if(data == null)
//            return;
//        //查询完毕后保存结果，并设置查询索引
//        try {
//            mCursor = data;
//            mPlayListIDIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
//            mAdapter.setCursor(data);
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
//        if (mAdapter != null)
//            mAdapter.setCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if(mCursor != null)
//            mCursor.close();
    }
}
