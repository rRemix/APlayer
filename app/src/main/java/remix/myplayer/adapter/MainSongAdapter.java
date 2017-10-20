package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.MediaStore;
import android.view.View;

import java.util.List;

import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.model.mp3.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.RecetenlyActivity;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2017/10/20.
 */

public class MainSongAdapter extends SongAdapter {
    private List<MP3Item> mList;
    public MainSongAdapter(Context context, Cursor cursor, MultiChoice multiChoice, int type) {
        super(context, cursor, multiChoice, type);
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public void setData(List<MP3Item> mp3Items){
        mList = mp3Items;
        notifyDataSetChanged();
    }

    public List<MP3Item> getData(){
        return mList;
    }

    @Override
    public void onBind(BaseViewHolder baseHolder, int position) {
        if(position == 0){
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mList == null || mList.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }

            if(mType == RECENTLY){
                //最近添加 隐藏排序方式
                headerHolder.mSortContainer.setVisibility(View.GONE);
            }
            //显示当前排序方式
            headerHolder.mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? R.string.sort_as_add_time : R.string.sort_as_letter);
            headerHolder.mAscDesc.setText(!ASCDESC.equals(" asc") ? R.string.sort_as_desc : R.string.sort_as_asc);
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OnClick(headerHolder,v);
                }
            };
            headerHolder.mSort.setOnClickListener(listener);
            headerHolder.mAscDesc.setOnClickListener(listener);
            headerHolder.mShuffle.setOnClickListener(listener);
            return;
        }
        if(!(baseHolder instanceof SongViewHolder))
            return;
        final SongViewHolder holder = (SongViewHolder) baseHolder;
        final MP3Item temp = mList.get(position - 1);

        try {
            //是否为无损
            String prefix = temp.getDisplayname().substring(temp.getDisplayname().lastIndexOf(".") + 1);
            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);

            //设置歌曲名
            String name = CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE);
            holder.mName.setText(name);

            //艺术家与专辑
            String artist = CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE);
            String album = CommonUtil.processInfo(temp.getAlbum(),CommonUtil.ALBUMTYPE);
            holder.mOther.setText(artist + "-" + album);

            //封面
            new AsynLoadImage(holder.mImage).execute(temp.getAlbumId(),Constants.URL_ALBUM);

            //背景点击效果
            holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        } catch (Exception e){
            e.printStackTrace();
        }

        //选项Dialog
        if(holder.mButton != null) {
            //设置按钮着色
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);

            //按钮点击效果
            holder.mButton.setBackground(Theme.getPressDrawable(
                    mDefaultDrawable,
                    mSelectDrawable,
                    ThemeStore.getRippleColor(),
                    null,null));

            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mMultiChoice.isShow())
                        return;
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Item", temp);
                    mContext.startActivity(intent);
                }
            });
        }

        if(mOnItemClickLitener != null && holder.mContainer != null) {
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.getAdapterPosition() - 1 < 0){
                        ToastUtil.show(mContext,R.string.illegal_arg);
                        return;
                    }
                    mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition() - 1);
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(holder.getAdapterPosition() - 1 < 0){
                        ToastUtil.show(mContext,R.string.illegal_arg);
                        return true;
                    }
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition() - 1);
                    return true;
                }
            });
        }

        if(mType == ALLSONG){
            if(MultiChoice.TAG.equals(SongFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position - 1))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        } else {
            if(MultiChoice.TAG.equals(RecetenlyActivity.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position - 1))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        }

    }
}
