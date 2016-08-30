package remix.myplayer.listener;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;

/**
 * @ClassName AudioPopupListener
 * @Description 播放界面窗口
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
public class AudioPopupListener implements PopupMenu.OnMenuItemClickListener{
    private Context mContext;
    private MP3Item mInfo;
    View mRootView;
    @BindView(R.id.song_layout)
    TextInputLayout mSongLayout;
    @BindView(R.id.song_edt)
    TextInputEditText mSongEdt;
    @BindView(R.id.album_layout)
    TextInputLayout mAlbumLayout;
    @BindView(R.id.album_edt)
    TextInputEditText mAlbumEdt;
    @BindView(R.id.artist_layout)
    TextInputLayout mArtistLayout;
    @BindView(R.id.artist_edt)
    TextInputEditText mArtistEdt;
    @BindView(R.id.year_layout)
    TextInputLayout mYearLayout;
    @BindView(R.id.year_edt)
    TextInputEditText mYearEdt;
    @BindView(R.id.genre_layout)
    TextInputLayout mGenreLayout;
    @BindView(R.id.genre_edt)
    TextInputEditText mGenreEdt;


    public AudioPopupListener(Context context,MP3Item info){
        mContext = context;
        mInfo = info;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_edit:
                break;
            case R.id.menu_detail:

                MediaFormat mf = MusicService.getMediaFormat();
                int bitRate = mf.getInteger(MediaFormat.KEY_BIT_RATE);
                int sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                int channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                String mime = mf.getString(MediaFormat.KEY_MIME);
                MaterialDialog dialog = new MaterialDialog.Builder(mContext)
                        .title("歌曲详情")
                        .customView(R.layout.dialog_song_detail,true)
                        .positiveText("确定")
                        .positiveColorRes(R.color.black)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                //歌曲名
                                ContentValues cv = new ContentValues();
                                ContentValues genreCv = new ContentValues();
                                String song = "",artist = "",album = "",genre = "",year = "";
                                song = mSongLayout.getEditText() != null ? mSongLayout.getEditText().getText().toString() : mInfo.getTitle();
                                if(TextUtils.isEmpty(song)){
                                    Toast.makeText(mContext,"歌曲名不能为空",Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                artist = mArtistLayout.getEditText() != null ? mArtistLayout.getEditText().getText().toString() : "未知歌手";
                                album = mAlbumLayout.getEditText() != null ? mAlbumLayout.getEditText().getText().toString() : "未知歌曲";
                                year = mYearLayout.getEditText() != null ? mYearLayout.getEditText().getText().toString() : "";
                                genre = mGenreLayout.getEditText() != null ? mGenreLayout.getEditText().getText().toString() : "";
                                cv.put(MediaStore.Audio.Media.TITLE,song);
                                cv.put(MediaStore.Audio.Media.ARTIST,artist);
                                cv.put(MediaStore.Audio.Media.ALBUM,album);
                                cv.put(MediaStore.Audio.Media.YEAR,year);
                                genreCv.put(MediaStore.Audio.Genres.NAME,genre);
                                int updateRow = -1;
                                int updateGenre = -1;
                                try {
                                    updateRow = mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,cv,
                                            MediaStore.Audio.Media._ID + "=" + mInfo.getId(),null);
                                    updateGenre = mContext.getContentResolver().update(
                                            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                                            genreCv,
                                            MediaStore.Audio.Genres.Members.AUDIO_ID + "=" + mInfo.getAlbumId(),null);
                                }catch (Exception e){
                                    e.toString();
                                }


                                dialog.dismiss();
                            }
                        }).build();
                dialog.show();

                mRootView = dialog.getCustomView();
                ButterKnife.bind(AudioPopupListener.this,mRootView);
                mSongEdt.addTextChangedListener(new TextInputEditWatcher(mSongLayout,"歌曲名不能为空"));
                mSongEdt.setText(mInfo.getTitle());
                mAlbumEdt.setText(mInfo.getAlbum());
                mArtistEdt.setText(mInfo.getArtist());

//                mAlbumEdt.addTextChangedListener(new TextInputEditWatcher(mAlbumLayout));
//                mArtistEdt.addTextChangedListener(new TextInputEditWatcher(mArtistLayout));
//                mYearEdt.addTextChangedListener(new TextInputEditWatcher(mYearLayout));
//                mGenreEdt.addTextChangedListener(new TextInputEditWatcher(mGenreLayout));
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
