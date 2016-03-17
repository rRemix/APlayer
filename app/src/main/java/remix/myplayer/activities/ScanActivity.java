package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.ui.customviews.CustomSeekBar;
import remix.myplayer.ui.popupwindow.TimerDialog;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by taeja on 16-3-7.
 */
public class ScanActivity extends ToolbarActivity {
    private Toolbar mToolBar;
    private CustomSeekBar mCustomSeekbar;
    private int mPosition;
    public static ArrayList<Integer> mSizeList = new ArrayList<>();
    static {
        mSizeList.add(0);
        mSizeList.add(300 * 1024);
        mSizeList.add(500 * 1024);
        mSizeList.add(800 * 1024);
        mSizeList.add(1024 * 1024);
        mSizeList.add(2 * 1024 * 1024);
    }
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mCustomSeekbar.setPosition(msg.arg1);
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar,"返回");
        initSeekbar();
    }

    private void initSeekbar() {
        mCustomSeekbar = (CustomSeekBar)findViewById(R.id.custom_seekbar);
        for (int i = 0 ; i < mSizeList.size() ;i++){
            mPosition = i;
            if(mSizeList.get(i) == Constants.SCAN_SIZE)
                break;
        }
        mCustomSeekbar.setOnSeekBarChangeListener(new CustomSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CustomSeekBar seekBar, int position, boolean fromUser) {
                int size = mSizeList.get(position);
                if(size >= 0) {
                    SharedPrefsUtil.putValue(ScanActivity.this, "setting", "scansize", size);
                    Constants.SCAN_SIZE = size;
                }
            }
            @Override
            public void onStartTrackingTouch(CustomSeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(CustomSeekBar seekBar) {
            }
        });
        new Thread(){
            @Override
            public void run(){
                while (!mCustomSeekbar.isInit()){
                }
                Message msg = new Message();
                msg.arg1 = mPosition;
                mHandler.sendMessage(msg);
            }
        }.start();
    }

    private void initToolbar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("返回");
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
                        startActivity(new Intent(ScanActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(ScanActivity.this, TimerDialog.class));
                        break;
                }
                return true;
            }
        });
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
