package remix.myplayer.ui.activity;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

/**
 * Created by taeja on 16-3-14.
 */

/**
 * 将分享内容与专辑封面进行处理用于分享
 */
public class RecordShareActivity extends BaseMusicActivity {

  private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 268);
  public static final String EXTRA_SONG = "Song";
  public static final String EXTRA_CONTENT = "Content";

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
  private MsgHandler mHandler;

  @OnHandleMessage
  public void handleMessage(Message msg) {
    switch (msg.what) {
      //开始处理
      case START:
        showLoading();
        break;
      //处理中止
      case STOP:
        dismissLoading("");
        break;
      //处理完成
      case COMPLETE:
        if (mFile != null) {
          ToastUtil.show(mContext, R.string.screenshot_save_at, mFile.getAbsoluteFile(),
              Toast.LENGTH_LONG);
        }
        break;
      //处理错误
      case ERROR:
        dismissLoading(getString(R.string.share_error) + ":" + msg.obj);
        break;
    }
  }

  @Override
  protected void setStatusBarColor() {
    StatusBarUtil.setTransparent(this);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_recordshare);
    ButterKnife.bind(this);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    //初始化控件
    mContainer.setDrawingCacheEnabled(true);

    mInfo = getIntent().getExtras().getParcelable(EXTRA_SONG);
    if (mInfo == null) {
      return;
    }

    new LibraryUriRequest(mImage,
        getSearchRequestWithAlbumType(mInfo),
        new RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load();

    //设置歌曲名与分享内容
    String content = getIntent().getExtras().getString(EXTRA_CONTENT);
    mContent.setText(TextUtils.isEmpty(content) ? "" : content);
    mSong.setText(String.format("《%s》", mInfo.getTitle()));
    //背景
    mBackground1.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(2))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make());
    mBackground2.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make());
    mImageBackground.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1))
        .strokeColor(Color.parseColor("#f6f6f5"))
        .make());

    mProgressDialog = Theme.getBaseDialog(mContext)
        .title(R.string.please_wait)
        .content(R.string.processing_picture)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build();

    mHandler = new MsgHandler(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mHandler.remove();
  }

  public static Bitmap getBg() {
    return mBackgroudCache;
  }

  @OnClick({R.id.recordshare_cancel, R.id.recordshare_share})
  public void onClick(View v) {
    switch (v.getId()) {
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
  private class ProcessThread extends Thread {

    FileOutputStream mFileOutputStream = null;

    @SuppressLint("SimpleDateFormat")
    @Override
    public void run() {
      //开始处理,显示进度条
      if (!mHasPermission) {
        Message errMsg = mHandler.obtainMessage(ERROR);
        errMsg.obj = mContext.getString(R.string.plz_give_access_external_storage_permission);
        mHandler.sendMessage(errMsg);
        return;
      }
      mHandler.sendEmptyMessage(START);

      mBackgroudCache = mContainer.getDrawingCache(true);
      mFile = null;
      try {
        //将截屏内容保存到文件
        File shareDir = DiskCache.getDiskCacheDir(mContext, "share");
        if (!shareDir.exists()) {
          shareDir.mkdirs();
        }
        mFile = new File(String.format("%s/%s.png", DiskCache.getDiskCacheDir(mContext, "share"),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis()))));
        if (!mFile.exists()) {
          mFile.createNewFile();
        }
        mFileOutputStream = new FileOutputStream(mFile);
        mBackgroudCache.compress(Bitmap.CompressFormat.JPEG, 80, mFileOutputStream);
        mFileOutputStream.flush();
        mFileOutputStream.close();
        //处理完成
        mHandler.sendEmptyMessage(COMPLETE);
        mHandler.sendEmptyMessage(STOP);

        //打开分享的Dialog
//                Intent intent = new Intent(mContext, ShareDialog.class);
//                Bundle arg = new Bundle();
//                arg.putInt("Type", Constants.SHARERECORD);
//                arg.putString("Url",mFile.getAbsolutePath());
//                arg.putParcelable("Song",mInfo);
//                intent.putExtras(arg);
//                startActivityForResult(intent,REQUEST_SHARE);
        startActivity(Intent.createChooser(Util.createShareImageFileIntent(mFile, mContext), null));
      } catch (Exception e) {
        Message errMsg = mHandler.obtainMessage(ERROR);
        errMsg.obj = e.toString();
        mHandler.sendMessage(errMsg);
      } finally {
        if (mFileOutputStream != null) {
          try {
            mFileOutputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private void showLoading() {
    if (mProgressDialog != null && !mProgressDialog.isShowing()) {
      mProgressDialog.show();
    }
  }

  private void dismissLoading(String error) {
    if (!TextUtils.isEmpty(error)) {
      ToastUtil.show(mContext, error);
    }
    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
    }
  }


  public void onPause() {
    super.onPause();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    setResult(Activity.RESULT_OK, data);
    finish();
  }
}
