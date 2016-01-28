package remix.myplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import remix.myplayer.R;
import remix.myplayer.adapters.PlayListAdapter;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.Utility;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends AppCompatActivity{
    private RecyclerView mRecycleView;
    private ImageButton mSearch;
    private ImageButton mTimer;
    private PlayListAdapter mAdapter;
    public static Map<String,ArrayList<String>> mPlaylist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlist);
        mPlaylist = XmlUtil.getPlayList();

        mRecycleView = (RecyclerView)findViewById(R.id.playlist_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(this, 2));
        mAdapter = new PlayListAdapter(getApplicationContext());
        mAdapter.setOnItemClickLitener(new PlayListAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = null;
                Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
                for(int i = 0 ; i <= position ; i++)
                {
                    it.hasNext();
                    name = it.next().toString();
                }
                Intent intent = new Intent(PlayListActivity.this, ChildHolderActivity.class);
                intent.putExtra("Id", position);
                intent.putExtra("Title", name);
                intent.putExtra("Type", Utility.PLAYLIST_HOLDER);
                startActivity(intent);

            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecycleView.setAdapter(mAdapter);

    }

    public void onSearch(View v)
    {
        startActivity(new Intent(this, SearchActivity.class));
    }

    public void onTimer(View v)
    {
        startActivity(new Intent(this, TimerPopupWindow.class));
    }
}
