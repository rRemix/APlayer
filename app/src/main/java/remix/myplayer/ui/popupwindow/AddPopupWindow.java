package remix.myplayer.ui.popupwindow;

import android.app.Activity;
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

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.adapters.PlayListAddtoAdapter;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.utils.ErrUtil;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-2-1.
 */
public class AddPopupWindow extends BaseActivity {
    private ListView mList;
    private PlayListAddtoAdapter mAdapter;
    private String mSongName;
    private int mId;
    private int mAlbumId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_addto);

        mSongName = getIntent().getExtras().getString("SongName");
        mId = (int)getIntent().getExtras().getLong("Id");
        mAlbumId = (int)getIntent().getExtras().getLong("AlbumId");
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

        mList = (ListView)findViewById(R.id.playlist_addto_list);
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
                        for(PlayListItem item : PlayListActivity.mPlaylist.get(playlist)){
                            if(item.getId() == mId){
                                isExist = true;
                            }
                        }
                        if(isExist){
                            Toast.makeText(AddPopupWindow.this,"该歌曲已经存在", Toast.LENGTH_SHORT).show();
                        } else {
                            XmlUtil.addSong(playlist, mSongName,mId,mAlbumId);
                            Toast.makeText(AddPopupWindow.this,"添加成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                        Toast.makeText(AddPopupWindow.this,"添加失败", Toast.LENGTH_SHORT).show();
                    finish();
                } catch (Exception e){
                    e.printStackTrace();
                    ErrUtil.writeError("AddPopupWindow" + "---onItemClick---" + e.toString());
                }

            }
        });
    }

    public void onCancel(View v) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
