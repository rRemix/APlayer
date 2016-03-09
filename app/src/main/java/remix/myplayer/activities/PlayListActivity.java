package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import remix.myplayer.R;
import remix.myplayer.adapters.PlayListAdapter;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DensityUtil;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends AppCompatActivity  implements MusicService.Callback{
    public static PlayListActivity mInstance = null;
    private RecyclerView mRecycleView;
    private PlayListAdapter mAdapter;
    public static Map<String,ArrayList<PlayListItem>> mPlaylist = new HashMap<>();
    private Toolbar mToolBar;
    private ImageButton mDrawerExit;
    static {
        mPlaylist = XmlUtil.getPlayList("playlist.xml");
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        MusicService.addCallback(PlayListActivity.this);
        mInstance = this;
        mRecycleView = (RecyclerView)findViewById(R.id.playlist_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(this, 2));
        mAdapter = new PlayListAdapter(getApplicationContext());
        mAdapter.setOnItemClickLitener(new PlayListAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = null;
                Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
                for (int i = 0; i <= position; i++) {
                    it.hasNext();
                    name = it.next().toString();
                }
                if(mPlaylist.get(name).size() == 0) {
                    Toast.makeText(PlayListActivity.this, "该列表为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(PlayListActivity.this, ChildHolderActivity.class);
                intent.putExtra("Id", position);
                intent.putExtra("Title", name);
                intent.putExtra("Type", Constants.PLAYLIST_HOLDER);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecycleView.setAdapter(mAdapter);


        //初始化tooblar
        initToolbar();
    }

    private void initToolbar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("播放列表");
        mToolBar.setTitleTextColor(Color.parseColor("#ffffffff"));
        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.common_btn_back);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(PlayListActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(PlayListActivity.this, TimerPopupWindow.class));
                        break;
                }
                return true;
            }
        });
    }

    public void onAdd(View v)
    {
        final View contentView = LayoutInflater.from(this).inflate(R.layout.playlist_add,null);
        final PopupWindow window = new PopupWindow(contentView,
                DensityUtil.dip2px(getApplicationContext(),254f),
                DensityUtil.dip2px(getApplicationContext(),180f),
                true);
        window.setBackgroundDrawable(getResources().getDrawable(R.drawable.createlist_bg));
        window.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        window.setAnimationStyle(R.style.popwin_anim_style);
        window.showAtLocation(v, Gravity.CENTER,0,0);

        //修改获得焦点时下划线的颜色
        EditText editText = (EditText)contentView.findViewById(R.id.playlist_add_edit);
        editText.getBackground().setColorFilter(getResources().getColor(R.color.cursor_color), PorterDuff.Mode.SRC_ATOP);
        editText.setText("本地歌单" + mPlaylist.size());
        contentView.findViewById(R.id.playlist_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                window.dismiss();
            }
        });

        contentView.findViewById(R.id.playlist_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = ((EditText)contentView.findViewById(R.id.playlist_add_edit)).getText().toString();
                if(name != null && !name.equals("")) {
                    XmlUtil.addPlaylist(name);
                    mAdapter.notifyDataSetChanged();
                }
                window.dismiss();
            }
        });
    }

    public PlayListAdapter getAdapter()
    {
        return mAdapter;
    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {

    }

    @Override
    public int getType() {
        return Constants.PLAYLISTACTIVITY;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }
}
