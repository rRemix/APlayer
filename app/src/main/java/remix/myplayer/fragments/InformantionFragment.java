package remix.myplayer.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import remix.myplayer.R;
import remix.myplayer.adapters.PlayListAdapter;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/2.
 */
public class InformantionFragment extends Fragment {
    private TextView mArtist;
    private TextView mAlbum;
    private ImageView mImage;
    private MP3Info mInfo;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.information,null,false);
        mArtist = (TextView)rootView.findViewById(R.id.information_artist);
        mAlbum = (TextView)rootView.findViewById(R.id.information_album);
        mImage = (ImageView)rootView.findViewById(R.id.information_image);
        mInfo = (MP3Info)getArguments().getSerializable("MP3Info");
        UpdateInformation(mInfo);
        return rootView;
    }
    public void UpdateInformation(MP3Info mp3Info)
    {
//        if(mArtist != null)
//            System.out.println(mArtist.getText().toString());
//        if(mAlbum != null)
//            System.out.println(mAlbum.getText().toString());
        if(mp3Info != null)
        {
            mArtist.setText(mp3Info.getArtist());
            mAlbum.setText(mp3Info.getAlbum());
        }
    }
}
