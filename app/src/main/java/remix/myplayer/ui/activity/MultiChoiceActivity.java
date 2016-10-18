package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.umeng.analytics.MobclickAgent;

import remix.myplayer.R;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.dialog.TimerDialog;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
public class MultiChoiceActivity extends ToolbarActivity {
    protected MultiChoice mMultiChoice = null;

    public void setMultiChoice(MultiChoice MultiChoice) {
        this.mMultiChoice = MultiChoice;
    }

    public MultiChoice getMultiChoice(){
        return mMultiChoice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMultiChoice = new MultiChoice(this);
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
        getMenuInflater().inflate(mMultiChoice.isShow() ? R.menu.multi_menu : R.menu.toolbar_menu, menu);
        return true;
    }


}
