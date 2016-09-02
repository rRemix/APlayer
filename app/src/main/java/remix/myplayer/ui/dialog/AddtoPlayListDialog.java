package remix.myplayer.ui.dialog;

import android.os.Bundle;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAddtoAdapter;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.ui.activity.BaseActivity;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.Global;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的对话框
 */
public class AddtoPlayListDialog extends BaseActivity {
    @BindView(R.id.playlist_addto_list)
    ListView mList;

    private PlayListAddtoAdapter mAdapter;
    private String mSongName;
    private int mId;
    private int mAlbumId;
    private String mArtist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        lp.height = (int) (300 * metrics.densityDpi / 160);
        lp.width = (int) (metrics.widthPixels);
        w.setAttributes(lp);
        w.setGravity(Gravity.BOTTOM);

        mAdapter = new PlayListAddtoAdapter(getApplicationContext());
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    TextView textView = (TextView)view.findViewById(R.id.playlist_addto_text);
                    String playlist = textView.getText().toString();
                    boolean isExist = false;
                    if(playlist != null && mSongName != null && mId > 0) {
                        for(PlayListItem item : Global.mPlaylist.get(playlist)){
                            if(item.getId() == mId){
                                isExist = true;
                            }
                        }
                        if(isExist){
                            Toast.makeText(AddtoPlayListDialog.this,getString(R.string.song_already_exist), Toast.LENGTH_SHORT).show();
                        } else {
                            XmlUtil.addSongToPlayList(playlist, mSongName,mId,mAlbumId,mArtist);
                            Toast.makeText(AddtoPlayListDialog.this,getString(R.string.add_success), Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                        Toast.makeText(AddtoPlayListDialog.this,getString(R.string.add_error), Toast.LENGTH_SHORT).show();
                    finish();
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }

    public void onCancel(View v) {
        finish();
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
