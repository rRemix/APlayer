package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.application.Application;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.TipPopupwindow;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

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
    View mMultiToolBar;
    protected MultiChoice mMultiChoice = null;
    private TipPopupwindow mPopupWindow;
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
                if(true /**SPUtil.getValue(Application.getContext(),"Setting","IsFirstMulti",true)*/){
                    SPUtil.putValue(Application.getContext(),"Setting","IsFirstMulti",false);
                    if(mPopupWindow == null){
                        mPopupWindow = new TipPopupwindow(MultiChoiceActivity.this,mMultiToolBar,R.drawable.tip_delete,R.drawable.tip_playlist,R.drawable.tip_playqueue);
                    }
                    if(!mPopupWindow.isShowing() && multiShow){
                        mPopupWindow.show(mMultiToolBar);
                    }

//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            findView(R.id.multi_delete_tip).setVisibility(View.GONE);
//                            findView(R.id.multi_playlist_tip).setVisibility(View.GONE);
//                            findView(R.id.multi_playqueue_tip).setVisibility(View.GONE);
//                        }
//                    },2000);
                }

                if(!mMultiChoice.isShow()){
                    mMultiChoice.clear();
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



}
