package remix.myplayer.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import remix.myplayer.R;
import remix.myplayer.inject.ViewInject;

/**
 * Created by taeja on 16-3-7.
 */

/**
 * 反馈界面
 * 将用户的反馈通过邮箱发送
 */
public class FeedBakActivity extends ToolbarActivity {
    @ViewInject(R.id.toolbar)
    private Toolbar mToolBar;
    @ViewInject(R.id.feedback_edittext)
    private EditText mEditText;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initToolbar(mToolBar,getString(R.string.back));
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_feedback;
    }

    public void onSubmit(View v){
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:568920427@qq.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        data.putExtra(Intent.EXTRA_TEXT, mEditText.getText().toString());
        startActivity(data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
//                        startActivity(new Intent(FeedBakActivity.this, TimerDialog.class));
//                        break;
//                }
//                return true;
//            }
//        });
//    }
}
