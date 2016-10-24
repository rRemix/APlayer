package remix.myplayer.ui.dialog;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.ui.activity.RecordShareActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/9.
 */

/**
 * 分享的Dialog
 */
public class ShareDialog extends BaseDialogActivity implements IWeiboHandler.Response{
    public static ShareDialog mInstance;
    //四个分享按钮
    @BindView(R.id.share_qq)
    ImageView mQQ;
    @BindView(R.id.share_weibo)
    ImageView mWeibo;
    @BindView(R.id.share_wechat)
    ImageView mWechat;
    @BindView(R.id.share_circlefriend)
    ImageView mCircleFrient;
    //取消按钮
    @BindView(R.id.popup_share_cancel)
    Button mCancel;

    private MP3Item mInfo;
    //Api
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
        setContentView(R.layout.dialog_share);
        ButterKnife.bind(this);
        mInstance = this;

        mInfo = (MP3Item)getIntent().getExtras().getSerializable("MP3Item");
        mType = getIntent().getExtras().getInt("Type");
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
        lp.height = 200 * metrics.densityDpi / 160;
        lp.width = metrics.widthPixels;
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
    }

    @OnClick({R.id.share_qq,R.id.share_weibo,R.id.share_wechat,R.id.share_circlefriend,R.id.popup_share_cancel})
    public void onShare(View v){
        switch (v.getId()){
            case R.id.popup_share_cancel:
                finish();
                break;
            case R.id.share_qq:
                if(mType == Constants.SHARESONG)
                    shareSongtoQQ();
                else
                    shareMindtoQQ();
                break;
            case R.id.share_weibo:
                if( !mWeiboApi.isWeiboAppInstalled()) {
                    ToastUtil.show(ShareDialog.this,R.string.not_install_weibo);
                    return;
                }
                if(mType == Constants.SHARESONG)
                    shareSongtoWeibo();
                else
                    shareMindtoWeibo();
                break;
            case R.id.share_wechat:
            case R.id.share_circlefriend:
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
                break;
        }
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
        String album_url = MediaStoreUtil.getAlbumUrlByAlbumId(mInfo.getAlbumId());
        bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        bundle.putString(QQShare.SHARE_TO_QQ_TITLE, mInfo.getTitle());
        bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, mInfo.getArtist());

        bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://music.baidu.com/" + "search?key=" + URLEncoder.encode(mInfo.getTitle()));
//        if (album_url != null && !album_url.equals(""))
//            bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, album_url);
        bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, album_url != null ? album_url : Uri.parse("res://remix.myplayer/" + R.drawable.song_artist_empty_bg).toString());
        bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, getResources().getString(R.string.app_name));
        mTencentApi.shareToQQ(ShareDialog.this, bundle, mQQListener);
    }

    //分享歌曲到微博
    private void shareSongtoWeibo() {
        TextObject textObject = new TextObject();
        textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
                "的《" + mInfo.getTitle() + "》，" + " 来自@" + getResources().getString(R.string.app_name) + "安卓客户端";
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

    //分享心情到微信
    private void shareMindtoWeChat(View v) {

        WXImageObject imgObj = new WXImageObject();
        imgObj.setImagePath(mImageUrl);

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        //设置缩略图
        if(RecordShareActivity.getBg() != null){
            Bitmap thumbBmp = Bitmap.createScaledBitmap(RecordShareActivity.getBg(), 150, 150, true);
//            msg.thumbData = CommonUtil.bmpToByteArray(thumbBmp, true);
            msg.setThumbImage(thumbBmp);
        }

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
                "的《" + mInfo.getTitle() + "》";

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObject;
        msg.description = "推荐一首好歌：" + mInfo.getArtist() +
                "的《" + mInfo.getTitle() + "》";
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
        if(intent.getAction().equals("com.sina.weibo.sdk.action.ACTION_SDK_REQ_ACTIVITY"))
            mWeiboApi.handleWeiboResponse(intent, this);
    }

    //微博回调
    @Override
    public void onResponse(BaseResponse baseResponse) {
        switch (baseResponse.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                ToastUtil.show(this,R.string.share_success);
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                ToastUtil.show(this,R.string.share_cancel);
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                ToastUtil.show(this,"分享失败" + " Error Message: " + baseResponse.errMsg);
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
            ToastUtil.show(ShareDialog.this, R.string.share_success);
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }

        @Override
        public void onError(UiError uiError) {
            ToastUtil.show(ShareDialog.this,R.string.share_error);
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }
        @Override
        public void onCancel() {
            ToastUtil.show(ShareDialog.this,R.string.share_cancel);
//            if(RecordShareActivity.mInstance != null){
//                RecordShareActivity.mInstance.finish();
//            }
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.slide_bottom_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }
}
