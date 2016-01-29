package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import remix.myplayer.R;
import remix.myplayer.adapters.PlayListAdapter;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.DensityUtil;
import remix.myplayer.utils.Utility;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListActivity extends AppCompatActivity{
    private RecyclerView mRecycleView;
    private PlayListAdapter mAdapter;
    public static Map<String,ArrayList<String>> mPlaylist = new HashMap<>();
    private ImageButton mButton;
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
        mButton = (ImageButton)findViewById(R.id.btn_top_timer);
    }

    public void onSearch(View v)
    {
        startActivity(new Intent(this, SearchActivity.class));
    }

    public void onTimer(View v)
    {
        startActivity(new Intent(this, TimerPopupWindow.class));
    }

    public void onAdd(View v)
    {
        final View contentView = LayoutInflater.from(this).inflate(R.layout.playlist_add,null);
        final PopupWindow window = new PopupWindow(contentView,
                DensityUtil.dip2px(getApplicationContext(),254f),
                DensityUtil.dip2px(getApplicationContext(),180f),
                true);
        window.setBackgroundDrawable(getResources().getDrawable(R.drawable.playlist_add_bg));
        window.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        window.showAtLocation(v, Gravity.CENTER,0,0);

        //修改获得焦点时下划线的颜色
        EditText editText = (EditText)contentView.findViewById(R.id.playlist_add_edit);
        editText.getBackground().setColorFilter(getResources().getColor(R.color.progress_complete), PorterDuff.Mode.SRC_ATOP);
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
                if(name != null && !name.equals(""))
                    XmlUtil.addPlaylist(name);
                window.dismiss();
            }
        });

    }
}
