package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.CircleImageView;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加界面的适配器
 */
public class RecentlyAdapter extends BaseAdapter {
    private ArrayList<MP3Info> mInfoList;
    private ColumnView mColumnView;
    private Context mContext;

    public RecentlyAdapter(Context context, ArrayList<MP3Info> infolist) {
        this.mInfoList = infolist;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return mInfoList == null ? 0 : mInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mInfoList == null ? null : mInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        //检查缓存
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.allsong_item,null);
            holder = new ViewHolder();
            holder.mImage = (CircleImageView)convertView.findViewById(R.id.song_head_image);
            holder.mName = (TextView)convertView.findViewById(R.id.displayname);
            holder.mOther = (TextView)convertView.findViewById(R.id.detail);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder)convertView.getTag();

        final MP3Info temp = (MP3Info) getItem(position);
        if(temp == null)
            return convertView;
        //获得正在播放的歌曲
        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean flag = temp.getId() == currentMP3.getId();
            holder.mName.setTextColor(flag ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
            mColumnView = (ColumnView)convertView.findViewById(R.id.columnview);
            mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);

            if(MusicService.getIsplay() && !mColumnView.getStatus() && flag){
                mColumnView.startAnim();
            }
            else if(!MusicService.getIsplay() && mColumnView.getStatus()){
                mColumnView.stopAnim();
            }
        }
        //设置歌曲名
        holder.mName.setText(temp.getDisplayname());

        String artist = temp.getArtist();
        String album = temp.getAlbum();
        //设置艺术家与专辑名
        holder.mOther.setText(artist + "-" + album);
        //设置封面
        ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" + temp.getAlbumId(),
                holder.mImage);
        //选项Dialog
        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.allsong_item_button);
        mItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, OptionDialog.class);
                intent.putExtra("MP3Info",temp);
                mContext.startActivity(intent);
            }
        });
        return convertView;
    }

    public static class ViewHolder{
        public TextView mName;
        public TextView mOther;
        public CircleImageView mImage;
    }
}
