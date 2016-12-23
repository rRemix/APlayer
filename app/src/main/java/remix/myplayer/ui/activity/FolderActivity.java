package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * @ClassName FolderActivity
 * @Description 文件夹activity
 * @Author Xiaoborui
 * @Date 2016/10/8 09:46
 */
public class FolderActivity extends MultiChoiceActivity {
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    private static FolderActivity mInstance;
    private FolderAdapter mAdapter;
    private boolean mIsRunning = false;
    public static final String TAG = FolderActivity.class.getSimpleName();
    //更新
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.CLEAR_MULTI){
                mMultiChoice.clearSelectedViews();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"Folder");
        super.onCreate(savedInstanceState);
        mInstance = this;
        initViews();
    }

    private void initViews() {
        setContentView(R.layout.activity_folder);
        ButterKnife.bind(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new FolderAdapter(this,mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String path = CommonUtil.getMapkeyByPosition(Global.mFolderMap,position);
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
                String path = CommonUtil.getMapkeyByPosition(Global.mFolderMap,position);
                if(!TextUtils.isEmpty(path))
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,position,TAG,Constants.FOLDER);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        setUpToolbar(mToolBar,"文件夹");
    }

    public void updateList() {
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
        mIsRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    protected void onPause() {
        MobclickAgent.onPageEnd(FolderActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    public static FolderActivity getInstance() {
        return mInstance;
    }
}
