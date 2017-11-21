package remix.myplayer.adapter;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.model.mp3.PlayListSong;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 正在播放列表的适配器
 */
public class PlayQueueAdapter extends BaseAdapter<PlayListSong,PlayQueueAdapter.PlayQueueHolder> {
    public PlayQueueAdapter(Context context,int layoutId) {
        super(context,layoutId);
    }

    @Override
    protected void convert(final PlayQueueHolder holder, PlayListSong playListSong, int position) {
        final int audioId = playListSong.AudioId;
        final Song item = MediaStoreUtil.getMP3InfoById(audioId);
        if(item == null) {
            //歌曲已经失效
            holder.mSong.setText(mContext.getString(R.string.song_lose_effect));
            holder.mArtist.setVisibility(View.GONE);
        } else {
            //设置歌曲与艺术家
            holder.mSong.setText(CommonUtil.processInfo(item.getTitle(),CommonUtil.SONGTYPE));
            holder.mArtist.setText(CommonUtil.processInfo(item.getArtist(),CommonUtil.ARTISTTYPE));
            holder.mArtist.setVisibility(View.VISIBLE);
//                //高亮
            if(MusicService.getCurrentMP3() != null && MusicService.getCurrentMP3().getId() == item.getId()){
                holder.mSong.setTextColor(ThemeStore.getAccentColor());
            } else {
//                holder.mSong.setTextColor(Color.parseColor(ThemeStore.isDay() ? "#323335" : "#ffffff"));
                holder.mSong.setTextColor(ThemeStore.getTextColorPrimary());
            }
        }

        //删除按钮
        holder.mDelete.setOnClickListener(v -> {
            if(PlayListUtil.deleteSong(audioId,Global.PlayQueueID)){
//                if(MusicService.getCurrentMP3() != null && MusicService.getCurrentMP3().getId() == audioId){
//                    mContext.sendBroadcast(new Intent(Constants.CTL_ACTION).putExtra("Control", Constants.NEXT));
//                }
            }
            //更新界面
           new Handler().sendEmptyMessage(Constants.NOTIFYDATACHANGED);
        });
        if(mOnItemClickLitener != null){
            holder.mContainer.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition()));
        }
    }

    static class PlayQueueHolder extends BaseViewHolder{
        @BindView(R.id.playlist_item_name)
        TextView mSong;
        @BindView(R.id.playlist_item_artist)
        TextView mArtist;
        @BindView(R.id.playqueue_delete)
        ImageView mDelete;
        @BindView(R.id.item_root)
        RelativeLayout mContainer;
        public PlayQueueHolder(View v) {
            super(v);
        }
    }
}
