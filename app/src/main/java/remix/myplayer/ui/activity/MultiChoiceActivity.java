package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.application.Application;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.TipPopupwindow;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.SPUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
public class MultiChoiceActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.toolbar_multi)
    ViewGroup mMultiToolBar;
    protected MultiChoice mMultiChoice = null;
    private TipPopupwindow mTipPopupWindow;
    public MultiChoice getMultiChoice(){
        return mMultiChoice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMultiChoice = new MultiChoice(this);
        mMultiChoice.setOnUpdateOptionMenuListener(new OnUpdateOptionMenuListener() {
            @Override
            public void onUpdate(boolean multiShow) {
                mMultiChoice.setShowing(multiShow);

                mToolBar.setVisibility(multiShow ? View.GONE : View.VISIBLE);
                mMultiToolBar.setVisibility(multiShow ? View.VISIBLE : View.GONE);
                //清空
                if(!mMultiChoice.isShow()){
                    mMultiChoice.clear();
                }
                //只有主界面显示分割线
                mMultiToolBar.findViewById(R.id.multi_divider).setVisibility(MultiChoiceActivity.this instanceof MainActivity ? View.VISIBLE : View.GONE);
                //第一次长按操作显示提示框
                if(true /**SPUtil.getValue(Application.getContext(),"Setting","IsFirstMulti",true)*/){
                    SPUtil.putValue(Application.getContext(),"Setting","IsFirstMulti",false);
                    if(mTipPopupWindow == null){
                        mTipPopupWindow = new TipPopupwindow(MultiChoiceActivity.this);
                        mTipPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                            @Override
                            public void onDismiss() {
                                mTipPopupWindow = null;
                            }
                        });
                    }
                    if(!mTipPopupWindow.isShowing() && multiShow){
                        mTipPopupWindow.show(new View(MultiChoiceActivity.this));
                    }
                }


            }
        });
    }

    @Override
    protected void initToolbar(Toolbar toolbar, String title) {
        super.initToolbar(toolbar,title);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(MultiChoiceActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(MultiChoiceActivity.this, TimerDialog.class));
                        break;
                    case R.id.toolbar_delete:
                        MobclickAgent.onEvent(MultiChoiceActivity.this,"Delete");
                        if(mMultiChoice != null)
                            mMultiChoice.OnDelete();
                        break;
                    case R.id.toolbar_add_playing:
                        MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayingList");
                        if(mMultiChoice != null)
                            mMultiChoice.OnAddToPlayQueue();
                        break;
                    case R.id.toolbar_add_playlist:
                        MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayList");
                        if(mMultiChoice != null)
                            mMultiChoice.OnAddToPlayList();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(mMultiChoice.isShow() ? R.menu.multi_menu : R.menu.toolbar_menu, menu);
        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        return true;
    }

    public void onBackPress(){
        mMultiChoice.UpdateOptionMenu(false);
        if(mTipPopupWindow != null && mTipPopupWindow.isShowing()){
            mTipPopupWindow.dismiss();
            mTipPopupWindow = null;
        }
    }


    @OnClick({R.id.multi_delete,R.id.multi_playlist,R.id.multi_playqueue})
    public void onMutltiClick(View v){
        switch (v.getId()){
            case R.id.multi_delete:
                MobclickAgent.onEvent(MultiChoiceActivity.this,"Delete");
                if(mMultiChoice != null)
                    mMultiChoice.OnDelete();
                break;
            case R.id.multi_playqueue:
                MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayingList");
                if(mMultiChoice != null)
                    mMultiChoice.OnAddToPlayQueue();
                break;
            case R.id.multi_playlist:
                MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayList");
                if(mMultiChoice != null)
                    mMultiChoice.OnAddToPlayList();
                break;
        }
    }
}
