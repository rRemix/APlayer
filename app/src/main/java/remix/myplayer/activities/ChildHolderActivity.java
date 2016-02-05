package remix.myplayer.activities;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import remix.myplayer.adapters.AlbumHolderAdapter;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.PlayListItem;
import remix.myplayer.utils.Utility;

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
    private ServiceConnection mConnecting = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.PlayerBinder)service).getService();
            mService.addCallback(ChildHolderActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //绑定控制播放的service
//        Intent intent = new Intent(ChildHolderActivity.this,MusicService.class);
//        bindService(intent, mConnecting, Context.BIND_AUTO_CREATE);
        MusicService.addCallback(ChildHolderActivity.this);

        //注册Musicreceiver
//        MusicService service = new MusicService(getApplicationContext());
//        mMusicReceiver = service.new PlayerReceiver();
//        IntentFilter musicfilter = new IntentFilter(Utility.CTL_ACTION);
//        registerReceiver(mMusicReceiver, musicfilter);

        mId = getIntent().getIntExtra("Id",-1);
        int type = getIntent().getIntExtra("Type",-1);
        String Title = getIntent().getStringExtra("Title");
        if(mId >= 0)
        {
            mInfoList = new ArrayList<>();
            switch (type)
            {
                case Utility.ALBUM_HOLDER:
                    mInfoList = Utility.getMP3InfoByArtistIdOrAlbumId(mId, Utility.ALBUM_HOLDER);
                    break;
                case Utility.ARTIST_HOLDER:
                    mInfoList = Utility.getMP3InfoByArtistIdOrAlbumId(mId, Utility.ARTIST_HOLDER);
                    break;
                case Utility.FOLDER_HOLDER:
                    String bucket_display_name = Utility.mFolderList.get(mId);
                    mInfoList = Utility.getMP3ListByFolder(bucket_display_name);
                    Title = bucket_display_name;
                    break;
                case Utility.PLAYLIST_HOLDER:
                    ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(Title);
                    ArrayList<String> names = new ArrayList<>();
                    for(PlayListItem item : list)
                        names.add(item.getmSongame());
                    mInfoList = Utility.getMP3ListByNames(names);
                    break;
            }
        }
        if(mInfoList == null)
            return;
        setContentView(R.layout.artist_album_holder);
        mListView = (ListView)findViewById(R.id.artist_album_holder_list);
        mListView.setAdapter(new AlbumHolderAdapter(mInfoList, getLayoutInflater()));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                MP3Info temp = (MP3Info) parent.getAdapter().getItem(position);
                if(mInfoList.size() == 0)
                    return;
                ArrayList<Long> ids = new ArrayList<Long>();
                for(MP3Info info : mInfoList)
                    ids.add(info.getId());
                Utility.mPlayingList = (ArrayList) ids.clone();
//                mService.UpdateNextSong(position);

                Intent intent = new Intent(Utility.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Utility.PLAYSELECTEDSONG);
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
                MusicService.setPlayModel(Utility.PLAY_SHUFFLE);
                Intent intent = new Intent(Utility.CTL_ACTION);
                intent.putExtra("Control", Utility.NEXT);
                ArrayList<Long> ids = new ArrayList<Long>();
                for (MP3Info info : mInfoList) {
                    ids.add(info.getId());
                }
                Utility.setPlayingList(ids);
                sendBroadcast(intent);
            }
        });

        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Utility.mPlayingList.size() == 0)
            return;
        mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayingList.get(0)), false);



    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mConnecting);
//        unregisterReceiver(mMusicReceiver);
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
