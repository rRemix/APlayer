package remix.myplayer.listener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.Genre;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
public class AudioPopupListener implements PopupMenu.OnMenuItemClickListener{
    private Context mContext;
    private MP3Item mInfo;
    private View mEditRootView;
    @BindView(R.id.song_layout)
    @Nullable
    TextInputLayout mSongLayout;
    @BindView(R.id.album_layout)
    @Nullable
    TextInputLayout mAlbumLayout;
    @BindView(R.id.artist_layout)
    @Nullable
    TextInputLayout mArtistLayout;
    @BindView(R.id.year_layout)
    @Nullable
    TextInputLayout mYearLayout;
    @BindView(R.id.genre_layout)
    @Nullable
    TextInputLayout mGenreLayout;

    private View mDetailRootView;
    @BindView(R.id.song_detail_path)
    @Nullable
    TextView mDetailPath;
    @BindView(R.id.song_detail_name)
    @Nullable
    TextView mDetailName;
    @BindView(R.id.song_detail_size)
    @Nullable
    TextView mDetailSize;
    @BindView(R.id.song_detail_mime)
    @Nullable
    TextView mDetailMime;
    @BindView(R.id.song_detail_duration)
    @Nullable
    TextView mDetailDuration;
    @BindView(R.id.song_detail_bit_rate)
    @Nullable
    TextView mDetailBitRate;
    @BindView(R.id.song_detail_sample_rate)
    @Nullable
    TextView mDetailSampleRate;

    private Genre mGenreInfo;

