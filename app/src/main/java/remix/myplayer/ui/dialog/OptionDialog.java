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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 歌曲的选项对话框
 */
public class OptionDialog extends BaseDialogActivity {
    //添加 设置铃声 分享 删除按钮
    @BindView(R.id.popup_add)
    View mAdd;
    @BindView(R.id.popup_ring)
    View mRing;
    @BindView(R.id.popup_share)
    View mShare;
    @BindView(R.id.popup_delete)
    View mDelete;

    //标题
    @BindView(R.id.popup_title)
    TextView mTitle;
    //专辑封面
    @BindView(R.id.popup_image)
    SimpleDraweeView mDraweeView;

    //当前正在播放的歌曲
    private Song mInfo = null;
    //是否是删除播放列表中歌曲
    private boolean mIsDeletePlayList = false;
    //播放列表名字
    private String mPlayListName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_option);
        ButterKnife.bind(this);

        mInfo = (Song)getIntent().getExtras().getSerializable("Song");
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
//        w.setWindowAnimations(R.style.AnimBottom);
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        //为按钮着色
        final int tintColor = ThemeStore.isDay() ?
                ColorUtil.getColor(R.color.day_textcolor_primary ) :
                ColorUtil.getColor(R.color.white_f4f4f5);
        ((ImageView)findViewById(R.id.popup_add_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_add2list),tintColor));
        ((ImageView)findViewById(R.id.popup_ring_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_ring),tintColor));
        ((ImageView)findViewById(R.id.popup_share_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_share),tintColor));
        ((ImageView)findViewById(R.id.popup_delete_img)).setImageDrawable(Theme.TintDrawable(getResources().getDrawable(R.drawable.pop_btn_delete),tintColor));

        ButterKnife.apply( new TextView[]{findView(R.id.popup_add_text),findView(R.id.popup_ring_text),
                        findView(R.id.popup_share_text),findView(R.id.popup_delete_text)},
                new ButterKnife.Action<TextView>(){
                    @Override
                    public void apply(@NonNull TextView textView, int index) {
                        textView.setTextColor(tintColor);
                    }
                });
    }

    @OnClick({R.id.popup_add,R.id.popup_share,R.id.popup_delete,R.id.popup_ring})
    public void onClick(View v){
        switch (v.getId()){
            //添加到播放列表
            case R.id.popup_add:
                MobclickAgent.onEvent(this,"AddtoPlayList");
                Intent intentAdd = new Intent(OptionDialog.this,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putInt("Id",mInfo.getId());
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                finish();
                break;
            //设置铃声
            case R.id.popup_ring:
                MobclickAgent.onEvent(this,"Ring");
                setRing(mInfo.getId());
                finish();
                break;
            //分享
            case R.id.popup_share:
                MobclickAgent.onEvent(this,"Share");
                Intent intentShare = new Intent(OptionDialog.this,ShareDialog.class);
                Bundle argShare = new Bundle();
                argShare.putSerializable("Song",mInfo);
                argShare.putInt("Type",Constants.SHARESONG);
                intentShare.putExtras(argShare);
                startActivity(intentShare);
                finish();
                break;
            //删除
            case R.id.popup_delete:
                MobclickAgent.onEvent(this,"Delete");
                try {
                    String title = getString(R.string.confirm_delete_from_playlist_or_library,mIsDeletePlayList ? mPlayListName : "曲库");
                    new MaterialDialog.Builder(OptionDialog.this)
                            .content(title)
                            .buttonRippleColor(ThemeStore.getRippleColor())
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    boolean deleteSuccess = !mIsDeletePlayList ?
                                            MediaStoreUtil.delete(mInfo.getId() , Constants.SONG) > 0 :
                                            PlayListUtil.deleteSong(mInfo.getId(),mPlayListName);
//                                    if(deleteSuccess){
//                                        sendBroadcast(new Intent(MusicService.ACTION_MEDIA_CHANGE));
//                                    }
                                    ToastUtil.show(OptionDialog.this,deleteSuccess ? R.string.delete_success : R.string.delete_error);
                                    finish();
                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                }
                            })
                            .backgroundColorAttr(R.attr.background_color_3)
                            .positiveColorAttr(R.attr.text_color_primary)
                            .negativeColorAttr(R.attr.text_color_primary)
                            .contentColorAttr(R.attr.text_color_primary)
                            .show();
                } catch (Exception e){
                    e.printStackTrace();
                }
                break;

        }
    }


    /**
     * 设置铃声
     * @param audioId
     */
    private void setRing(int audioId) {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
        cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
        cv.put(MediaStore.Audio.Media.IS_ALARM, false);
        cv.put(MediaStore.Audio.Media.IS_MUSIC, false);
        // 把需要设为铃声的歌曲更新铃声库
        if(getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv, MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) > 0) {
            Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, newUri);
            ToastUtil.show(this,R.string.set_ringtone_success);
        }
        else
            ToastUtil.show(this,R.string.set_ringtone_error);
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
