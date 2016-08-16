package remix.myplayer.ui.activity;

import android.content.Intent;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;

/**
 * Created by taeja on 16-3-7.
 */

/**
 * 反馈界面
 * 将用户的反馈通过邮箱发送
 */
public class FeedBakActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.feedback_edittext)
    EditText mEditText;
    @BindView(R.id.feedback_submit)
    Button mSubmit;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        ButterKnife.bind(this);
        initToolbar(mToolBar,getString(R.string.back));
        mSubmit.setBackground(Theme.getBgCorner(1.0f,5,0,ColorUtil.getColor(ThemeStore.isDay()? ThemeStore.getMaterialPrimaryColor() : R.color.purple_782899)));
    }


    public void onSubmit(View v){
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:568920427@qq.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
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
//                        startActivity(new Intent(FeedBakActivity.this, TimerDialog.class));
//                        break;
//                }
//                return true;
//            }
//        });
//    }
}
