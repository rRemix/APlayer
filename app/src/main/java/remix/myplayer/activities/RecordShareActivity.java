package remix.myplayer.activities;

import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.ui.dialog.ShareDialog;
import remix.myplayer.utils.Constants;

/**
 * Created by taeja on 16-3-14.
 */

/**
 * 将分享内容与专辑封面进行处理用于分享
 */
public class RecordShareActivity extends BaseAppCompatActivity {
    public static RecordShareActivity mInstance;
    private ImageView mImage;

    //歌曲名与分享内容
    private TextView mSong;
    private TextView mContent;
    //保存截屏
    private static Bitmap mBackgroudCache;
    private RelativeLayout mContainer;
    //当前正在播放的歌曲
    private MP3Info mInfo;
    //处理图片的进度条
    private ProgressDialog mProgressDialog;
    //处理状态
    private final int START = 0;
    private final int STOP = 1;
    private final int COMPLETE = 2;
    private final int ERROR = 3;
    //截屏文件
    private File mFile;
    //更新处理结果的Handler
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                //开始处理
                case START:
                    mProgressDialog = ProgressDialog.show(RecordShareActivity.this,"请稍候","图片处理中",true,false);
                    break;
                //处理中止
                case STOP:
                    if(mProgressDialog != null)
                        mProgressDialog.dismiss();
                    break;
                //处理完成
                case COMPLETE:
                    if(mFile != null)
                        Toast.makeText(RecordShareActivity.this, "截屏文件已保存至" + mFile.getAbsolutePath(),Toast.LENGTH_LONG).show();
                    break;
                //处理错误
                case ERROR:
                    Toast.makeText(RecordShareActivity.this,getString(R.string.share_error),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.activity_recordshare);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //初始化控件
        mImage = (ImageView)findViewById(R.id.recordshare_image);
        mSong = (TextView)findViewById(R.id.recordshare_name);
        mContent = (TextView)findViewById(R.id.recordshare_content);

        mContainer = (RelativeLayout)findViewById(R.id.recordshare_container);
        mContainer.setDrawingCacheEnabled(true);

//        findViewById(R.id.recordshare_content_container).setAlpha((float)0.8);

        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        if(mInfo == null)
            return;
        //设置专辑封面
        mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
        //设置歌曲名与分享内容
        mContent.setText(getIntent().getExtras().getString("Content"));
        mSong.setText(" " + mInfo.getDisplayname() + " ");
    }

    public static Bitmap getBg(){
        return mBackgroudCache;
    }

    public void onShare(View v){
        new ProcessThread().start();
    }

    public void onCancel(View v){
        finish();
    }

    class ProcessThread extends Thread{
        FileOutputStream fos = null;
        @Override
        public void run() {
            //开始处理,显示进度条
            mHandler.sendEmptyMessage(START);
            mBackgroudCache = mContainer.getDrawingCache(true);
            mFile = null;
            try {
                //将截屏内容保存到文件
                mFile = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + getPackageName() + "/record.png");
                if (!mFile.exists()) {
                    mFile.createNewFile();
                }
                fos = new FileOutputStream(mFile);
                if (null != fos) {
                    mBackgroudCache.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.flush();
                    fos.close();
                }
                //处理完成
                mHandler.sendEmptyMessage(COMPLETE);
                mHandler.sendEmptyMessage(STOP);

            } catch (Exception e) {
                mHandler.sendEmptyMessage(ERROR);
                e.printStackTrace();
            } finally {
                if(fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            //打开分享的Dialog
            Intent intent = new Intent(RecordShareActivity.this, ShareDialog.class);
            Bundle arg = new Bundle();
            arg.putInt("Type", Constants.SHARERECORD);
            arg.putString("Url",mFile.getAbsolutePath());
            arg.putSerializable("MP3Info",mInfo);
            intent.putExtras(arg);
            startActivity(intent);
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
