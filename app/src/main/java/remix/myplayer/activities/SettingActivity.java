package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.adapters.SettingAdapter;
import remix.myplayer.ui.TimerPopupWindow;

/**
 * Created by taeja on 16-3-7.
 */
public class SettingActivity extends AppCompatActivity {
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
                        break;
                }
            }
        });

        //初始化tooblar
        initToolbar();
    }

    private void initToolbar() {

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("设置");
        mToolBar.setTitleTextColor(Color.parseColor("#ffffffff"));
        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.common_btn_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(SettingActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(SettingActivity.this, TimerPopupWindow.class));
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}
