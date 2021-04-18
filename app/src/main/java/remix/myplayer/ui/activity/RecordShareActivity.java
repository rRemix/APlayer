package remix.myplayer.ui.activity;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.afollestad.materialdialogs.MaterialDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ActivityRecordshareBinding;
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

  private ActivityRecordshareBinding binding;

  private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 268);
  public static final String EXTRA_SONG = "Song";
  public static final String EXTRA_CONTENT = "Content";

  //保存截屏
  private static Bitmap mBackgroudCache;

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
          ToastUtil.show(this, R.string.screenshot_save_at, mFile.getAbsoluteFile(),
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
    binding = ActivityRecordshareBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
    //初始化控件
    binding.recordshareContainer.setDrawingCacheEnabled(true);

    mInfo = getIntent().getExtras().getParcelable(EXTRA_SONG);
    if (mInfo == null) {
      return;
    }

    new LibraryUriRequest(binding.recordshareImage,
        getSearchRequestWithAlbumType(mInfo),
        new RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load();

    //设置歌曲名与分享内容
    String content = getIntent().getExtras().getString(EXTRA_CONTENT);
    binding.recordshareContent.setText(TextUtils.isEmpty(content) ? "" : content);
    binding.recordshareName.setText(String.format("《%s》", mInfo.getTitle()));
    //背景
    binding.recordshareBackground1.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(2))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make());
    binding.recordshareBackground2.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make());
    binding.recordshareImageContainer.setBackground(new GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1))
        .strokeColor(Color.parseColor("#f6f6f5"))
        .make());

    mProgressDialog = Theme.getBaseDialog(this)
        .title(R.string.please_wait)
        .content(R.string.processing_picture)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build();

    binding.recordshareCancel.setOnClickListener(v -> finish());
    binding.recordshareShare.setOnClickListener(v -> new ProcessThread().start());
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

  /**
   * 将图片保存到本地
   */
  private class ProcessThread extends Thread {

    FileOutputStream mFileOutputStream = null;

    @SuppressLint("SimpleDateFormat")
    @Override
    public void run() {
      //开始处理,显示进度条
      if (!hasPermission) {
        Message errMsg = mHandler.obtainMessage(ERROR);
        errMsg.obj = getString(R.string.plz_give_access_external_storage_permission);
        mHandler.sendMessage(errMsg);
        return;
      }
      mHandler.sendEmptyMessage(START);

      mBackgroudCache = binding.recordshareContainer.getDrawingCache(true);
      mFile = null;
      try {
        //将截屏内容保存到文件
        File shareDir = DiskCache.getDiskCacheDir(RecordShareActivity.this, "share");
        if (!shareDir.exists()) {
          shareDir.mkdirs();
        }
        mFile = new File(String.format("%s/%s.png", DiskCache.getDiskCacheDir(RecordShareActivity.this, "share"),
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
//                Intent intent = new Intent(this, ShareDialog.class);
//                Bundle arg = new Bundle();
//                arg.putInt("Type", Constants.SHARERECORD);
//                arg.putString("Url",mFile.getAbsolutePath());
//                arg.putParcelable("Song",mInfo);
//                intent.putExtras(arg);
//                startActivityForResult(intent,REQUEST_SHARE);
        startActivity(Intent.createChooser(Util.createShareImageFileIntent(mFile, RecordShareActivity.this), null));
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
      ToastUtil.show(this, error);
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
