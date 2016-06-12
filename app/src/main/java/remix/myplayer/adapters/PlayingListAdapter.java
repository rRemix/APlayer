package remix.myplayer.adapters;

import android.app.VoiceInteractor;
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
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.Global;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayingListAdapter extends BaseAdapter {
    private Context mContext;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };
    public PlayingListAdapter(Context context) {
        mContext = context;
    }


    @Override
    public int getCount() {
        return Global.mPlayingList != null ? Global.mPlayingList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return Global.mPlayingList != null ? Global.mPlayingList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        //检查是否有缓存
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playinglist_item,null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder)convertView.getTag();


        if(Global.mPlayingList == null || Global.mPlayingList.size() == 0)
            return convertView;

        final MP3Info temp = DBUtil.getMP3InfoById(Global.mPlayingList.get(position));
        if(temp != null) {
            //设置歌曲与艺术家
            holder.mSong.setText(CommonUtil.processInfo(temp.getDisplayname(),CommonUtil.SONGTYPE));
            holder.mArtist.setText(CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE));
            //删除按钮
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XmlUtil.deleteSongFromPlayingList(temp.getId());
                    if(temp.getId() == MusicService.getCurrentMP3().getId()) {
                        Intent intent = new Intent(Constants.CTL_ACTION);
                        intent.putExtra("Control", Constants.NEXT);
                        mContext.sendBroadcast(intent);
                    }
                    //更新界面
                    mHandler.sendEmptyMessage(Constants.NOTIFYDATACHANGED);
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
