package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;

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

        mSubmit.setBackground(Theme.getBgCorner(1.0f,5,0,ThemeStore.getStressColor()));
    }

    @OnClick(R.id.feedback_submit)
    public void onClick(View v){
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:568920427@qq.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        data.putExtra(Intent.EXTRA_TEXT, mEditText.getText().toString());
        startActivityForResult(data,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this,resultCode == Activity.RESULT_OK ? "发送成功 感谢您的反馈!" : "发送失败 请重试!",Toast.LENGTH_SHORT).show();
        finish();
    }
}
