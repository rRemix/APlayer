package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;
import com.tencent.connect.share.QQShare;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import java.net.URLEncoder;

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.activities.RecordShareActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/12/9.
 */
public class ShareDialog extends BaseActivity implements IWeiboHandler.Response{
    public static ShareDialog mInstance;
    private ImageView mQQ;
    private ImageView mWeibo;
    private ImageView mWechat;
    private ImageView mCircleFrient;
    private MP3Info mInfo;
    private Button mCancel;
    private Tencent mTencentApi;
    private IWeiboShareAPI mWeiboApi;
    private BaseUiListener mQQListener;
    private IWXAPI mWechatApi;
    private String mImageUrl;
    //分享心情还是歌曲
    private int mType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.popup_share);

        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        mType = (int)getIntent().getExtras().getInt("Type");
        mImageUrl = getIntent().getExtras().getString("Url");
        if(mInfo == null)
            return;

        //改变高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = (int) (200 * metrics.densityDpi / 160);
        lp.width = (int) (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        //初始化tencent API
        mTencentApi = Tencent.createInstance(Constants.TECENT_APIID, getApplicationContext());
        mQQListener = new BaseUiListener();
        //初始化微博API
        mWeiboApi = WeiboShareSDK.createWeiboAPI(this, Constants.WEIBO_APIID);
        mWeiboApi.registerApp();
        //初始化微信api
        mWechatApi = WXAPIFactory.createWXAPI(this, Constants.WECHAT_APIID,false);
        mWechatApi.registerApp(Constants.WECHAT_APIID);

        if (savedInstanceState != null) {
            mWeiboApi.handleWeiboResponse(getIntent(), this);
        }


        mQQ = (ImageButton)findViewById(R.id.share_qq);
        mQQ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mType == Constants.SHARESONG)
                    shareSongtoQQ();
                else
                    shareMindtoQQ();

            }
        });
        mWeibo = (ImageButton)findViewById(R.id.share_weibo);
        mWeibo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mWeiboApi.isWeiboAppInstalled()) {
                    Toast.makeText(ShareDialog.this, "您还未安装微博客户端",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mType == Constants.SHARESONG)
                    shareSongtoWeibo();
                else
                    shareMindtoWeibo();

            }
        });

        WeChatClickListener listener = new WeChatClickListener();
        mWechat = (ImageButton)findViewById(R.id.share_wechat);
        mWechat.setOnClickListener(listener);

        mCircleFrient = (ImageButton)findViewById(R.id.share_circlefriend);
        mCircleFrient.setOnClickListener(listener);

        mCancel = (Button)findViewById(R.id.popup_share_cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    //分享心情到qq
    private void shareMindtoQQ() {
        Bundle params = new Bundle();
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, Uri.parse(mImageUrl.toString()).toString());
        params.putString(QQShare.SHARE_TO_QQ_APP_NAME, getResources().getString(R.string.app_name));
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
        mTencentApi.shareToQQ(ShareDialog.this, params, mQQListener);
    }
    //分享歌曲到qq
    private void shareSongtoQQ() {
        Bundle bundle = new Bundle();
        String album_url = DBUtil.CheckUrlByAlbumId(mInfo.getAlbumId());
        bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        bundle.putString(QQShare.SHARE_TO_QQ_TITLE, mInfo.getDisplayname());
        bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, mInfo.getArtist());

        bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://music.baidu.com/" + "search?key=" + URLEncoder.encode(mInfo.getDisplayname()));
