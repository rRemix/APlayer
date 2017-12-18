package remix.myplayer.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.dialog.ShareDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by taeja on 16-3-14.
 */

/**
 * 将分享内容与专辑封面进行处理用于分享
 */
public class RecordShareActivity extends BaseActivity {
    private static final int IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),268);
    @BindView(R.id.recordshare_image)
    SimpleDraweeView mImage;
    //歌曲名与分享内容
    @BindView(R.id.recordshare_name)
    TextView mSong;
    @BindView(R.id.recordshare_content)
    TextView mContent;
    //背景
    @BindView(R.id.recordshare_background_1)
    View mBackground1;
    @BindView(R.id.recordshare_background_2)
    LinearLayout mBackground2;
    @BindView(R.id.recordshare_image_container)
    FrameLayout mImageBackground;
    //保存截屏
    private static Bitmap mBackgroudCache;
    @BindView(R.id.recordshare_container)
    LinearLayout mContainer;

    private boolean mHasPermission = false;
    //当前正在播放的歌曲
    private Song mInfo;
    //处理图片的进度条
    private MaterialDialog mProgressDialog;
    //处理状态
    private static final int START = 0;
    private static final int STOP = 1;
    private static final int COMPLETE = 2;
    private static final int ERROR = 3;
    //截屏文件
    private File mFile;
    //更新处理结果的Handler
    private ProcessHandler mHandler;
    private static class ProcessHandler extends Handler{
        private WeakReference<RecordShareActivity> mRef;

        public ProcessHandler(Looper looper,RecordShareActivity activity) {
            super(looper);
            mRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            RecordShareActivity activity = mRef != null ? mRef.get() : null;
            if(activity == null)
                return;
            switch (msg.what){
                //开始处理
                case START:
                   activity.showLoading();
                    break;
                //处理中止
                case STOP:
                    activity.dismissLoading("");
                    break;
                //处理完成
                case COMPLETE:
                    if(activity.mFile != null)
                        ToastUtil.show(activity,R.string.screenshot_save_at,activity.mFile.getAbsoluteFile());
                    break;
                //处理错误
                case ERROR:
                    activity.dismissLoading(activity.getString(R.string.share_error) + ":" + msg.obj);
                    break;
            }
        }
    }


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"Share");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordshare);
        ButterKnife.bind(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //初始化控件
        mContainer.setDrawingCacheEnabled(true);

        mInfo = getIntent().getExtras().getParcelable("Song");
        if(mInfo == null)
            return;

        new LibraryUriRequest(mImage,
                getSearchRequestWithAlbumType(mInfo),
                new RequestConfig.Builder(IMAGE_SIZE,IMAGE_SIZE).build()).load();

        //设置歌曲名与分享内容
        mContent.setText(getIntent().getExtras().getString("Content"));
        mSong.setText(String.format("《%s》", mInfo.getTitle()));
        //背景
        mBackground1.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, Color.WHITE,0, DensityUtil.dip2px(this,2), ColorUtil.getColor(R.color.black_2a2a2a),0,0,1));
        mBackground2.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, Color.WHITE,0,DensityUtil.dip2px(this,1), ColorUtil.getColor(R.color.black_2a2a2a),0,0,1));
        mImageBackground.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, Color.WHITE,0,DensityUtil.dip2px(this,1), ColorUtil.getColor(R.color.white_f6f6f5),0,0,1));


        mProgressDialog = new MaterialDialog.Builder(this)
                .title(R.string.please_wait)
                .titleColorRes(R.color.day_textcolor_primary)
                .content(R.string.processing_picture)
                .contentColorRes(R.color.day_textcolor_primary)
                .progress(true, 0)
                .backgroundColorRes(R.color.day_background_color_3)
                .progressIndeterminateStyle(false).build();

        mHandler = new ProcessHandler(getMainLooper(),this);
    }

    public static Bitmap getBg(){
        return mBackgroudCache;
    }

    @OnClick({R.id.recordshare_cancel,R.id.recordshare_share})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.recordshare_cancel:
                finish();
                break;
            case R.id.recordshare_share:
                new ProcessThread().start();
                break;
        }
    }

    /**
     * 将图片保存到本地
     */
    private class ProcessThread extends Thread{
        FileOutputStream fos = null;
        @Override
        public void run() {
            //开始处理,显示进度条
            if(!mHasPermission){
                Message errMsg = mHandler.obtainMessage(ERROR);
                errMsg.obj = mContext.getString(R.string.please_give_access_external_storage_permission);
                mHandler.sendMessage(errMsg);
                return;
            }
            mHandler.sendEmptyMessage(START);

            mBackgroudCache = mContainer.getDrawingCache(true);
            mFile = null;
            try {
                //将截屏内容保存到文件
                File shareDir = DiskCache.getDiskCacheDir(RecordShareActivity.this,"share");
                if(!shareDir.exists()){
                    shareDir.mkdirs();
                }
                mFile = new File(String.format("%s/%s.png", DiskCache.getDiskCacheDir(RecordShareActivity.this, "share"), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()))));
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
                fos = new FileOutputStream(mFile);
                mBackgroudCache.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.flush();
                fos.close();
                //处理完成
                mHandler.sendEmptyMessage(COMPLETE);
                mHandler.sendEmptyMessage(STOP);

                //打开分享的Dialog
                Intent intent = new Intent(RecordShareActivity.this, ShareDialog.class);
                Bundle arg = new Bundle();
                arg.putInt("Type", Constants.SHARERECORD);
                arg.putString("Url",mFile.getAbsolutePath());
                arg.putParcelable("Song",mInfo);
                intent.putExtras(arg);
                startActivity(intent);
            } catch (Exception e) {
                Message errMsg = mHandler.obtainMessage(ERROR);
                errMsg.obj = e.toString();
                mHandler.sendMessage(errMsg);
            } finally {
                if(fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    private void showLoading(){
        if(mProgressDialog != null && !mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    private void dismissLoading(String error){
        if(!TextUtils.isEmpty(error))
            ToastUtil.show(mContext,error);
        if(mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    public void onResume() {
        MobclickAgent.onPageStart(RecordShareActivity.class.getSimpleName());
        super.onResume();
        new RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if(aBoolean != mHasPermission){
                        mHasPermission = aBoolean;
                        Intent intent = new Intent(MusicService.ACTION_PERMISSION_CHANGE);
                        intent.putExtra("permission",mHasPermission);
                        sendBroadcast(intent);
                    }
                });
    }
    public void onPause() {
        MobclickAgent.onPageEnd(RecetenlyActivity.class.getSimpleName());
        super.onPause();
    }

}
