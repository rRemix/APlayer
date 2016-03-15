package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import remix.myplayer.R;
import remix.myplayer.ui.TimerPopupWindow;

/**
 * Created by taeja on 16-3-7.
 */
public class FeedBakActivity extends BaseToolbarActivity {
    private Toolbar mToolBar;
    private EditText mEditText;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        mEditText = (EditText)findViewById(R.id.feedback_edittext);

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar,"返回");
    }

    public void onSubmit(View v){
        Intent data=new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:568920427@qq.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, "APlayer意见与反馈");
        data.putExtra(Intent.EXTRA_TEXT, mEditText.getText().toString());
        startActivity(data);
    }

//    private void initToolbar() {
//        mToolBar = (Toolbar) findViewById(R.id.toolbar);
//        mToolBar.setTitle("返回");
//        mToolBar.setTitleTextColor(Color.parseColor("#ffffffff"));
//        setSupportActionBar(mToolBar);
//        mToolBar.setNavigationIcon(R.drawable.common_btn_back);
//        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
//        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.toolbar_search:
//                        startActivity(new Intent(FeedBakActivity.this, SearchActivity.class));
//                        break;
//                    case R.id.toolbar_timer:
//                        startActivity(new Intent(FeedBakActivity.this, TimerPopupWindow.class));
//                        break;
//                }
//                return true;
//            }
//        });
//    }
}
