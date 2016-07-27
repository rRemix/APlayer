package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PlayListAdapter;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.dialog.AddPlayListDialog;
import remix.myplayer.util.Constants;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends ToolbarActivity implements MusicService.Callback{
    private static final String TAG = "PlayListActivity";
    public static PlayListActivity mInstance = null;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.playlist_recycleview)
    RecyclerView mRecycleView;

    private PlayListAdapter mAdapter;
    public static Map<String,ArrayList<PlayListItem>> mPlaylist = new HashMap<>();

    static {
        mPlaylist = XmlUtil.getPlayList("playlist.xml");
    }

    public static Map<String,ArrayList<PlayListItem>> getPlayList(){
        return mPlaylist;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        ButterKnife.bind(this);
        MusicService.addCallback(PlayListActivity.this);

        mInstance = this;
        mRecycleView.setLayoutManager(new GridLayoutManager(this, 2));
        mAdapter = new PlayListAdapter(getApplicationContext());
        mAdapter.setOnItemClickLitener(new PlayListAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = "";
                Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
                for (int i = 0; i <= position; i++) {
                    it.hasNext();
                    name = it.next().toString();
                }
                if(mPlaylist.get(name).size() == 0) {
                    Toast.makeText(PlayListActivity.this, getString(R.string.list_isempty), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(PlayListActivity.this, ChildHolderActivity.class);
                intent.putExtra("Test",true);
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
        initToolbar(mToolBar,getString(R.string.playlist));
    }


    //打开添加播放列表的Dialog
    public void onAdd(View v) {
        startActivity(new Intent(PlayListActivity.this, AddPlayListDialog.class));
    }

    public PlayListAdapter getAdapter()
    {
        return mAdapter;
    }

    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {

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

    public void UpdateAdapter() {
        if(mRecycleView.getAdapter() != null){
            mRecycleView.getAdapter().notifyDataSetChanged();
        }
    }



    @Override
    public void onStop() {
        super.onStop();
    }
}
