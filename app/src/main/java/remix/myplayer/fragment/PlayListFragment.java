package remix.myplayer.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.db.PlayLists;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class PlayListFragment extends CursorFragment implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = PlayListFragment.class.getSimpleName();
    public static int mPlayListIDIndex;
    public static int mPlayListNameIndex;
    public static int mPlayListSongCountIndex;

    @BindView(R.id.playlist_recycleview)
    RecyclerView mRecyclerView;

    //列表显示与网格显示切换
    @BindView(R.id.list_model)
    ImageView mListModelBtn;
    @BindView(R.id.grid_model)
    ImageView mGridModelBtn;
    @BindView(R.id.divider)
    View mDivider;
    //当前列表模式 1:列表 2:网格
    public static int ListModel = 2;

    private static int LOADER_ID = 0;
    private MultiChoice mMultiChoice;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getLoaderManager().initLoader(LOADER_ID, null, (LoaderManager.LoaderCallbacks) this);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        ListModel = SPUtil.getValue(getActivity(),"Setting","PlayListModel",Constants.GRID_MODEL);
        mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
        mRecyclerView.setLayoutManager(ListModel == 1 ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));

        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }
        mAdapter = new PlayListAdapter(getActivity(),mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = getPlayListName(position);
                if(!TextUtils.isEmpty(name) && !mMultiChoice.itemAddorRemoveWithClick(view,position,getPlayListId(position),TAG)){
                    if(getPlayListSongCount(position) == 0) {
                        ToastUtil.show(getActivity(),getString(R.string.list_isempty));
                        return;
                    }
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", getPlayListId(position));
                    intent.putExtra("Title", name);
                    intent.putExtra("Type", Constants.PLAYLIST);
                    intent.putExtra("PlayListID", getPlayListId(position));
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String name = getPlayListName(position);
                if(!TextUtils.isEmpty(name))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,getPlayListId(position),TAG,Constants.PLAYLIST);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        //模式切换按钮
        mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
        return rootView;
    }

    public static synchronized int getModel(){
        return ListModel;
    }

    private int getPlayListId(int position){
        int playListId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playListId = mCursor.getInt(mPlayListIDIndex);
        }
        return playListId;
    }

    private String getPlayListName(int position){
        String playlistName = "";
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playlistName = mCursor.getString(mPlayListNameIndex);
        }
        return playlistName;
    }

    private int getPlayListSongCount(int position){
        int count = 0;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            count = mCursor.getInt(mPlayListSongCountIndex);
        }
        return count;
    }

    //打开添加播放列表的Dialog
    @OnClick({R.id.list_model,R.id.grid_model})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.list_model:
            case R.id.grid_model:
                int newModel = v.getId() == R.id.list_model ? Constants.LIST_MODEL : Constants.GRID_MODEL;
                if(newModel == ListModel)
                    return;
                ListModel = newModel;
                mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
                mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
                mRecyclerView.setLayoutManager(ListModel == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
                mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
                SPUtil.putValue(getActivity(),"Setting","PlayListModel",ListModel);
                break;

        }
    }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), PlayLists.CONTENT_URI,
                null,
                PlayLists.PlayListColumns.NAME + "!= ?",new String[]{Constants.PLAY_QUEUE},null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        try {
            mCursor = data;
            mPlayListIDIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns._ID);
            mPlayListNameIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns.NAME);
            mPlayListSongCountIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns.COUNT);
            mAdapter.setCursor(data);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }

}
