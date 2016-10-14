package remix.myplayer.adapter;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.XmlUtil;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayQueueAdapter extends BaseAdapter {
    private Context mContext;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };
    public PlayQueueAdapter(Context context) {
        mContext = context;
    }


    @Override
    public int getCount() {
        return Global.mPlayQueue != null ? Global.mPlayQueue.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return Global.mPlayQueue != null ? Global.mPlayQueue.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        PlayListHolder holder;
        //检查是否有缓存
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playinglist_item,null);
            holder = new PlayListHolder(convertView);
            convertView.setTag(holder);
        } else
            holder = (PlayListHolder)convertView.getTag();


        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return convertView;

        final MP3Item temp = MediaStoreUtil.getMP3InfoById(Global.mPlayQueue.get(position));
        if(temp != null) {
            //设置歌曲与艺术家
            holder.mSong.setText(CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE));
            holder.mArtist.setText(CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE));
            //删除按钮
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    XmlUtil.deleteSongFromPlayQueue(temp.getId());
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


    public static class PlayListHolder {
        @BindView(R.id.playlist_item_name)
        public TextView mSong;
        @BindView(R.id.playlist_item_artist)
        public TextView mArtist;
        @BindView(R.id.playlist_item_button)
        public ImageView mButton;
        public PlayListHolder(View v) {
            ButterKnife.bind(this,v);
        }

    }
}
