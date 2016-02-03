package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/2.
 */
public class PlayingAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };
    public PlayingAdapter(LayoutInflater inflater, Context context)
    {
        mContext = context;
        mInflater = inflater;
    }

    @Override
    public int getCount() {
        if(Utility.mPlayingList == null)
            return 0;
        return Utility.mPlayingList.size();
    }

    @Override
    public Object getItem(int position)
    {
        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
            return null;
        return Utility.mPlayingList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null)
        {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playinglist_item,null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();


        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
            return convertView;

        final MP3Info temp = new MP3Info(Utility.getMP3InfoById(Utility.mPlayingList.get(position)));
        if(temp != null) {
            holder.mSong.setText(temp.getDisplayname());
            holder.mArtist.setText(temp.getArtist());
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utility.mPlayingList.remove(temp.getId());
                    if(temp.getId() == MusicService.getCurrentMP3().getId())
                    {
                        Intent intent = new Intent(Utility.CTL_ACTION);
                        intent.putExtra("Control", Utility.NEXT);
                        mContext.sendBroadcast(intent);
                    }
                    mHandler.sendEmptyMessage(0x132);
//                    notifyDataSetChanged();
                }
            });
        }
        return convertView;
//        View ItemView = mInflater.inflate(R.layout.playinglist_item,null);
//        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
//            return ItemView;
//        TextView title = (TextView)ItemView.findViewById(R.id.playlist_item_name);
//        TextView artist = (TextView)ItemView.findViewById(R.id.playlist_item_artist);
//
//        MP3Info temp = new MP3Info(Utility.getMP3InfoById(Utility.mPlayingList.get(position)));
//        title.setText(temp.getDisplayname());
//        artist.setText(temp.getArtist());

    }
    public static class ViewHolder{
        public final TextView mSong;
        public final TextView mArtist;
        public final ImageView mButton;
        public ViewHolder(View v) {
            mSong = (TextView)v.findViewById(R.id.playlist_item_name);
            mArtist = (TextView)v.findViewById(R.id.playlist_item_artist);
            mButton = (ImageView) v.findViewById(R.id.playlist_item_button);
        }

    }
}
