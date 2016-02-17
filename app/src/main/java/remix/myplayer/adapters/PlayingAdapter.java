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
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;

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
        if(DBUtil.mPlayingList == null)
            return 0;
        return DBUtil.mPlayingList.size();
    }

    @Override
    public Object getItem(int position)
    {
        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return null;
        return DBUtil.mPlayingList.get(position);
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


        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return convertView;

        final MP3Info temp = new MP3Info(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(position)));
        if(temp != null) {
            holder.mSong.setText(temp.getDisplayname());
            holder.mArtist.setText(temp.getArtist());
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DBUtil.mPlayingList.remove(temp.getId());
                    if(temp.getId() == MusicService.getCurrentMP3().getId())
                    {
                        Intent intent = new Intent(Constants.CTL_ACTION);
                        intent.putExtra("Control", Constants.NEXT);
                        mContext.sendBroadcast(intent);
                    }
                    mHandler.sendEmptyMessage(0x132);
//                    notifyDataSetChanged();
                }
            });
        }
        return convertView;

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
