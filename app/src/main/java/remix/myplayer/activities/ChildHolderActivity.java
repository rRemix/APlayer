package remix.myplayer.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.ChildHolderAdapter;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;

/**
 * Created by Remix on 2015/12/4.
 */
public class ChildHolderActivity extends AppCompatActivity implements MusicService.Callback{
    private MusicService mService;
    private ImageView mBack;
    public static String mFLAG = "CHILD";
    private int mId;
    private ArrayList<MP3Info> mInfoList;
    private ListView mListView;
    private TextView mNum;
    private TextView mTitle;
    private BottomActionBarFragment mActionbar;
    private MusicService.PlayerReceiver mMusicReceiver;
    public static ChildHolderActivity mInstance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        //绑定控制播放的service;
        MusicService.addCallback(ChildHolderActivity.this);


        mId = getIntent().getIntExtra("Id",-1);
        int type = getIntent().getIntExtra("Type",-1);
        String Title = getIntent().getStringExtra("Title");
        if(mId >= 0)
        {
            mInfoList = new ArrayList<>();
            switch (type)
            {
                case Constants.ALBUM_HOLDER:
                    mInfoList = DBUtil.getMP3InfoByArtistIdOrAlbumId(mId, Constants.ALBUM_HOLDER);
                    break;
                case Constants.ARTIST_HOLDER:
                    mInfoList = DBUtil.getMP3InfoByArtistIdOrAlbumId(mId, Constants.ARTIST_HOLDER);
                    break;
                case Constants.FOLDER_HOLDER:
                    String bucket_display_name = DBUtil.mFolderList.get(mId);
                    mInfoList = DBUtil.getMP3ListByFolder(bucket_display_name);
                    Title = bucket_display_name;
                    break;
                case Constants.PLAYLIST_HOLDER:
                    ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(Title);
                    ArrayList<String> names = new ArrayList<>();
                    for(PlayListItem item : list)
                        names.add(item.getmSongame());
                    mInfoList = DBUtil.getMP3ListByNames(names);
                    break;
            }
        }
        if(mInfoList == null)
            return;
        setContentView(R.layout.child_holder);
        mListView = (ListView)findViewById(R.id.artist_album_holder_list);
        mListView.setAdapter(new ChildHolderAdapter(mInfoList, getLayoutInflater()));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mInfoList.size() == 0)
                    return;
                ArrayList<Long> ids = new ArrayList<Long>();
                for(MP3Info info : mInfoList)
                    ids.add(info.getId());
                DBUtil.setPlayingList((ArrayList) ids.clone());

                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                sendBroadcast(intent);
            }
        });
        //返回键
        mBack = (ImageView)findViewById(R.id.back_view);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //歌曲数目与标题
        mNum = (TextView)findViewById(R.id.album_holder_item_num);
        mNum.setText(mInfoList.size() + "首歌曲");
        mTitle = (TextView)findViewById(R.id.artist_album_title);
        mTitle.setText(Title);

        //随机播放
        RelativeLayout layout = (RelativeLayout)findViewById(R.id.child_holder_shuffle);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mInfoList.size() == 0)
                    return;
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                ArrayList<Long> ids = new ArrayList<Long>();
                for (MP3Info info : mInfoList) {
                    ids.add(info.getId());
                }
                DBUtil.setPlayingList(ids);
                sendBroadcast(intent);
            }
        });

        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(DBUtil.mPlayingList.size() == 0)
            return;
        mActionbar.UpdateBottomStatus(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(0)), false);



    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
        MP3Info temp = MP3info;
        mActionbar.UpdateBottomStatus(MP3info, isplay);
    }

    @Override
    public int getType() {
        return 3;
    }
}
