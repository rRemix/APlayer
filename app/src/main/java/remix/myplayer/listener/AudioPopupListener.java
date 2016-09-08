package remix.myplayer.listener;

import android.content.ContentUris;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.Genre;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;

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
        switch (item.getItemId()){
            case R.id.menu_edit:
                MaterialDialog editDialog = new MaterialDialog.Builder(mContext)
                        .title("音乐标签编辑")
                        .customView(R.layout.dialog_song_edit,true)
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
                                    Toast.makeText(mContext,"歌曲名不能为空",Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                artist = mArtistLayout.getEditText() != null ? mArtistLayout.getEditText().getText().toString() : "未知歌手";
                                album = mAlbumLayout.getEditText() != null ? mAlbumLayout.getEditText().getText().toString() : "未知歌曲";
                                year = mYearLayout.getEditText() != null ? mYearLayout.getEditText().getText().toString() : " ";
                                genre = mGenreLayout.getEditText() != null ? mGenreLayout.getEditText().getText().toString() : "";

                                int updateRow = -1;
                                int updateGenreRow = -1;
                                try {
                                    updateRow = DBUtil.updateMP3Info(mInfo.getId(),title,artist,album,year);
                                    if(mGenreInfo.GenreID > 0){
                                        updateGenreRow = DBUtil.updateGenre(mGenreInfo.GenreID,genre);
                                    }
                                    else {
                                        Uri newUri = DBUtil.insertGenre(mInfo.getId(),genre);
                                        long genreId = ContentUris.parseId(newUri);
                                        if(genreId > 0){
                                            updateGenreRow = DBUtil.updateGenre(genreId,genre);
                                        }
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                } finally {
                                    if(updateGenreRow > 0 && updateRow > 0){
                                        Toast.makeText(mContext, "保存成功" ,Toast.LENGTH_SHORT).show();
                                        mInfo.setAlbum(album);
                                        mInfo.setArtist(artist);
                                        mInfo.setTitle(title);
                                        ((AudioHolderActivity)mContext).UpdateTopStatus(mInfo);
                                        ((AudioHolderActivity)mContext).setMP3Item(mInfo);
                                    } else {
                                        Toast.makeText(mContext, "保存失败" ,Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }).build();
                editDialog.show();

                mEditRootView = editDialog.getCustomView();
                if(mEditRootView != null){
                    ButterKnife.bind(AudioPopupListener.this, mEditRootView);
                    if(mSongLayout.getEditText() != null){
                        mSongLayout.getEditText().addTextChangedListener(new TextInputEditWatcher(mSongLayout,"歌曲名不能为空"));
                        mSongLayout.getEditText().setText(mInfo.getTitle());
                    }
                    if(mAlbumLayout.getEditText() != null) {
                        mAlbumLayout.getEditText().setText(mInfo.getAlbum());
                    }
                    if(mArtistLayout.getEditText() != null) {
                        mArtistLayout.getEditText().setText(mInfo.getArtist());
                    }
                    if(mYearLayout.getEditText() != null){
                        mYearLayout.getEditText().setText(mInfo.getYear() + "");
                    }
                    mGenreInfo = DBUtil.getGenre(mInfo.getId());
                    if(mGenreLayout.getEditText() != null){
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

                    if(mDetailPath != null)
                        mDetailPath.setText(mInfo.getUrl());
                    if(mDetailName != null)
                        mDetailName.setText(mInfo.getDisplayname());
                    if(mDetailSize != null)
                        mDetailSize.setText(mInfo.getSize() / 1024 / 1024 + "MB");
                    if(mDetailMime != null)
                        mDetailMime.setText(MusicService.getRateInfo(Constants.MIME));
                    if(mDetailDuration != null)
                        mDetailDuration.setText(CommonUtil.getTime(mInfo.getDuration()));
                    if(mDetailBitRate != null)
                        mDetailBitRate.setText(MusicService.getRateInfo(Constants.BIT_RATE) + " kb/s");
                    if(mDetailSampleRate != null)
                        mDetailSampleRate.setText(MusicService.getRateInfo(Constants.SAMPLE_RATE) + " Hz");

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
