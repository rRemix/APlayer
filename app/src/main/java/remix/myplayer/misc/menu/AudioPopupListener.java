package remix.myplayer.misc.menu;

import android.content.ContextWrapper;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.PlayListSong;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.Tag;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.ui.dialog.AddtoPlayListDialog;
import remix.myplayer.ui.dialog.FileChooserDialog;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.ui.fragment.LyricFragment;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
public class AudioPopupListener<ActivityCallback extends AppCompatActivity & FileChooserDialog.FileCallback> extends ContextWrapper implements PopupMenu.OnMenuItemClickListener{
    private ActivityCallback mActivity;
    private Song mInfo;
    private Tag mTag;

    public AudioPopupListener(ActivityCallback activity, Song song){
        super(activity);
        mActivity = activity;
        mInfo = song;
        mTag = new Tag(activity,song);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_lyric:
                final boolean alreadyIgnore = SPUtil.getValue(mActivity,SPUtil.LYRIC_KEY.NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE;
                final LyricFragment lyricFragment = ((PlayerActivity)mActivity).getLyricFragment();
                new MaterialDialog.Builder(mActivity)
                        .items(getString(R.string.netease),
                                getString(R.string.kugou),
                                getString(R.string.select_lrc),
                                getString(!alreadyIgnore ? R.string.ignore_lrc : R.string.cancel_ignore_lrc)
                                /**,getString(R.string.change_offset)*/)
                        .itemsColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsCallback((dialog, itemView, position, text) -> {
                            switch (position){
                                case 0: //网易 酷狗
                                case 1:
                                    SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.NAME,mInfo.getId() + "",position == 0 ? SPUtil.LYRIC_KEY.LYRIC_NETEASE : SPUtil.LYRIC_KEY.LYRIC_KUGOU);
                                    lyricFragment.updateLrc(mInfo,true);
                                    sendBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
                                    break;
                                case 2: //手动选择歌词
                                    new FileChooserDialog.Builder(mActivity)
                                            .extensionsFilter(".lrc")
                                            .show();
                                    break;
                                case 3: //忽略或者取消忽略
                                    new MaterialDialog.Builder(mActivity)
                                            .negativeText(R.string.cancel)
                                            .negativeColorAttr(R.attr.text_color_primary)
                                            .positiveText(R.string.confirm)
                                            .positiveColorAttr(R.attr.text_color_primary)
                                            .title(!alreadyIgnore ? R.string.confirm_ignore_lrc : R.string.confirm_cancel_ignore_lrc)
                                            .titleColorAttr(R.attr.text_color_primary)
                                            .backgroundColorAttr(R.attr.background_color_3)
                                            .onPositive((dialog1, which) -> {
                                                if(!alreadyIgnore){//忽略
                                                    if (mInfo != null) {
                                                        SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_IGNORE);
                                                        lyricFragment.updateLrc(mInfo);
                                                    }
                                                } else {//取消忽略
                                                    SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_DEFAULT);
                                                    lyricFragment.updateLrc(mInfo);
                                                }
                                                sendBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
                                            })
                                            .show();
                                    break;
                                case 4://歌词时间轴调整
                                    ((PlayerActivity) mActivity).showLyricOffsetView();
                                    break;
                            }

                        })
                        .show();
                break;
            case R.id.menu_edit:
                mTag.edit();
                break;
            case R.id.menu_detail:
                mTag.detail();
                break;
            case R.id.menu_timer:
                mActivity.startActivity(new Intent(mActivity, TimerDialog.class));
                break;
            case R.id.menu_eq:
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                if(Util.isIntentAvailable(mActivity,audioEffectIntent)){
                    mActivity.startActivityForResult(audioEffectIntent, 0);
                } else {
                    mActivity.startActivity(new Intent(mActivity,EQActivity.class));
                }
                break;
            case R.id.menu_collect:
                PlayListSong info = new PlayListSong(mInfo.getId(), Global.MyLoveID,Constants.MYLOVE);
                ToastUtil.show(mActivity,
                        PlayListUtil.addSong(info) > 0 ? getString(R.string.add_song_playlist_success, 1,Constants.MYLOVE) : getString(R.string.add_song_playlist_error));
                break;
            case R.id.menu_add_to_playlist:
                Intent intentAdd = new Intent(this,AddtoPlayListDialog.class);
                Bundle ardAdd = new Bundle();
                ardAdd.putSerializable("list", new ArrayList<>(Collections.singletonList(mInfo.getId())));
                intentAdd.putExtras(ardAdd);
                startActivity(intentAdd);
                break;
            case R.id.menu_delete:
                new MaterialDialog.Builder(mActivity)
                        .content(R.string.confirm_delete_from_library)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .checkBoxPromptRes(R.string.delete_source, false, null)
                        .onAny((dialog, which) -> {
                            if(which == POSITIVE){
                                if(MediaStoreUtil.delete(mInfo.getId() , Constants.SONG,dialog.isPromptCheckBoxChecked()) > 0){
                                    if(PlayListUtil.deleteSong(mInfo.getId(), Global.PlayQueueID)){
                                        ToastUtil.show(mActivity, getString(R.string.delete_success));
                                        //移除的是正在播放的歌曲
                                        if(MusicService.getCurrentMP3() == null)
                                            return;
                                        if(mInfo.getId() == MusicService.getCurrentMP3().getId() && Global.PlayQueue.size() >= 2){
                                            Intent intent = new Intent(MusicService.ACTION_CMD);
                                            intent.putExtra("Control", Command.NEXT);
                                            mActivity.sendBroadcast(intent);
                                        }
                                    }
                                } else {
                                    ToastUtil.show(mActivity, getString(R.string.delete_error));
                                }
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
//            case R.id.menu_vol:
//                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                if(audioManager != null){
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
//                }
        }
        return true;
    }

    private class TextInputEditWatcher implements TextWatcher{
        private TextInputLayout mInputLayout;
        private String mError;
        TextInputEditWatcher(TextInputLayout layout,String error){
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
