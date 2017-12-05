package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.OnUpdateHighLightListener;
import remix.myplayer.interfaces.SortChangeCallback;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.RecetenlyActivity;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScroller;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.uri.LibraryUriRequest;
import remix.myplayer.uri.RequestConfig;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.uri.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends HeaderAdapter<Song,BaseViewHolder> implements FastScroller.SectionIndexer,OnUpdateHighLightListener {
    //升序还是降序
    public static String ASCDESC = SPUtil.getValue(APlayerApplication.getContext(),"Setting","AscDesc"," asc");
    //按字母排序还是按添加时间排序
    public static String SORT = SPUtil.getValue(APlayerApplication.getContext(),"Setting","Sort",MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

    protected MultiChoice mMultiChoice;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    private SortChangeCallback mCallback;

    private RecyclerView mRecyclerView;
    private int mLastIndex;

    public SongAdapter(Context context,int layoutId, MultiChoice multiChoice, int type,RecyclerView recyclerView) {
        super(context,layoutId,multiChoice);
        mMultiChoice = multiChoice;
        mType = type;
        mRecyclerView = recyclerView;
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    public void setChangeCallback(SortChangeCallback callback){
        mCallback = callback;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == TYPE_HEADER ?
                new HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_1,parent,false)) :
                new SongViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_song_recycle,parent,false));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        if(holder instanceof SongViewHolder){
            ((SongViewHolder) holder).mImage.setImageURI(Uri.EMPTY);
        }
    }

    @Override
    protected void convert(BaseViewHolder baseHolder, final Song song, int position) {
        if(position == 0){
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mDatas == null || mDatas.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            } else {
                headerHolder.mRoot.setVisibility(View.VISIBLE);
            }

            if(mType == RECENTLY){
                //最近添加 隐藏排序方式
                headerHolder.mSortContainer.setVisibility(View.GONE);
            }
            //显示当前排序方式
            headerHolder.mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? R.string.sort_as_add_time : R.string.sort_as_letter);
            headerHolder.mAscDesc.setText(!ASCDESC.equals(" asc") ? R.string.sort_as_desc : R.string.sort_as_asc);
            View.OnClickListener listener = v -> OnClick(headerHolder,v);
            headerHolder.mSort.setOnClickListener(listener);
            headerHolder.mAscDesc.setOnClickListener(listener);
            headerHolder.mShuffle.setOnClickListener(listener);
            return;
        }

        if(!(baseHolder instanceof SongViewHolder))
            return;
        final SongViewHolder holder = (SongViewHolder) baseHolder;

