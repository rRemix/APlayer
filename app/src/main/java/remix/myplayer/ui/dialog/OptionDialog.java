package remix.myplayer.ui.dialog;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
public class OptionDialog extends BaseDialogActivity {
    //添加 设置铃声 分享 删除按钮
    @BindView(R.id.popup_add)
    ImageView mAdd;
    @BindView(R.id.popup_ring)
    ImageView mRing;
    @BindView(R.id.popup_share)
    ImageView mShare;
    @BindView(R.id.popup_delete)
    ImageView mDelete;

    //标题
    @BindView(R.id.popup_title)
    TextView mTitle;
    //专辑封面
    @BindView(R.id.popup_image)
    SimpleDraweeView mDraweeView;

    //当前正在播放的歌曲
    private MP3Item mInfo = null;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_option);
        ButterKnife.bind(this);

        mInfo = (MP3Item)getIntent().getExtras().getSerializable("MP3Item");
        if(mInfo == null)
            return;
        if(mIsDeletePlayList = getIntent().getExtras().getBoolean("IsDeletePlayList", false)){
            mPlayListName = getIntent().getExtras().getString("PlayListName");
        }

        //设置歌曲名与封面
        mTitle.setText(mInfo.getTitle() + "-" + mInfo.getArtist());
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

    }

    @OnClick({R.id.popup_add,R.id.popup_share,R.id.popup_delete,R.id.popup_ring})
    public void onClick(View v){
        switch (v.getId()){
            //添加到播放列表
            case R.id.popup_add:
                Intent intentAdd = new Intent(OptionDialog.this,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putString("SongName",mInfo.getTitle());
                ardAdd.putLong("Id",mInfo.getId());
                ardAdd.putLong("AlbumId",mInfo.getAlbumId());
                ardAdd.putString("Artist",mInfo.getArtist());
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                finish();
                break;
            //设置铃声
            case R.id.popup_ring:
                setRing(mInfo.getUrl(), (int) mInfo.getId());
                finish();
                break;
            //分享
            case R.id.popup_share:
                Intent intentShare = new Intent(OptionDialog.this,ShareDialog.class);
                Bundle argShare = new Bundle();
                argShare.putSerializable("MP3Item",mInfo);
                argShare.putInt("Type",Constants.SHARESONG);
                intentShare.putExtras(argShare);
                startActivity(intentShare);
                finish();
                break;
            //删除
            case R.id.popup_delete:
                try {
                    String title = mIsDeletePlayList ? getString(R.string.confirm_delete_playlist) :getString(R.string.confirm_delete_song);
                    new MaterialDialog.Builder(OptionDialog.this)
                            .content(title)
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    String result = "";
                                    if(!mIsDeletePlayList){
                                        result = DBUtil.deleteSong(mInfo.getId() , Constants.SONG) ?
                                                getString(R.string.delete_success) :
                                                getString(R.string.delete_error);
                                    } else {
                                        result = DBUtil.deleteSongInPlayList(mPlayListName,mInfo.getId()) ?
                                                getString(R.string.delete_success):
                                                getString(R.string.delete_error);
                                    }
                                    Toast.makeText(OptionDialog.this,result,Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                }
                            })
                            .backgroundColor(ThemeStore.getBackgroundColor3())
                            .positiveColor(ThemeStore.getTextColorPrimary())
                            .negativeColor(ThemeStore.getTextColorPrimary())
                            .contentColor(ThemeStore.getTextColorPrimary())
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;

        }
    }


    /**
     * 设置铃声
     * @param path
     * @param Id
     */
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
