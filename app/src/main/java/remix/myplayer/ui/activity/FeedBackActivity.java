package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.bmob.Feedback;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ToastUtil;

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
        setUpToolbar(mToolBar,getString(R.string.back));

        mSubmit.setBackground(Theme.getCorner(1.0f,DensityUtil.dip2px(this,2),0,ThemeStore.getAccentColor()));
        mContent.setBackground(Theme.getCorner(1.0f,DensityUtil.dip2px(this,2),0, ColorUtil.getColor(R.color.gray_e2e2e2)));
        Theme.setTint(mContact,ThemeStore.getMaterialPrimaryColor(),false);
    }

    public static void commitByBomb(Context context, String content, String contact){
        try {
            if(TextUtils.isEmpty(content)){
                return;
            }
            PackageManager pm = App.getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(App.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Feedback feedback =  new Feedback(content,
                    contact,
                    pi.versionName,
                    pi.versionCode + "",
                    Build.DISPLAY,
                    Build.CPU_ABI + "," + Build.CPU_ABI2,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    Build.VERSION.SDK_INT + ""
            );

            feedback.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                }
            });
        } catch (PackageManager.NameNotFoundException e) {

        }
    }

    @OnClick(R.id.feedback_submit)
    public void onClick(View v){
        try {
            if(TextUtils.isEmpty(mContent.getText())){
                ToastUtil.show(this,getString(R.string.input_feedback_content));
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
//            mFeedBack.save(new SaveListener<String>() {
//                @Override
//                public void done(String s, BmobException e) {
//                    if(e == null){
//                        ToastUtil.show(FeedBackActivity.this,R.string.send_success);
//                        finish();
//                    } else {
//                        commitByEmail();
//                    }
//                }
//            });
        } catch (PackageManager.NameNotFoundException e) {
            ToastUtil.show(FeedBackActivity.this,R.string.send_error);
        }
    }

    private void commitByEmail(){
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:568920427@qq.com"));
        data.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        data.putExtra(Intent.EXTRA_TEXT, mContent.getText().toString() + "\n\n\n" + mFeedBack);
        startActivityForResult(data,0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ToastUtil.show(this,requestCode == Activity.RESULT_OK ? R.string.send_success : R.string.send_error);
        finish();
    }

    public void onResume() {
        MobclickAgent.onPageStart(FeedBackActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(FeedBackActivity.class.getSimpleName());
        super.onPause();
    }
}