//        if (album_url != null && !album_url.equals(""))
//            bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, album_url);
        bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, album_url != null ? album_url : Uri.parse("res://remix.myplayer/" + R.drawable.default_recommend).toString());
        bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, getResources().getString(R.string.app_name));
        mTencentApi.shareToQQ(ShareDialog.this, bundle, mQQListener);
    }

    //分享歌曲到微博
    private void shareSongtoWeibo() {
        TextObject textObject = new TextObject();
        textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
                "的《" + mInfo.getDisplayname() + "》，" + " 来自@" + getResources().getString(R.string.app_name) + "安卓客户端";
        WeiboMultiMessage msg = new WeiboMultiMessage();
        msg.textObject  = textObject;
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.multiMessage = msg;
        mWeiboApi.sendRequest(ShareDialog.this, request);
    }

    //分享心情到微博
    private void shareMindtoWeibo(){
//        TextObject textObject = new TextObject();
//        textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
//                "的《" + mInfo.getDisplayname() + "》，" + " 来自@" + getResources().getString(R.string.app_name) + "安卓客户端";
        ImageObject imageObject = new ImageObject();
        imageObject.imagePath = mImageUrl;
        imageObject.setImageObject(RecordShareActivity.getBg());
        WeiboMultiMessage msg = new WeiboMultiMessage();
        msg.imageObject = imageObject;
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.multiMessage = msg;
        mWeiboApi.sendRequest(ShareDialog.this, request);
    }

    class WeChatClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            if (!mWechatApi.isWXAppInstalled()) {
                Toast.makeText(ShareDialog.this, "您还未安装微信客户端",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if(mType == Constants.SHARESONG){
                shareSongtoWechat(v);
            }
            else {
                shareMindtoWeChat(v);
            }
        }
    }

    //分享心情到微信
    private void shareMindtoWeChat(View v) {

        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(mImageUrl);

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        //设置缩略图
//        Bitmap thumbBmp = Bitmap.createScaledBitmap(RecordShareActivity.getBg(), 150, 150, true);
//        msg.thumbData = CommonUtil.bmpToByteArray(thumbBmp, true);

        //发送请求
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = v.getId() == R.id.share_wechat ? SendMessageToWX.Req.WXSceneSession : SendMessageToWX.Req.WXSceneTimeline;
        mWechatApi.sendReq(req);
    }

    //分享歌曲到微信
    private void shareSongtoWechat(View v) {
        WXTextObject textObject = new WXTextObject();
        textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
                "的《" + mInfo.getDisplayname() + "》";

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObject;
        msg.description = "推荐一首好歌：" + mInfo.getArtist() +
                "的《" + mInfo.getDisplayname() + "》";
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.scene = v.getId() == R.id.share_wechat ? SendMessageToWX.Req.WXSceneSession : SendMessageToWX.Req.WXSceneTimeline;
        req.transaction = buildTransaction("text"); // transaction字段用于唯一标识一个请求
        req.message = msg;
        mWechatApi.sendReq(req);
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getAction() == "com.sina.weibo.sdk.action.ACTION_SDK_REQ_ACTIVITY")
            mWeiboApi.handleWeiboResponse(intent, this);
    }
    @Override
    public void onResponse(BaseResponse baseResponse) {
        switch (baseResponse.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                Toast.makeText(this,"分享成功", Toast.LENGTH_LONG).show();
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                Toast.makeText(this, "分享取消", Toast.LENGTH_LONG).show();
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                Toast.makeText(this, "分享失败" + " Error Message: " + baseResponse.errMsg,
                        Toast.LENGTH_LONG).show();
                break;
        }
//        if(RecordShareActivity.mInstance != null){
//            RecordShareActivity.mInstance.finish();
//        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null) {
            String action = data.getAction();
        }
        if(requestCode != 765)
            Tencent.onActivityResultData(requestCode, resultCode, data, mQQListener);
    }

    //qq分享回调接口
    private class BaseUiListener implements IUiListener {
        @Override
        public void onComplete(Object o) {
            Toast.makeText(ShareDialog.this,"分享成功",Toast.LENGTH_SHORT).show();
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }

        @Override
        public void onError(UiError uiError) {
            Toast.makeText(ShareDialog.this,"分享失败",Toast.LENGTH_SHORT).show();
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }
        @Override
        public void onCancel() {
            Toast.makeText(ShareDialog.this,"分享取消",Toast.LENGTH_SHORT).show();
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }
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
