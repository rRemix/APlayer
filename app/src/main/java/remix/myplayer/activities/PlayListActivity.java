package remix.myplayer.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import remix.myplayer.R;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends AppCompatActivity{
    private RecyclerView mRecycleView;
    String[] tests = new String[]{"111","222","333","444","555"};
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlist);
        mRecycleView = (RecyclerView)findViewById(R.id.playlist_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(this, 3));
//        mRecycleView.setAdapter();
    }
}
