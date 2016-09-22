//package remix.myplayer.adapter;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import remix.myplayer.R;
//import remix.myplayer.model.MP3Item;
//import remix.myplayer.service.MusicService;
//import remix.myplayer.theme.ThemeStore;
//import remix.myplayer.ui.activity.RecetenlyActivity;
//import remix.myplayer.ui.customview.CircleImageView;
//import remix.myplayer.ui.customview.ColumnView;
//import remix.myplayer.ui.dialog.OptionDialog;
//import remix.myplayer.util.ColorUtil;
//import remix.myplayer.util.CommonUtil;
//
///**
// * Created by taeja on 16-3-4.
// */
//
///**
// * 最近添加界面的适配器
// */
//public class RecentlyAdapter extends BaseAdapter {
//    private ArrayList<MP3Item> mInfoList;
//    private ColumnView mColumnView;
//    private Context mContext;
//
//    public RecentlyAdapter(Context context, ArrayList<MP3Item> infolist) {
//        this.mInfoList = infolist;
//        this.mContext = context;
//    }
//
//    @Override
//    public int getCount() {
//        return mInfoList == null ? 0 : mInfoList.size();
//    }
//
//    @Override
//    public Object getItem(int position) {
//        return mInfoList == null ? null : mInfoList.get(position);
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return position;
//    }
//
//    @Override
//    public View getView(final int position, View convertView, ViewGroup parent) {
//        RecentlyHolder holder;
//        //检查缓存
//        if(convertView == null) {
//            convertView = LayoutInflater.from(mContext).inflate(R.layout.song_recycle_item,null);
//            holder = new RecentlyHolder(convertView);
//            convertView.setTag(holder);
//        } else
//            holder = (RecentlyHolder)convertView.getTag();
//
//        final MP3Item temp = (MP3Item) getItem(position);
//        if(temp == null)
//            return convertView;
//        //获得正在播放的歌曲
//        final MP3Item currentMP3 = MusicService.getCurrentMP3();
//        //判断该歌曲是否是正在播放的歌曲
//        //如果是,高亮该歌曲，并显示动画
//        if(currentMP3 != null){
//            boolean flag = temp.getId() == currentMP3.getId();
//            holder.mName.setTextColor(flag ?
//                    ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.MATERIAL_COLOR_PRIMARY : R.color.purple_782899) :
//                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
//            mColumnView = (ColumnView)convertView.findViewById(R.id.song_columnview);
//            mColumnView.setColumnColor(ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.MATERIAL_COLOR_PRIMARY : R.color.purple_782899));
//            mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);
//
//            if(MusicService.getIsplay() && !mColumnView.getStatus() && flag){
//                mColumnView.startAnim();
//            }
//            else if(!MusicService.getIsplay() && mColumnView.getStatus()){
//                mColumnView.stopAnim();
//            }
//        }
//
//        try {
//            //设置歌曲名
//            holder.mName.setText( temp.getTitle() != null ? temp.getTitle() : mContext.getString(R.string.unknow_song));
//            String artist = CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE);
//            String album = CommonUtil.processInfo(temp.getAlbum(),CommonUtil.ALBUMTYPE);
//            //设置艺术家与专辑名
//            holder.mOther.setText(artist + "-" + album);
//
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//
//        //设置封面
////        ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" + temp.getAlbumId(),
////                holder.mImage);
//        //选项Dialog
//        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.song_button);
//        mItemButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(RecetenlyActivity.MultiChoice.isShow()){
//                    return;
//                }
//                Intent intent = new Intent(mContext, OptionDialog.class);
//                intent.putExtra("MP3Item",temp);
//                mContext.startActivity(intent);
//            }
//        });
//        return convertView;
//    }
//
//    public static class RecentlyHolder {
//        @BindView(R.id.song_title)
//        public TextView mName;
//        @BindView(R.id.song_other)
//        public TextView mOther;
//        @BindView(R.id.song_head_image)
//        public CircleImageView mImage;
//        public RecentlyHolder(View itemView){
//            ButterKnife.bind(this,itemView);
//        }
//    }
//}
