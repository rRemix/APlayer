package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.interfaces.SortChangeCallback;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.RecetenlyActivity;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends HeaderAdapter{
    public static String ASCDESC = " asc";
    public static String SORT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

    private MultiChoice mMultiChoice;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    private SortChangeCallback mCallback;

    public SongAdapter(Context context, Cursor cursor, MultiChoice multiChoice, int type) {
        super(context,cursor,multiChoice,R.layout.layout_topbar_1);
        this.mMultiChoice = multiChoice;
        this.mType = type;
        int size = DensityUtil.dip2px(mContext,60);
        //读取排序方式
        ASCDESC = SPUtil.getValue(context,"Setting","AscDesc"," asc");
        SORT = SPUtil.getValue(context,"Setting","Sort",MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    public void setChangeCallback(SortChangeCallback callback){
        mCallback = callback;
    }

    @Override
    public BaseViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        return viewType == TYPE_HEADER ?
                new HeaderHolder(mHeaderView) :
                new SongViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_song_recycle,parent,false));
    }

    @Override
    public void onBind(final BaseViewHolder baseHolder, final int position) {
        if(position == 0){
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mCursor == null || mCursor.getCount() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }

            if(mType == RECENTLY){
                //最近添加 隐藏排序方式
                headerHolder.mSortContainer.setVisibility(View.GONE);
            }
            //显示当前排序方式
            headerHolder.mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? "按添加时间" : "按字母");
            headerHolder.mAscDesc.setText(!ASCDESC.equals(" asc") ? "降序" : "升序");
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
        if(mCursor == null || !mCursor.moveToPosition(position - 1)){
            return;
        }
        final SongViewHolder holder = (SongViewHolder) baseHolder;
        final MP3Item temp = new MP3Item(mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
                mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),0,"","",0,"","",0);

        //获得当前播放的歌曲
        final MP3Item currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean highlight = temp.getId() == MusicService.getCurrentMP3().getId();
            holder.mName.setTextColor(highlight ?
                    ThemeStore.getAccentColor():
                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
            holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.INVISIBLE);
            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.isPlay() && !holder.mColumnView.getStatus() && highlight){
                holder.mColumnView.startAnim();
            }
            else if(!MusicService.isPlay() && holder.mColumnView.getStatus()){
                holder.mColumnView.stopAnim();
            }
        }

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
//            new AsynLoadImage(holder.mImage).execute(temp.getAlbumId(),Constants.URL_ALBUM);
            MediaStoreUtil.setImageUrl(holder.mImage,temp.getAlbumId());
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
                        ToastUtil.show(mContext,"参数错误");
                        return;
                    }
                    mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition() - 1);
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(holder.getAdapterPosition() - 1 < 0){
                        ToastUtil.show(mContext,"参数错误");
                        return true;
                    }
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition() - 1);
                    return true;
                }
            });
        }

        if(mType == ALLSONG){
            if(MultiChoice.TAG.equals(SongFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        } else {
            if(MultiChoice.TAG.equals(RecetenlyActivity.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        }

    }

    private void OnClick(HeaderHolder headerHolder, View v){
        switch (v.getId()){
            case R.id.play_shuffle:
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                Global.setPlayQueue(Global.AllSongList,mContext,intent);
                break;
            case R.id.asc_desc:
                if(ASCDESC.equals(" asc")){
                    ASCDESC = " desc";
                } else {
                    ASCDESC = " asc";
                }
                headerHolder.mAscDesc.setText(!ASCDESC.equals(" asc") ? "降序" : "升序");
                SPUtil.putValue(mContext,"Setting","AscDesc", ASCDESC);
                mCallback.AscDescChange(ASCDESC);
                break;
            case R.id.sort:
                if(SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER)){
                    SORT = MediaStore.Audio.Media.DATE_ADDED;
                } else {
                    SORT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
                }
                headerHolder.mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? "按添加时间" : "按字母" );
                SPUtil.putValue(mContext,"Setting","Sort",SORT);
                mCallback.SortChange(SORT);
                break;
        }
    }

    static class SongViewHolder extends BaseViewHolder{
        @BindView(R.id.sq)
        View mSQ;
        @BindView(R.id.song_title)
        TextView mName;
        @BindView(R.id.song_other)
        TextView mOther;
        @BindView(R.id.song_head_image)
        SimpleDraweeView mImage;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        @BindView(R.id.song_button)
        ImageButton mButton;
        @BindView(R.id.item_root)
        View mContainer;
        SongViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderHolder extends BaseViewHolder{
        View mRoot;
        @BindView(R.id.divider)
        View mDivider;
        @BindView(R.id.play_shuffle)
        View mShuffle;
        @BindView(R.id.asc_desc)
        TextView mAscDesc;
        @BindView(R.id.sort)
        TextView mSort;
        @BindView(R.id.sort_container)
        View mSortContainer;

        HeaderHolder(View itemView) {
            super(itemView);
            mRoot = itemView;
        }
    }
}
