package remix.myplayer.test;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import remix.myplayer.R;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/20.
 */
public class ShareTestActivity extends AppCompatActivity{
    private Tencent mTencent;
    private EditText mEditText;
    private BaseUiListener mListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testactivity);

        mTencent = Tencent.createInstance(Utility.TECENT_APIID,getApplicationContext());
//        mEditText = (EditText)findViewById(R.id.test_edit);
        mListener = new BaseUiListener();
    }

    public void share(View view)
    {
        Bundle bundle = new Bundle();
        bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE,QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        bundle.putString(QQShare.SHARE_TO_QQ_TITLE, "分享的标题");
        bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY,"分享的摘要");
        bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://www.qq.com/news/1.html");
        bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME,"MyPlayer");
        mTencent.shareToQQ(ShareTestActivity.this,bundle,mListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Tencent.onActivityResultData(requestCode,resultCode,data,mListener);
    }

    private class BaseUiListener implements IUiListener
    {

        @Override
        public void onComplete(Object o) {
            mEditText.append("\r\n");
            mEditText.setText(o.toString());
        }

        @Override
        public void onError(UiError uiError) {
            System.out.println("Msg = " + uiError.errorMessage + " Detail = " + uiError.errorDetail);
        }
        @Override
        public void onCancel() {
            System.out.println("取消");
        }
    }
}
