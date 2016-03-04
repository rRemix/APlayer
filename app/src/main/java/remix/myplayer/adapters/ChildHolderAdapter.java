package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.ColumnView;
import remix.myplayer.ui.SelectedPopupWindow;

/**
 * Created by Remix on 2015/12/4.
 */
public class ChildHolderAdapter extends BaseAdapter implements ImpAdapter{
    private ArrayList<MP3Info> mInfoList;
    private LayoutInflater mInflater;
    private ColumnView mColumnView;
    private Context mContext;

    public ChildHolderAdapter(ArrayList<MP3Info> mInfoList, LayoutInflater mInflater,Context context) {
        this.mInfoList = mInfoList;
        this.mInflater = mInflater;
        this.mContext = context;
    }
    @Override
    public int getCount() {
        if(mInfoList == null)
            return 0;
        return mInfoList.size();
    }
    @Override
    public Object getItem(int position) {
        if(mInfoList == null || mInfoList.size() == 0)
            return null;
        return mInfoList.get(position);
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
            convertView = mInflater.inflate(R.layout.child_holder_item,null);
            holder = new ViewHolder();
            holder.mArtist = (TextView)convertView.findViewById(R.id.album_holder_item_artist);
            holder.mTitle = (TextView)convertView.findViewById(R.id.album_holder_item_title);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();
        if(mInfoList == null || mInfoList.size() == 0 )
            return convertView;
        final MP3Info temp = mInfoList.get(position);
        if(temp == null)
            return convertView;

        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        if(currentMP3 != null){
            boolean flag = temp.getId() == currentMP3.getId();
            holder.mTitle.setTextColor(flag ? Color.parseColor("#ff0030") : Color.parseColor("#1c1b19"));
            mColumnView = (ColumnView)convertView.findViewById(R.id.columnview);
            mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);

            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !mColumnView.getStatus()){
                mColumnView.startAnim();
            }
            else if(!MusicService.getIsplay() && mColumnView.getStatus()){
                mColumnView.stopAnim();
            }
        }

        holder.mTitle.setText(temp.getDisplayname());
        holder.mArtist.setText(temp.getArtist());

        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.song_item_button);
        mItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SelectedPopupWindow.class);
                intent.putExtra("Position",position);
                mContext.startActivity(intent);
            }
        });
        return convertView;
    }

    @Override
    public void UpdateColumnView(boolean isplay) {
        if(mColumnView != null){
            if(isplay)
                mColumnView.startAnim();
            else
                mColumnView.stopAnim();
        }
    }

    class ViewHolder {
        private TextView mTitle;
        private TextView mArtist;
    }
}