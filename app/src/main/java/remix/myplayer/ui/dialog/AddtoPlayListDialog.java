package remix.myplayer.ui.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAddtoAdapter;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Global;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的对话框
 */
public class AddtoPlayListDialog extends BaseDialogActivity {
    @BindView(R.id.playlist_addto_list)
    ListView mList;
    @BindView(R.id.playlist_addto_new)
    TextView mNew;

    private PlayListAddtoAdapter mAdapter;
    private String mSongName;
    private int mId;
    private int mAlbumId;
    private String mArtist;

    private boolean mAddAfterCreate = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Theme.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playlist_addto);
        ButterKnife.bind(this);

        mId = (int)getIntent().getExtras().getLong("Id");
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

        mAdapter = new PlayListAddtoAdapter(getApplicationContext());
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view != null) {
                    Toast.makeText(AddtoPlayListDialog.this,
                            getString(R.string.add_song_playlist_success,
                                    XmlUtil.addSongToPlayList(((TextView)view.findViewById(R.id.playlist_addto_text)).getText().toString()
                                    ,mSongName,mId,mAlbumId,mArtist,true) ? 1 : 0),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AddtoPlayListDialog.this,R.string.add_song_playlist_error, Toast.LENGTH_SHORT).show();
                }
                finish();
            }
        });
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
                    .input("", "本地歌单" + Global.mPlaylist.size(), new MaterialDialog.InputCallback() {
                        @Override
                        public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                            if(!TextUtils.isEmpty(input)){
                                XmlUtil.addPlaylist(AddtoPlayListDialog.this,input.toString());
                                if(mAddAfterCreate){
                                    Toast.makeText(AddtoPlayListDialog.this,
                                            getString(R.string.add_song_playlist_success,
                                                    XmlUtil.addSongToPlayList(input.toString(),mSongName,mId,mAlbumId,mArtist,true) ? 1 : 0),
                                            Toast.LENGTH_SHORT).show();

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
}
