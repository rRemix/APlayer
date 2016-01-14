package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.AlbumHolderAdapter;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/4.
 */
public class AlbumHolderFragment extends Fragment {
    private int mId;
    private ArrayList<MP3Info> mInfoList;
    private ListView mListView;
    private TextView mNum;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.artist_album_holder,null);
        mId = getArguments().getInt("Id",-1);
        if(mId > 0)
            mInfoList = Utility.getMP3InfoByArtistIdOrAlbumId(mId,0);
        mListView = (ListView)rootView.findViewById(R.id.artist_album_holder_list);
        mListView.setAdapter(new AlbumHolderAdapter(mInfoList,inflater));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MP3Info temp = (MP3Info) parent.getAdapter().getItem(position);
                Intent intent = new Intent(Utility.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Utility.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                getContext().sendBroadcast(intent);
                ArrayList<Long> ids = new ArrayList<Long>();
                for(MP3Info info : mInfoList)
                {
                    ids.add(info.getId());
                }
                MusicService.setCurrentList(ids);
            }
        });
        mNum = (TextView)rootView.findViewById(R.id.album_holder_item_num);
        mNum.setText(mInfoList.size() + "首歌曲");
        return rootView;
    }

}
