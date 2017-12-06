package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.FolderAdapter;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.Util;

/**
 * @ClassName FolderActivity
 * @Description 文件夹activity
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class FolderActivity extends PermissionActivity<Object,FolderAdapter> {
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    public static final String TAG = FolderActivity.class.getSimpleName();
    private Handler mRefreshHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"Folder");
        super.onCreate(savedInstanceState);
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_folder);
        ButterKnife.bind(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mRefreshHandler = new Handler();
        mAdapter = new FolderAdapter(this,R.layout.item_folder_recycle,mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String path = Util.getMapkeyByPosition(Global.FolderMap,position);
                if(!TextUtils.isEmpty(path) &&
                        !mMultiChoice.itemAddorRemoveWithClick(view,position,position,TAG)){
                    Intent intent = new Intent(FolderActivity.this, ChildHolderActivity.class);
                    intent.putExtra("Id", position);
                    intent.putExtra("Type", Constants.FOLDER);
                    intent.putExtra("Title",path);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String path = Util.getMapkeyByPosition(Global.FolderMap,position);
                if(!TextUtils.isEmpty(path))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,position,TAG,Constants.FOLDER);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        setUpToolbar(mToolBar,getString(R.string.folder));
    }


    private void updateList() {
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(FolderActivity.class.getSimpleName());
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshHandler.removeCallbacksAndMessages(null);
    }

    protected void onPause() {
        MobclickAgent.onPageEnd(FolderActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.postDelayed(() -> mMultiChoice.clearSelectedViews(),500);
        }
    }


    @Override
    public void onMediaStoreChanged() {
        updateList();
    }


    @Override
    protected int getLoaderId() {
        return LoaderIds.FOLDER_ACTIVITY;
    }

}
