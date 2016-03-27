package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.umeng.update.UmengUpdateAgent;

import remix.myplayer.R;
import remix.myplayer.adapters.SettingAdapter;
import remix.myplayer.ui.dialog.TimerDialog;


/**
 * Created by taeja on 16-3-7.
 */

/**
 * 设置界面，目前包括扫描文件、意见与反馈、关于我们、检查更新
 */
public class SettingActivity extends ToolbarActivity {
    private ListView mListView;
    private Toolbar mToolBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //初始化listview
        mListView = (ListView)findViewById(R.id.setting_list);
        mListView.setAdapter(new SettingAdapter(this));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        //扫描大小
                        startActivity(new Intent(SettingActivity.this,ScanActivity.class));
                        break;
                    case 1:
                        //意见与反馈
                        startActivity(new Intent(SettingActivity.this,FeedBakActivity.class));
                        break;
                    case 2:
                        //关于我们
                        startActivity(new Intent(SettingActivity.this,AboutActivity.class));
                        break;
                        //检查更新
                    case 3:
                        UmengUpdateAgent.forceUpdate(SettingActivity.this);
                }
            }
        });

        //初始化tooblar
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar,"设置");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
