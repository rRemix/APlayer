package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.umeng.update.UmengUpdateAgent;

import java.util.Set;

import remix.myplayer.R;
import remix.myplayer.adapters.SettingAdapter;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.SharedPrefsUtil;


/**
 * Created by taeja on 16-3-7.
 */

/**
 * 设置界面，目前包括扫描文件、意见与反馈、关于我们、检查更新
 */
public class SettingActivity extends ToolbarActivity {
    private ListView mListView;
    private Toolbar mToolBar;
    private ImageView mSystem;
    private ImageView mWhite;
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
                        //音效设置
                        startActivity(new Intent(SettingActivity.this,EQActivity.class));
                        break;
                    case 2:
                        //通知栏底色
                        try {
                            View notifycolor = LayoutInflater.from(SettingActivity.this).inflate(R.layout.popup_notifycolor,null);
                            boolean isSystem = SharedPrefsUtil.getValue(SettingActivity.this,"setting","IsSystemColor",true);
                            mSystem = (ImageView)notifycolor.findViewById(R.id.popup_notify_image_system);
                            mWhite = (ImageView)notifycolor.findViewById(R.id.popup_notify_image_white);
                            if(mSystem != null)
                                mSystem.setVisibility(isSystem ? View.VISIBLE : View.INVISIBLE);
                            if(mWhite != null)
                                mWhite.setVisibility(isSystem ? View.INVISIBLE : View.VISIBLE);

                            ColorListener listener = new ColorListener();
                            notifycolor.findViewById(R.id.notifycolor_system).setOnClickListener(listener);
                            notifycolor.findViewById(R.id.notifycolor_white).setOnClickListener(listener);

                            AlertDialog alertDialog = new AlertDialog.Builder(SettingActivity.this).setView(notifycolor).create();
                            alertDialog.show();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        break;
                    case 3:
                        //意见与反馈
                        startActivity(new Intent(SettingActivity.this,FeedBakActivity.class));
                        break;
                    case 4:
                        //关于我们
                        startActivity(new Intent(SettingActivity.this,AboutActivity.class));
                        break;
                    case 5:
                        //检查更新
                        UmengUpdateAgent.forceUpdate(SettingActivity.this);
                }
            }
        });

        //初始化tooblar
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar,"设置");
    }

    class ColorListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            boolean isSystem = v.getId() == R.id.notifycolor_system;
            if(mSystem != null)
                mSystem.setVisibility(isSystem ? View.VISIBLE : View.INVISIBLE);
            if(mWhite != null)
                mWhite.setVisibility(isSystem ? View.INVISIBLE : View.VISIBLE);
            SharedPrefsUtil.putValue(SettingActivity.this,"setting","IsSystemColor",isSystem);
            //更新通知栏
            sendBroadcast(new Intent(Constants.NOTIFY));
        }
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
