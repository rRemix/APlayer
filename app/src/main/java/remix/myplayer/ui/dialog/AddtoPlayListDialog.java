package remix.myplayer.ui.dialog;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
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
import remix.myplayer.adapter.AddtoPlayListAdapter;
import remix.myplayer.db.PlayLists;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.PlayListSongInfo;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
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
    RecyclerView mRecyclerView;
    @BindView(R.id.playlist_addto_new)
    TextView mNew;

    private AddtoPlayListAdapter mAdapter;
    private Cursor mCursor;
    public static int mPlayListNameIndex;
    public static int mPlayListIdIndex;

    private int mAudioID;

    private boolean mAddAfterCreate = true;
    private static int LOADER_ID = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_addto_playlist);
        ButterKnife.bind(this);

        mAudioID = (int)getIntent().getExtras().getLong("Id");

        mAdapter = new AddtoPlayListAdapter(this);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(view != null ) {
                    PlayListSongInfo info = new PlayListSongInfo(mAudioID,getPlayListId(position),getPlayListName(position));
                    ToastUtil.show(AddtoPlayListDialog.this,
                            PlayListUtil.addSong(info) > 0 ? getString(R.string.add_song_playlist_success, 1,getPlayListName(position)) : getString(R.string.add_song_playlist_error));
                } else {
                    ToastUtil.show(AddtoPlayListDialog.this,R.string.add_song_playlist_error,Toast.LENGTH_SHORT);
                }
                finish();
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(LOADER_ID++, null, this);

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

    @OnClick({R.id.playlist_addto_new})
    public void onClick(View v) {
        if(v.getId() == R.id.playlist_addto_new){
            new MaterialDialog.Builder(this)
                    .title(R.string.new_playlist)
                    .titleColorAttr(R.attr.text_color_primary)
                    .buttonRippleColor(ThemeStore.getRippleColor())
                    .positiveText(R.string.create)
                    .positiveColorAttr(R.attr.text_color_primary)
                    .negativeText(R.string.cancel)
                    .negativeColorAttr(R.attr.text_color_primary)
                    .backgroundColorAttr(R.attr.background_color_3)
                    .contentColorAttr(R.attr.text_color_primary)
                    .inputRange(1,15)
                    .input("", "本地歌单" + Global.mPlayList.size(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            if(!TextUtils.isEmpty(input)){
//                                XmlUtil.addPlaylist(AddtoPlayListDialog.this,input.toString());
                                int newPlayListId = PlayListUtil.addPlayList(input.toString());
                                ToastUtil.show(AddtoPlayListDialog.this, newPlayListId > 0 ?
                                                R.string.add_playlist_success :
                                                newPlayListId == -1 ? R.string.add_playlist_error : R.string.playlist_alread_exist,
                                        Toast.LENGTH_SHORT);
                                if(newPlayListId < 0){
                                    return;
                                }
                                if(mAddAfterCreate){
                                    ToastUtil.show(AddtoPlayListDialog.this,
                                            PlayListUtil.addSong(new PlayListSongInfo(mAudioID,newPlayListId,input.toString())) > 0 ? getString(R.string.add_song_playlist_success, 1,input.toString()) : getString(R.string.add_song_playlist_error),
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

    public AddtoPlayListAdapter getAdaper(){
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
        return new CursorLoader(this,PlayLists.CONTENT_URI,null,
                PlayLists.PlayListColumns.NAME + "!= ?",new String[]{Constants.PLAY_QUEUE},null);
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
