package remix.myplayer.ui.dialog;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAddtoAdapter;
import remix.myplayer.model.PlayListSongInfo;
import remix.myplayer.db.PlayLists;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的对话框
 */
public class AddtoPlayListDialog extends BaseDialogActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    @BindView(R.id.playlist_addto_list)
    RecyclerView mList;
    @BindView(R.id.playlist_addto_new)
    TextView mNew;

    private PlayListAddtoAdapter mAdapter;
    private Cursor mCursor;
    public static int mPlayListNameIndex;
    public static int mPlayListIdIndex;

    private String mSongName;
    private int mAudioID;
    private int mAlbumId;
    private String mArtist;

    private boolean mAddAfterCreate = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playlist_addto);
        ButterKnife.bind(this);

        mAudioID = (int)getIntent().getExtras().getLong("Id");
        mSongName = getIntent().getExtras().getString("SongName");
        mAlbumId = (int)getIntent().getExtras().getLong("AlbumId");
        mArtist = getIntent().getExtras().getString("Artist");
        //改变高度，并置于底部
        Window w = getWindow();
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.height = 300 * metrics.densityDpi / 160;
        lp.width = metrics.widthPixels;
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        mAdapter = new PlayListAddtoAdapter(this);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(view != null && view.getTag() instanceof Integer) {
                    PlayListSongInfo info = new PlayListSongInfo(mAudioID,getPlayListId(position),getPlayListName(position));
                    ToastUtil.show(AddtoPlayListDialog.this,
                            PlayListUtil.addSong(info) > 0 ? getString(R.string.add_song_playlist_success, 1) : getString(R.string.add_song_playlist_error),
                            Toast.LENGTH_SHORT);
                } else {
                    ToastUtil.show(AddtoPlayListDialog.this,R.string.add_song_playlist_error,Toast.LENGTH_SHORT);
                }
                finish();
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        mList.setAdapter(mAdapter);
    }

    private int getPlayListId(int position){
        int playListId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playListId = mCursor.getInt(mCursor.getColumnIndex(PlayLists.PlayListColumns._ID));
        }
        return playListId;
    }

    private String getPlayListName(int position){
        String playlistName = "";
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            playlistName = mCursor.getString(mCursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
        }
        return playlistName;
    }

    @OnClick({R.id.playlist_addto_cancel,R.id.playlist_addto_new})
    public void onClick(View v) {
        if(v.getId() == R.id.playlist_addto_cancel)
            finish();
        if(v.getId() == R.id.playlist_addto_new){
            new MaterialDialog.Builder(this)
                    .title(R.string.new_playlist)
                    .titleColor(ThemeStore.getTextColorPrimary())
                    .positiveText(R.string.create)
                    .positiveColor(ThemeStore.getMaterialColorPrimaryColor())
                    .negativeText(R.string.cancel)
                    .negativeColor(ThemeStore.getTextColorPrimary())
                    .backgroundColor(ThemeStore.getBackgroundColor3())
                    .content(R.string.input_playlist_name)
                    .contentColor(ThemeStore.getTextColorPrimary())
                    .inputRange(1,15)
                    .input("", "本地歌单" + Global.mPlayList.size(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            if(!TextUtils.isEmpty(input)){
//                                XmlUtil.addPlaylist(AddtoPlayListDialog.this,input.toString());
                                int newPlayListId = PlayListUtil.addPlayList(input.toString());
                                if(mAddAfterCreate){
                                    ToastUtil.show(AddtoPlayListDialog.this,
                                            PlayListUtil.addSong(new PlayListSongInfo(mAudioID,newPlayListId,input.toString())) > 0 ? getString(R.string.add_song_playlist_success, 1) : getString(R.string.add_song_playlist_error),
                                            Toast.LENGTH_SHORT);
                                }
                            }
                        }
                    })
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .show();

        }
    }

    public PlayListAddtoAdapter getAdaper(){
        return mAdapter;
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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        mCursor = data;
        mPlayListIdIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns._ID);
        mPlayListNameIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns.NAME);
        if(mAdapter != null)
            mAdapter.setCursor(mCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mCursor != null)
            mCursor.close();
    }
}
