package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayQueueAdapter extends RecyclerView.Adapter<PlayQueueAdapter.PlayQueueHolder> {
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };
    public PlayQueueAdapter(Context context) {
        mContext = context;
    }

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }
    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public PlayQueueHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlayQueueHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_item,parent,false));
    }

    @Override
    public void onBindViewHolder(PlayQueueHolder holder, int position) {
        if(mCursor.moveToPosition(position)){
            final MP3Item item = MediaStoreUtil.getMP3InfoById(mCursor.getInt(0));
            if(item == null) {
                //歌曲已经失效
                holder.mSong.setText(mContext.getString(R.string.song_lose_effect));
                holder.mArtist.setVisibility(View.GONE);
                return;
            }
            //设置歌曲与艺术家
            holder.mSong.setText(CommonUtil.processInfo(item.getTitle(),CommonUtil.SONGTYPE));
            holder.mArtist.setText(CommonUtil.processInfo(item.getArtist(),CommonUtil.ARTISTTYPE));
            //删除按钮
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    XmlUtil.deleteSongFromPlayQueue(temp.getId());
                    PlayListUtil.deleteSong(item.getId(),Global.mPlayQueueId);
                    if(item.getId() == MusicService.getCurrentMP3().getId()) {
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
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }


    public static class PlayQueueHolder extends BaseViewHolder{
        @BindView(R.id.playlist_item_name)
        public TextView mSong;
        @BindView(R.id.playlist_item_artist)
        public TextView mArtist;
        @BindView(R.id.playlist_item_button)
        public ImageView mButton;
        public PlayQueueHolder(View v) {
            super(v);
        }

    }
}
