package remix.myplayer.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.util.ByteConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.tageditor.TagEditor;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.helper.MusicServiceRemote.getCurrentSong;
import static remix.myplayer.helper.MusicServiceRemote.setCurrentSong;


public class Tag extends ContextWrapper {
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
    @BindView(R.id.track_layout)
    @Nullable
    TextInputLayout mTrackLayout;

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

    private final Song mInfo;
    private final TagEditor mTagEditor;

    public Tag(Context context, Song song) {
        super(context);
        mInfo = song != null ? song : getCurrentSong();
        mTagEditor = new TagEditor(mInfo.getUrl());
    }

    public void detail() {
        MaterialDialog detailDialog = Theme.getBaseDialog(this)
                .title(R.string.song_detail)
                .customView(R.layout.dialog_song_detail, true)
                .positiveText(R.string.confirm)
                .build();
        detailDialog.show();
        mDetailRootView = detailDialog.getCustomView();
        if (mDetailRootView != null) {
            ButterKnife.bind(this, mDetailRootView);

            //歌曲路径
            if (mDetailPath != null)
                mDetailPath.setText(mInfo.getUrl());
            //歌曲名称
            if (mDetailName != null)
                mDetailName.setText(mInfo.getDisplayname());
            //歌曲大小
            if (mDetailSize != null)
                mDetailSize.setText(getString(R.string.cache_size, 1.0f * mInfo.getSize() / ByteConstants.MB));
            //歌曲格式
            if (mDetailMime != null) {
                mDetailMime.setText(mTagEditor.getFormat());
            }
            //歌曲时长
            if (mDetailDuration != null)
                mDetailDuration.setText(Util.getTime(mInfo.getDuration()));
            //歌曲码率
            if (mDetailBitRate != null)
                mDetailBitRate.setText(String.format("%s kb/s", mTagEditor.getBitrate()));
            //歌曲采样率
            if (mDetailSampleRate != null)
                mDetailSampleRate.setText(String.format("%s Hz", mTagEditor.getSamplingRate()));
        }
    }

    public void edit() {
        MaterialDialog editDialog = Theme.getBaseDialog(this)
                .title(R.string.song_edit)
                .customView(R.layout.dialog_song_edit, true)
                .negativeText(R.string.cancel)
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {
                    String title, artist, album, genre, year, track;
                    title = mSongLayout != null ? mSongLayout.getEditText().getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) {
                        ToastUtil.show(this, R.string.song_not_empty);
                        return;
                    }
                    artist = mArtistLayout.getEditText() != null ? mArtistLayout.getEditText().getText().toString().trim() : "";
                    album = mAlbumLayout.getEditText() != null ? mAlbumLayout.getEditText().getText().toString().trim() : "";
                    year = mYearLayout.getEditText() != null ? mYearLayout.getEditText().getText().toString().trim() : " ";
                    genre = mGenreLayout.getEditText() != null ? mGenreLayout.getEditText().getText().toString().trim() : "";
                    track = mTrackLayout.getEditText() != null ? mTrackLayout.getEditText().getText().toString().trim() : "";

                    mTagEditor.save(mInfo, title, album, artist, year, genre, track, "")
                            .compose(RxUtil.applyScheduler())
                            .subscribe(song -> {
                                Util.sendLocalBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
                                Util.sendLocalBroadcast(new Intent(Constants.TAG_EDIT)
                                        .putExtra("newSong", song));
                                setCurrentSong(song);
                                ToastUtil.show(this, R.string.save_success);
                            }, throwable -> ToastUtil.show(this, R.string.tag_save_error, throwable.toString()));
                }).build();
        editDialog.show();

        View editDialogCustomView = editDialog.getCustomView();
        if (editDialogCustomView != null) {
            ButterKnife.bind(this, editDialogCustomView);

            if (!ThemeStore.isDay()) {
                mSongLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                mSongLayout.getEditText().getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            }
            mSongLayout.getEditText().addTextChangedListener(new TextInputEditWatcher(mSongLayout, getString(R.string.song_not_empty)));
            mSongLayout.getEditText().setText(mInfo.getTitle());

            if (!ThemeStore.isDay()) {
                mAlbumLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
            }
            mAlbumLayout.getEditText().setText(mInfo.getAlbum());

            if (!ThemeStore.isDay()) {
                mArtistLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
            }
            mArtistLayout.getEditText().setText(mInfo.getArtist());

            if (!ThemeStore.isDay()) {
                mYearLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
            }
            mYearLayout.getEditText().setText(mInfo.getYear());

            if (!ThemeStore.isDay()) {
                mGenreLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
            }
            mGenreLayout.getEditText().setText(mTagEditor.getGenreName());

            if (!ThemeStore.isDay()) {
                mTrackLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
            }
            mTrackLayout.getEditText().setText(mTagEditor.getTrackNumber());
        }
    }

    private class TextInputEditWatcher implements TextWatcher {
        private TextInputLayout mInputLayout;
        private String mError;

        TextInputEditWatcher(TextInputLayout layout, String error) {
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
            if (s == null || TextUtils.isEmpty(s.toString())) {
                mInputLayout.setError(mError);
            } else {
                mInputLayout.setError("");
            }
        }
    }
}
