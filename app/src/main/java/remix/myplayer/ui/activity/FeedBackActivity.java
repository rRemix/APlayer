package remix.myplayer.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.bean.misc.Feedback;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.App.IS_GOOGLEPLAY;

/**
 * Created by taeja on 16-3-7.
 */

/**
 * 反馈界面
 * 将用户的反馈通过邮箱发送
 */
public class FeedBackActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.feedback_content)
    EditText mContent;
    @BindView(R.id.feedback_contact)
    EditText mContact;
    @BindView(R.id.feedback_submit)
    Button mSubmit;

    Feedback mFeedBack = new Feedback();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        ButterKnife.bind(this);
        setUpToolbar(mToolBar, getString(R.string.back));

        mSubmit.setBackground(Theme.getCorner(1.0f, DensityUtil.dip2px(this, 2), 0,
                ThemeStore.getAccentColor()));
        mContent.setBackground(Theme.getCorner(1.0f, DensityUtil.dip2px(this, 2), 0,
                Color.parseColor("#e2e2e2")));
        Theme.setTint(mContact, ThemeStore.getMaterialPrimaryColor(), false);
    }

    @OnClick(R.id.feedback_submit)
    public void onClick(View v) {
        try {
            if (TextUtils.isEmpty(mContent.getText())) {
                ToastUtil.show(this, getString(R.string.input_feedback_content));
                return;
            }
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            mFeedBack = new Feedback(mContent.getText().toString(),
                    mContact.getText().toString(),
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + ""
            );
            commitByEmail();
        } catch (PackageManager.NameNotFoundException e) {
            ToastUtil.show(this, R.string.send_error);
        }
    }

    private void commitByEmail() {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse(!IS_GOOGLEPLAY ? "mailto:568920427@qq.com" : "mailto:rRemix.me@gmail.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        data.putExtra(Intent.EXTRA_TEXT, mContent.getText().toString() + "\n\n\n" + mFeedBack);
        if (Util.isIntentAvailable(this, data)) {
            startActivity(data);
        } else {
            ToastUtil.show(this, R.string.send_error);
        }

    }
}
