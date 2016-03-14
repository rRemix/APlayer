package remix.myplayer.activities;

import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.SharePopupWindow;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-3-14.
 */
public class RecordShareActivity extends AppCompatActivity {
    private ImageView mImage;
    private TextView mText;
    private View mView;
    private static Bitmap mBackgroudCache;
    private Toolbar mToolBar;
    private LinearLayout mContainer;
    private MP3Info mInfo;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordshare);
        initToolbar();
        mImage = (ImageView)findViewById(R.id.share_image);
        mText = (TextView)findViewById(R.id.share_text);
        mView = getWindow().getDecorView();
        mContainer = (LinearLayout)findViewById(R.id.record_container);
        mContainer.setDrawingCacheEnabled(true);
        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        if(mInfo == null)
            return;
        Bitmap bitmap = DBUtil.CheckBitmapByAlbumId((int)mInfo.getAlbumId(),false);

        mImage.setImageBitmap(bitmap == null ? BitmapFactory.decodeResource(getResources(),R.drawable.artist_empty_bg) : bitmap);
//        mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), temp.getAlbumId()));
        mText.setText(getIntent().getExtras().getString("Content"));
    }

    public static Bitmap getBg(){
        return mBackgroudCache;
    }

    public void onShare(View v){
        mBackgroudCache = mContainer.getDrawingCache(true);
        mImage.setImageBitmap(mBackgroudCache);

        File file = null;
        try {
            file = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + getPackageName() + "/record.png");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            if (null != fos) {
                mBackgroudCache.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
                Toast.makeText(this, "截屏文件已保存至" + file.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"分享失败",Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SharePopupWindow.class);
        Bundle arg = new Bundle();
        arg.putInt("Type", Constants.SHARERECORD);
//        arg.putSerializable("Bitmap",mBackgroudCache);
        arg.putString("Url",file.getAbsolutePath());
        arg.putSerializable("MP3Info",mInfo);
        intent.putExtras(arg);
        startActivity(intent);
    }

    private void initToolbar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("分享");
        mToolBar.setTitleTextColor(Color.parseColor("#ffffffff"));
        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.common_btn_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(RecordShareActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(RecordShareActivity.this, TimerPopupWindow.class));
                        break;
                }
                return true;
            }
        });
    }
}
