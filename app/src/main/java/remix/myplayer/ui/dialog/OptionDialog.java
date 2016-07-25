package remix.myplayer.ui.dialog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.model.MP3Item;
import remix.myplayer.ui.activity.BaseAppCompatActivity;
import remix.myplayer.ui.customview.CircleImageView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
public class OptionDialog extends BaseAppCompatActivity {
    //添加 设置铃声 分享 删除按钮
    @ViewInject(R.id.popup_add)
    private ImageView mAdd;
    @ViewInject(R.id.popup_ring)
    private ImageView mRing;
    @ViewInject(R.id.popup_share)
    private ImageView mShare;
    @ViewInject(R.id.popup_delete)
    private ImageView mDelete;
    private Button mCancel;
    //标题
    @ViewInject(R.id.popup_title)
    private TextView mTitle;
    //专辑封面
    @ViewInject(R.id.popup_image)
    private SimpleDraweeView mDraweeView;

    //当前正在播放的歌曲
    private MP3Item mInfo = null;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;

    @Override
    public int getLayoutId() {
        return R.layout.popup_option;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mInfo = (MP3Item)getIntent().getExtras().getSerializable("MP3Item");
        if(mInfo == null)
            return;
        if(mIsDeletePlayList = getIntent().getExtras().getBoolean("IsDeletePlayList", false)){
            mPlayListName = getIntent().getExtras().getString("PlayListName");
        }

        //设置歌曲名与封面
        mTitle.setText(mInfo.getDisplayname() + "-" + mInfo.getArtist());
        mDraweeView.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mInfo.getAlbumId()));

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
                arg.putSerializable("MP3Item",mInfo);
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
                    String title = mIsDeletePlayList ? getString(R.string.confirm_delete_playlist) :getString(R.string.confirm_delete_song);

//                    AlertDialog alertDialog = new AlertDialog.Builder(OptionDialog.this)
//                            .setTitle(title)
//                            .setPositiveButton("确认", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    String result = "";
//                                    if(!mIsDeletePlayList){
//                                        result = DBUtil.deleteSong(mInfo.getImgUrl(), Constants.DELETE_SINGLE) == true ? "删除成功" : "删除失败";
//                                    } else {
//                                        result = DBUtil.deleteSongInPlayList(mPlayListName,mInfo.getId()) ? "删除成功" : "删除失败";
//                                    }
//                                    Toast.makeText(OptionDialog.this,result,Toast.LENGTH_SHORT).show();
//                                    finish();
//                                }
//                            })
//                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                }
//                            }).create();
//                    alertDialog.show();


                    View delete = LayoutInflater.from(OptionDialog.this).inflate(R.layout.popup_delete,null);
                    ((TextView)delete.findViewById(R.id.delete_title)).setText(title);
                    delete.findViewById(R.id.delete_cancel).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                    delete.findViewById(R.id.delete_confirm).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String result = "";
                            if(!mIsDeletePlayList){
                                result = DBUtil.deleteSong(mInfo.getUrl(), Constants.DELETE_SINGLE) == true ? getString(R.string.delete_success) :
                                        getString(R.string.delete_error);
                            } else {
                                result = DBUtil.deleteSongInPlayList(mPlayListName,mInfo.getId()) ? getString(R.string.delete_success):
                                        getString(R.string.delete_error);
                            }
                            Toast.makeText(OptionDialog.this,result,Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });

                    AlertDialog alertDialog = new AlertDialog.Builder(OptionDialog.this).setView(delete).create();
                    alertDialog.show();

                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

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
            Toast.makeText( getApplicationContext (),getString(R.string.set_ringtone_success), Toast.LENGTH_SHORT ).show();
        }
        else
            Toast.makeText( getApplicationContext (),getString(R.string.set_ringtone_error), Toast.LENGTH_SHORT ).show();
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