    public AudioPopupListener(Context context,MP3Item info){
        mContext = context;
        mInfo = info;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        MobclickAgent.onEvent(mContext,item.getItemId() == R.id.menu_edit ? "SongEdit" : "SongDetail" );
        switch (item.getItemId()){
            case R.id.menu_edit:
                MaterialDialog editDialog = new MaterialDialog.Builder(mContext)
                        .title("音乐标签编辑")
                        .customView(R.layout.dialog_song_edit,true)
//                        .inputType()
                        .negativeText(R.string.cancel)
                        .negativeColorRes(R.color.black)
                        .positiveText(R.string.confirm)
                        .positiveColorRes(R.color.black)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                String title = "",artist = "",album = "",genre = "",year = "";
                                title = mSongLayout.getEditText() != null ? mSongLayout.getEditText().getText().toString() : mInfo.getTitle();
                                if(TextUtils.isEmpty(title)){
                                    ToastUtil.show(mContext,R.string.song_not_empty);
                                    return;
                                }
                                artist = mArtistLayout.getEditText() != null ? mArtistLayout.getEditText().getText().toString() : "未知歌手";
                                album = mAlbumLayout.getEditText() != null ? mAlbumLayout.getEditText().getText().toString() : "未知歌曲";
                                year = mYearLayout.getEditText() != null ? mYearLayout.getEditText().getText().toString() : " ";
                                genre = mGenreLayout.getEditText() != null ? mGenreLayout.getEditText().getText().toString() : "";

                                int updateRow = -1;
                                int updateGenreRow = -1;
                                try {
                                    //更新歌曲等信息
                                    updateRow = MediaStoreUtil.updateMP3Info(mInfo.getId(),title,artist,album,year);
                                    //更新流派信息
                                    //先判断是否存在该流派，如果不存在先新建该流派，再建立歌曲与流派的映射
                                    if(mGenreInfo.GenreID > 0){
                                        updateGenreRow = MediaStoreUtil.updateGenre(mGenreInfo.GenreID,genre);
                                    }
                                    else {
                                        long genreId = MediaStoreUtil.insertGenre(genre);
                                        if(genreId != -1){
                                            updateGenreRow = MediaStoreUtil.insertGenreMap(mInfo.getId(),(int)genreId) ? 1 : -1;
                                        }
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                if(updateGenreRow > 0 && updateRow > 0){
                                    ToastUtil.show(mContext,R.string.save_success);
                                    mInfo.setAlbum(album);
                                    mInfo.setArtist(artist);
                                    mInfo.setTitle(title);
                                    ((AudioHolderActivity)mContext).UpdateTopStatus(mInfo);
                                    ((AudioHolderActivity)mContext).setMP3Item(mInfo);
                                } else {
                                    ToastUtil.show(mContext,R.string.save_error);
                                }
                            }
                        }).build();
                editDialog.show();

                mEditRootView = editDialog.getCustomView();
                if(mEditRootView != null){
                    ButterKnife.bind(AudioPopupListener.this, mEditRootView);

                    if(mSongLayout.getEditText() != null){
                        mSongLayout.setHintTextAppearance(Theme.getTheme(true));
                        mSongLayout.getEditText().addTextChangedListener(new TextInputEditWatcher(mSongLayout,"歌曲名不能为空"));
                        mSongLayout.getEditText().setText(mInfo.getTitle());
                    }
                    if(mAlbumLayout.getEditText() != null) {
                        mAlbumLayout.setHintTextAppearance(Theme.getTheme(true));
                        mAlbumLayout.getEditText().setText(mInfo.getAlbum());
                    }
                    if(mArtistLayout.getEditText() != null) {
                        mArtistLayout.setHintTextAppearance(Theme.getTheme(true));
                        mArtistLayout.getEditText().setText(mInfo.getArtist());
                    }
                    if(mYearLayout.getEditText() != null){
                        mYearLayout.setHintTextAppearance(Theme.getTheme(true));
                        mYearLayout.getEditText().setText(mInfo.getYear() + "");
                    }
                    mGenreInfo = MediaStoreUtil.getGenre(mInfo.getId());
                    if(mGenreLayout.getEditText() != null){
                        mGenreLayout.setHintTextAppearance(Theme.getTheme(true));
                        mGenreLayout.getEditText().setText(mGenreInfo.GenreName);
                    }
                }
                break;

            case R.id.menu_detail:
                MaterialDialog detailDialog = new MaterialDialog.Builder(mContext)
                        .title("歌曲详情")
                        .customView(R.layout.dialog_song_detail,true)
                        .positiveText(R.string.confirm)
                        .positiveColorRes(R.color.black)
                        .build();
                detailDialog.show();
                mDetailRootView = detailDialog.getCustomView();
                if(mDetailRootView != null){
                    ButterKnife.bind(AudioPopupListener.this,mDetailRootView);

                    //歌曲路径
                    if(mDetailPath != null)
                        mDetailPath.setText(mInfo.getUrl());
                    //歌曲名称
                    if(mDetailName != null)
                        mDetailName.setText(mInfo.getDisplayname());
                    //歌曲大小
                    if(mDetailSize != null)
                        mDetailSize.setText(mInfo.getSize() / 1024 / 1024 + "MB");
                    //歌曲格式
                    if(mDetailMime != null){
                        String path = mInfo.getUrl();
                        String extension;
                        if(path.lastIndexOf('.') > -1 && path.lastIndexOf('.') < path.length() - 1){
                            extension = mInfo.getUrl().substring(mInfo.getUrl().lastIndexOf('.') + 1 ,mInfo.getUrl().length());
                        } else {
                            extension = CommonUtil.getType(MusicService.getRateInfo(Constants.MIME));
                        }
                        mDetailMime.setText(extension);
                    }
                    //歌曲时长
                    if(mDetailDuration != null)
                        mDetailDuration.setText(CommonUtil.getTime(mInfo.getDuration()));
                    //歌曲码率
                    if(mDetailBitRate != null)
                        mDetailBitRate.setText(MusicService.getRateInfo(Constants.BIT_RATE) + " kb/s");
                    //歌曲采样率
                    if(mDetailSampleRate != null)
                        mDetailSampleRate.setText(MusicService.getRateInfo(Constants.SAMPLE_RATE) + " Hz");

                }
                break;
            case R.id.menu_timer:
                mContext.startActivity(new Intent(mContext, TimerDialog.class));
                break;
            case R.id.menu_eq:
                MobclickAgent.onEvent(mContext,"EQ");
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                if(CommonUtil.isIntentAvailable(mContext,audioEffectIntent)){
                    ((Activity)mContext).startActivityForResult(audioEffectIntent, 0);
                } else {
                    mContext.startActivity(new Intent(mContext,EQActivity.class));
                }
                break;
            case R.id.menu_delete:
                try {
                    new MaterialDialog.Builder(mContext)
                            .content(mContext.getString(R.string.confirm_delete_playlist,"曲库"))
                            .positiveText(R.string.confirm)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                                    if(MediaStoreUtil.delete(mInfo.getId() , Constants.SONG)){
                                        if(PlayListUtil.deleteSong(mInfo.getId(), Global.mPlayQueueID)){
                                            ToastUtil.show(mContext, mContext.getString(R.string.delete_success));

                                        }
                                    } else {
                                        ToastUtil.show(mContext, mContext.getString(R.string.delete_error));
                                    }
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
        return true;
    }

    class TextInputEditWatcher implements TextWatcher{
        private TextInputLayout mInputLayout;
        private String mError;
        public TextInputEditWatcher(TextInputLayout layout,String error){
            mError = error;
            mInputLayout = layout;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s == null || TextUtils.isEmpty(s.toString())){
                mInputLayout.setError(mError);
            }else {
                mInputLayout.setError("");
            }
        }
    }
}
