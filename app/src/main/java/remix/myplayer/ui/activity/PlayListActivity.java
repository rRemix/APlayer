package remix.myplayer.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends MultiChoiceActivity implements MusicService.Callback{
    public static final String TAG = PlayListActivity.class.getSimpleName();
    public static PlayListActivity mInstance = null;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
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
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.UPDATE_MULTI:
                    mMultiChoice.clearSelectedViews();
                    break;
                case Constants.UPDATE_ADAPTER:
                    if(mAdapter != null)
                        mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        ButterKnife.bind(this);
        MusicService.addCallback(PlayListActivity.this);
        mMultiChoice.setOnUpdateOptionMenuListener(new OnUpdateOptionMenuListener() {
            @Override
            public void onUpdate(boolean multiShow) {
                mMultiChoice.setShowing(multiShow);
                mToolBar.setNavigationIcon(mMultiChoice.isShow() ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
                mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow()){
                            mMultiChoice.UpdateOptionMenu(false);
                            mMultiChoice.clear();
                        } else {
                            finish();
                        }
                    }
                });
                if(!mMultiChoice.isShow()){
                    mMultiChoice.clear();
                }
                invalidateOptionsMenu();
            }
        });

        mInstance = this;
        ListModel = SPUtil.getValue(this,"Setting","AlbumModel",2);
        mRecycleView.setLayoutManager(ListModel == 1 ? new LinearLayoutManager(this) : new GridLayoutManager(this, 2));
        mAdapter = new PlayListAdapter(this,mMultiChoice);
        mAdapter.setOnItemClickLitener(new PlayListAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = CommonUtil.getMapkeyByPosition(Global.mPlaylist,position);
                if(!TextUtils.isEmpty(name) && !mMultiChoice.itemAddorRemoveWithClick(view,position,position,TAG)){
                    if(Global.mPlaylist.get(name).size() == 0) {
                        Toast.makeText(PlayListActivity.this, getString(R.string.list_isempty), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(PlayListActivity.this, ChildHolderActivity.class);
                    intent.putExtra("Test",true);
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

        StateListDrawable stateListDrawable1 = new StateListDrawable();
        Drawable drawable1 =  Theme.TintDrawable(Theme.getDrawable(this,R.drawable.btn_list2), ThemeStore.getMaterialColorPrimaryColor());
        stateListDrawable1.addState(new int[]{android.R.attr.state_pressed}, drawable1);
        stateListDrawable1.addState(new int[]{android.R.attr.state_selected}, drawable1);
        stateListDrawable1.addState(new int[]{}, Theme.getDrawable(this,R.drawable.btn_list2));
        mListModelBtn.setImageDrawable(stateListDrawable1);
        mListModelBtn.setSelected(ListModel == 1);

        StateListDrawable stateListDrawable2 = new StateListDrawable();
        Drawable drawable2 =  Theme.TintDrawable(Theme.getDrawable(this,R.drawable.btn_list1), ThemeStore.getMaterialColorPrimaryColor());
        stateListDrawable2.addState(new int[]{android.R.attr.state_pressed}, drawable2);
        stateListDrawable2.addState(new int[]{android.R.attr.state_selected}, drawable2);
        stateListDrawable2.addState(new int[]{}, Theme.getDrawable(this,R.drawable.btn_list1));
        mGridModelBtn.setImageDrawable(stateListDrawable2);
        mGridModelBtn.setSelected(ListModel == 2);

        //初始化tooblar
        initToolbar(mToolBar,getString(R.string.playlist));

    }


    //打开添加播放列表的Dialog
    @OnClick({R.id.list_model,R.id.grid_model,R.id.floatbutton})
    public void onAdd(View v){
        switch (v.getId()){
            case R.id.floatbutton:
                if(mMultiChoice.isShow())
                    return;
                new MaterialDialog.Builder(this)
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
                                    XmlUtil.addPlaylist(PlayListActivity.this,input.toString());
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
                mListModelBtn.setSelected(v.getId() == R.id.list_model);
                mGridModelBtn.setSelected(v.getId() == R.id.grid_model);
                ListModel = v.getId() == R.id.list_model ? 1 : 2;
                mRecycleView.setLayoutManager(ListModel == 1 ? new LinearLayoutManager(this) : new GridLayoutManager(this, 2));
                SPUtil.putValue(this,"Setting","AlbumModel",ListModel);
                if(mAdapter != null)
                    mAdapter.notifyDataSetChanged();
                break;

        }

    }

    @OnClick({R.id.list_model,R.id.grid_model})
    public void onSwitch(View v){

    }


    public PlayListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
    }

    @Override
    public int getType() {
        return Constants.PLAYLISTACTIVITY;
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.UPDATE_MULTI,500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            mMultiChoice.UpdateOptionMenu(false);
        } else {
           finish();
        }
    }

    public void UpdateAdapter() {
        if(mRecycleView.getAdapter() != null){
            mRecycleView.getAdapter().notifyDataSetChanged();
        }
    }

}