//        if(SPUtil.getValue(mContext,"Setting","ShowHighLight",true)){
//            //获得当前播放的歌曲
//            final Song currentMP3 = MusicService.getCurrentMP3();
//            //判断该歌曲是否是正在播放的歌曲
//            //如果是,高亮该歌曲，并显示动画
//            if(currentMP3 != null){
//                boolean highlight = song.getId() == MusicService.getCurrentMP3().getId();
//                if(highlight)
//                    mLastIndex = position;
//                holder.mName.setTextColor(highlight ?
//                        ThemeStore.getAccentColor():
//                        ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
//                holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.GONE);
//                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
//                if(MusicService.isPlay() && !holder.mColumnView.getStatus() && highlight){
//                    holder.mColumnView.startAnim();
//                }
//                else if(!MusicService.isPlay() && holder.mColumnView.getStatus()){
//                    holder.mColumnView.stopAnim();
//                }
//            }
//        }
        //封面

        new LibraryUriRequest(holder.mImage, getSearchRequestWithAlbumType(song),new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();

        //是否为无损
        if(!TextUtils.isEmpty(song.getDisplayname())){
            String prefix = song.getDisplayname().substring(song.getDisplayname().lastIndexOf(".") + 1);
            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
        }

        //设置歌曲名
        holder.mName.setText(song.getTitle());

        //艺术家与专辑
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

        //背景点击效果
        holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        //设置按钮着色
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

        //按钮点击效果
        holder.mButton.setBackground(Theme.getPressDrawable(
                mDefaultDrawable,
                mSelectDrawable,
                ThemeStore.getRippleColor(),
                null,null));

        holder.mButton.setOnClickListener(v -> {
            if(mMultiChoice.isShow())
                return;
            Intent intent = new Intent(mContext, OptionDialog.class);
            intent.putExtra("Song", song);
            mContext.startActivity(intent);
        });

        holder.mContainer.setOnClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return;
            }
            mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition() - 1);
        });
        holder.mContainer.setOnLongClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return true;
            }
            mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition() - 1);
            return true;
        });

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

    protected void OnClick(HeaderHolder headerHolder, View v){
        switch (v.getId()){
            case R.id.play_shuffle:
                MusicService.getInstance().setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(MusicService.ACTION_CMD);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                if(mType == ALLSONG){
                    if(Global.AllSongList == null || Global.AllSongList.size() == 0){
                        ToastUtil.show(mContext,R.string.no_song);
                        return;
                    }
                    Global.setPlayQueue(Global.AllSongList,mContext,intent);
                } else {
                    ArrayList<Integer> IdList = new ArrayList<>();
                    for(int i = 0 ; i < mDatas.size();i++){
                        IdList.add(mDatas.get(i).getId());
                    }
                    if(IdList.size() == 0){
                        ToastUtil.show(mContext,R.string.no_song);
                        return;
                    }
                    Global.setPlayQueue(IdList,mContext,intent);
                }

                break;
            case R.id.asc_desc:
                if(ASCDESC.equals(" asc")){
                    ASCDESC = " desc";
                } else {
                    ASCDESC = " asc";
                }
                headerHolder.mAscDesc.setText(!ASCDESC.equals(" asc") ? R.string.sort_as_desc : R.string.sort_as_asc);
                SPUtil.putValue(mContext,"Setting","AscDesc", ASCDESC);
                mCallback.SortChange();
                break;
            case R.id.sort:
//                ASCDESC = SPUtil.getValue(context,"Setting","AscDesc"," asc");
//                SORT = SPUtil.getValue(context,"Setting","Sort",MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if(SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER)){
                    SORT = MediaStore.Audio.Media.DATE_ADDED;
                } else {
                    SORT = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
                }
                headerHolder.mSort.setText(!SORT.equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER) ? R.string.sort_as_add_time : R.string.sort_as_letter);
                SPUtil.putValue(mContext,"Setting","Sort",SORT);
                mCallback.SortChange();
                break;
        }
    }

    @Override
    public String getSectionText(int position) {
        if(position == 0)
            return "";
        if(mDatas != null && position - 1 < mDatas.size()){
            String title = mDatas.get(position - 1).getTitle();
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
    }

    /**
     * 更新高亮歌曲
     */
    @Override
    public void onUpdateHighLight() {
        Song currentSong = MusicService.getCurrentMP3();
        if(currentSong != null && mDatas != null && mDatas.indexOf(currentSong) >= 0){
            int index = mDatas.indexOf(currentSong) + 1;

            //播放的是同一首歌曲
            if(index == mLastIndex){
                return;
            }
            SongViewHolder newHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(index) instanceof SongViewHolder){
                newHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
            }
            SongViewHolder oldHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(mLastIndex) instanceof SongViewHolder){
                oldHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mLastIndex);
            }

            if(newHolder != null){
                newHolder.mName.setTextColor(ThemeStore.getAccentColor());
                newHolder.mColumnView.setVisibility(View.VISIBLE);
                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
                if(MusicService.isPlay() && !newHolder.mColumnView.getStatus()){
                    newHolder.mColumnView.startAnim();
                }
                else if(!MusicService.isPlay() && newHolder.mColumnView.getStatus()){
                    newHolder.mColumnView.stopAnim();
                }
            }
            if(oldHolder != null){
                oldHolder.mName.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
                oldHolder.mColumnView.stopAnim();
                oldHolder.mColumnView.setVisibility(View.GONE);
            }
            mLastIndex = index;
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
