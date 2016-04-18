package remix.myplayer.ui.dialog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sina.weibo.sdk.api.share.Base;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.activities.BaseAppCompatActivity;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.ui.customviews.CircleImageView;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.DensityUtil;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
public class OptionDialog extends BaseAppCompatActivity {
    //添加 设置铃声 分享 删除按钮
    private ImageView mAdd;
    private ImageView mRing;
    private ImageView mShare;
    private ImageView mDelete;
    private Button mCancel;
    //标题
    private TextView mTitle;
    //当前正在播放的歌曲
    private MP3Info mInfo = null;
    //专辑封面
    private CircleImageView mCircleView;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_option);

        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        if(mInfo == null)
            return;
        if(mIsDeletePlayList = getIntent().getExtras().getBoolean("IsDeletePlayList", false)){
            mPlayListName = getIntent().getExtras().getString("PlayListName");
        }

        //设置歌曲名与封面
        mTitle = (TextView)findViewById(R.id.popup_title);
        mTitle.setText(mInfo.getDisplayname() + "-" + mInfo.getArtist());
        mCircleView = (CircleImageView)findViewById(R.id.popup_image);
        ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" + mInfo.getAlbumId(),
                mCircleView);

        //置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        mAdd = (ImageButton)findViewById(R.id.popup_add);
        mRing= (ImageButton)findViewById(R.id.popup_ring);
        mShare = (ImageButton)findViewById(R.id.popup_share);
        mDelete= (ImageButton)findViewById(R.id.popup_delete);
//        mCancel= (Button)findViewById(R.id.popup_cancel);
        //添加到播放列表
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OptionDialog.this,AddtoPlayListDialog.class);
                Bundle arg = new Bundle();
                arg.putString("SongName",mInfo.getDisplayname());
                arg.putLong("Id",mInfo.getId());
                arg.putLong("AlbumId",mInfo.getAlbumId());
                arg.putString("Artist",mInfo.getArtist());
                intent.putExtras(arg);
                startActivity(intent);
                finish();
            }
        });
        //设置手机铃声
        mRing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRing(mInfo.getUrl(), (int) mInfo.getId());
                finish();
            }
        });
        //分享
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OptionDialog.this,ShareDialog.class);
                Bundle arg = new Bundle();
                arg.putSerializable("MP3Info",mInfo);
                arg.putInt("Type",Constants.SHARESONG);
                intent.putExtras(arg);
                startActivity(intent);
                finish();
            }
        });
        //删除
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String title = mIsDeletePlayList ? "确认从播放列表移除?" : "确认删除?";
                    new AlertDialog.Builder(OptionDialog.this)
                            .setTitle(title)
                            .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String result = "";
                                    if(!mIsDeletePlayList){
                                        result = DBUtil.deleteSong(mInfo.getUrl(), Constants.DELETE_SINGLE) == true ? "删除成功" : "删除失败";
                                    } else {
                                        result = DBUtil.deleteSongInPlayList(mPlayListName,mInfo.getId()) ? "删除成功" : "删除失败";
                                    }
                                    Toast.makeText(OptionDialog.this,result,Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).create().show();
                } catch (Exception e){
                    e.printStackTrace();
                }

//                String result = DBUtil.deleteSong(mInfo.getUrl(), Constants.DELETE_SINGLE) == true ? "删除成功" : "删除失败";
//                Toast.makeText(OptionDialog.this,result,Toast.LENGTH_SHORT).show();
//                finish();


            }
        });
//        mCancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
    }


    private void setRing(String path, int Id) {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        cv.put(MediaStore.Audio.Media.IS_ALARM, false);
        cv.put(MediaStore.Audio.Media.IS_MUSIC, false);
        // 把需要设为铃声的歌曲更新铃声库
        if(getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns.DATA + "=?", new String[]{path}) > 0) {
            Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Id);
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
            Toast.makeText( getApplicationContext (),"设置铃声成功!", Toast.LENGTH_SHORT ).show();
        }
        else
            Toast.makeText( getApplicationContext (),"设置铃声失败!", Toast.LENGTH_SHORT ).show();
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
