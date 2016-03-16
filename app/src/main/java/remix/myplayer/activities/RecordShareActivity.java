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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.ui.SharePopupWindow;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DensityUtil;

/**
 * Created by taeja on 16-3-14.
 */
public class RecordShareActivity extends AppCompatActivity {
    public static RecordShareActivity mInstance;
    private ImageView mImage;
    private TextView mSongArtist;
    private TextView mContent;
    private View mView;
    private static Bitmap mBackgroudCache;
    private Toolbar mToolBar;
    private RelativeLayout mContainer;
    private MP3Info mInfo;
    private ProgressDialog mProgressDialog;
    private final int START = 0;
    private final int STOP = 1;
    private final int COMPLETE = 2;
    private final int ERROR = 3;
    private File mFile;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:
                    mProgressDialog = ProgressDialog.show(RecordShareActivity.this,"请稍候","图片处理中",true,false);
                    break;
                case STOP:
                    if(mProgressDialog != null)
                        mProgressDialog.dismiss();
                    break;
                case COMPLETE:
                    if(mFile != null)
                        Toast.makeText(RecordShareActivity.this, "截屏文件已保存至" + mFile.getAbsolutePath(),Toast.LENGTH_LONG).show();
                    break;
                case ERROR:
                    Toast.makeText(RecordShareActivity.this,"分享失败",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.activity_recordshare);

        mImage = (ImageView)findViewById(R.id.recordshare_image);
        mSongArtist = (TextView)findViewById(R.id.recordshare_name);
        mContent = (TextView)findViewById(R.id.recordshare_content);
        mView = getWindow().getDecorView();
        mContainer = (RelativeLayout)findViewById(R.id.recordshare_container);
        mContainer.setDrawingCacheEnabled(true);

        findViewById(R.id.recordshare_content_container).setAlpha((float)0.8);

        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        if(mInfo == null)
            return;

        mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
        mContent.setText(getIntent().getExtras().getString("Content"));
        mSongArtist.setText(" " + mInfo.getDisplayname() + " ");
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
            mHandler.sendEmptyMessage(START);
            mBackgroudCache = mContainer.getDrawingCache(true);
            mFile = null;
            try {
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
            Intent intent = new Intent(RecordShareActivity.this, SharePopupWindow.class);
            Bundle arg = new Bundle();
            arg.putInt("Type", Constants.SHARERECORD);
            arg.putString("Url",mFile.getAbsolutePath());
            arg.putSerializable("MP3Info",mInfo);
            intent.putExtras(arg);
            startActivity(intent);
        }
    }

}
