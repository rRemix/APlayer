package remix.myplayer.ui;

import android.app.Activity;
import android.content.Intent;
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
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXTextObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import java.net.URLEncoder;

import remix.myplayer.R;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2015/12/9.
 */
public class SharePopupWindow extends Activity implements IWeiboHandler.Response{
    private ImageView mQQ;
    private ImageView mWeibo;
    private ImageView mWechat;
    private ImageView mCircleFrient;
    private MP3Info mInfo;
    private Button mCancel;
    private Tencent mTencentApi;
    private IWeiboShareAPI mWeiboApi;
    private BaseUiListener mListener;
    private IWXAPI mWechatApi;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_share);

        mInfo = new MP3Info((MP3Info)getIntent().getExtras().getSerializable("MP3Info"));
        if(mInfo == null)
            return;
        //初始化tencent API
        mTencentApi = Tencent.createInstance(Constants.TECENT_APIID, getApplicationContext());
        mListener = new BaseUiListener();
        //初始化微博API
        mWeiboApi = WeiboShareSDK.createWeiboAPI(this, Constants.WEIBO_APIID);
        mWeiboApi.registerApp();
        //初始化微信api
        mWechatApi = WXAPIFactory.createWXAPI(this, Constants.WECHAT_APIID,true);
        mWechatApi.registerApp(Constants.WECHAT_APIID);

        if (savedInstanceState != null) {
            mWeiboApi.handleWeiboResponse(getIntent(), this);
        }
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

        mQQ = (ImageButton)findViewById(R.id.share_qq);
        mQQ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String album_url = DBUtil.CheckUrlByAlbumId(mInfo.getAlbumId());
                bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
                bundle.putString(QQShare.SHARE_TO_QQ_TITLE, mInfo.getDisplayname());
                bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, mInfo.getArtist());

                bundle.putString(QQShare.SHARE_TO_QQ_TARGET_URL, "http://music.baidu.com/" + "search?key=" + URLEncoder.encode(mInfo.getDisplayname()));
                if (!album_url.equals(""))
                    bundle.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, album_url);
                bundle.putString(QQShare.SHARE_TO_QQ_APP_NAME, "MyPlayer");
                mTencentApi.shareToQQ(SharePopupWindow.this, bundle, mListener);
//                finish();
            }
        });
        mWeibo = (ImageButton)findViewById(R.id.share_weibo);
        mWeibo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //创建分享的内容
                if( !mWeiboApi.isWeiboAppInstalled()) {
                    Toast.makeText(SharePopupWindow.this, "您还未安装微博客户端",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                TextObject textObject = new TextObject();
                textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
                        "的《" + mInfo.getDisplayname() + "》，" + " 来自@MyPlayer 安卓客户端";
                WeiboMultiMessage msg = new WeiboMultiMessage();
                msg.textObject  = textObject;
                SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
                request.transaction = String.valueOf(System.currentTimeMillis());
                request.multiMessage = msg;
                mWeiboApi.sendRequest(SharePopupWindow.this, request);
//                finish();
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

    class WeChatClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View v) {
            if (!mWechatApi.isWXAppInstalled()) {
                Toast.makeText(SharePopupWindow.this, "您还未安装微信客户端",
                        Toast.LENGTH_SHORT).show();
                return;
            }
//            Bitmap bmp = CommonUtil.CheckBitmapBySongId((int)mInfo.getId());
//            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
//            bmp.recycle();
            WXTextObject textObject = new WXTextObject();
            textObject.text = "推荐一首好歌：" + mInfo.getArtist() +
                    "的《" + mInfo.getDisplayname() + "》";

            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = textObject;
            msg.description = "推荐一首好歌：" + mInfo.getArtist() +
                    "的《" + mInfo.getDisplayname() + "》";
            msg.title = "title";
            msg.mediaTagName = "mediaTagName";
            msg.messageExt = "messageExt";
//            msg.thumbData = CommonUtil.bmpToByteArray(thumbBmp,true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.scene = v.getId() == R.id.share_wechat ? SendMessageToWX.Req.WXSceneSession : SendMessageToWX.Req.WXSceneTimeline;
            req.transaction = String.valueOf(System.currentTimeMillis());
            req.message = msg;
            mWechatApi.sendReq(req);
//            finish();
        }
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
                finish();
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                Toast.makeText(this, "分享取消", Toast.LENGTH_LONG).show();
                finish();
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                Toast.makeText(this, "分享失败" + " Error Message: " + baseResponse.errMsg,
                        Toast.LENGTH_LONG).show();
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data != null)
        {
            String action = data.getAction();
            System.out.println(action);
        }
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != 765)
            Tencent.onActivityResultData(requestCode,resultCode,data,mListener);
    }
    //qq分享回调接口
    private class BaseUiListener implements IUiListener
    {
        @Override
        public void onComplete(Object o) {
            Toast.makeText(SharePopupWindow.this,"分享成功",Toast.LENGTH_SHORT).show();
            System.out.println(o.toString());
            finish();
        }

        @Override
        public void onError(UiError uiError) {
            Toast.makeText(SharePopupWindow.this,"分享失败",Toast.LENGTH_SHORT).show();
            System.out.println("Msg = " + uiError.errorMessage + " Detail = " + uiError.errorDetail);
            finish();
        }
        @Override
        public void onCancel() {
            Toast.makeText(SharePopupWindow.this,"分享取消",Toast.LENGTH_SHORT).show();
            System.out.println("取消分享");
            finish();
        }
    }
}
