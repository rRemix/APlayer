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

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;


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

    public void setList(ArrayList<MP3Info> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mInfoList == null ? 0 : mInfoList.size();
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
        //检查是否有缓存
        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.child_holder_item,null);
            holder = new ViewHolder();
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


        //获得正在播放的歌曲
        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean flag = temp.getId() == currentMP3.getId();
            holder.mTitle.setTextColor(flag ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
            mColumnView = (ColumnView)convertView.findViewById(R.id.columnview);
            mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);

            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !mColumnView.getStatus() && flag){
                mColumnView.startAnim();
            }
            else if(!MusicService.getIsplay() && mColumnView.getStatus()){
                mColumnView.stopAnim();
            }
        }

        //设置标题
        holder.mTitle.setText(temp.getDisplayname());

        //选项dialog
        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.song_item_button);
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
//        private TextView mArtist;
    }
}