package remix.myplayer.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.promeg.pinyinhelper.Pinyin;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.interfaces.SortChangeCallback;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-6-24.
 */
public class ChildHolderAdapter extends HeaderAdapter implements FastScrollRecyclerView.SectionedAdapter{
    //升序
    public static final int ASC = 0;
    //降序
    public static final int DESC = 1;
    //按字母排序
    public static final int NAME = 0;
    //按添加时间排序
    public static final int ADDTIME = 1;
    private ArrayList<MP3Item> mInfoList;
    private int mType;
    private String mArg;
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    private SortChangeCallback mCallback;
    //当前是升序还是降序 0:升序 1:降序
    public static int ASC_DESC = 0;
    //当前是按字母排序还是添加时间 0:字母 1:时间
    public static int SORT = 0;

    public ChildHolderAdapter(Context context, int type, String arg,MultiChoice multiChoice){
        super(context,null,multiChoice);
        this.mContext = context;
        this.mType = type;
        this.mArg = arg;
        this.mMultiChoice = multiChoice;
        //读取之前的排序方式
        SORT = SPUtil.getValue(context,"Setting","SubDirSort", NAME);
        ASC_DESC = SPUtil.getValue(context,"Setting","SubDirAscDesc", ASC);
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.RECTANGLE,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.RECTANGLE,ThemeStore.getSelectColor(),size,size);
    }

    public void setCallback(SortChangeCallback callback){
        mCallback = callback;
    }

    public void setList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public BaseViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        return viewType == TYPE_HEADER ?
                new SongAdapter.HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_1,parent,false)) :
                new ChildHolderViewHoler(LayoutInflater.from(mContext).inflate(R.layout.item_child_holder,parent,false));
    }

    @Override
    public void onBind(final BaseViewHolder baseHolder, int position) {
        if(position == 0){
            final SongAdapter.HeaderHolder headerHolder = (SongAdapter.HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mInfoList == null || mInfoList.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }
            //显示当前排序方式
            headerHolder.mAscDesc.setText(ASC_DESC != ASC ? "降序" : "升序");
            headerHolder.mSort.setText(SORT != NAME ?  "按添加时间" : "按字母");
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

        if(mInfoList == null || position > mInfoList.size())
            return;
        final ChildHolderViewHoler holder = (ChildHolderViewHoler) baseHolder;
        final MP3Item temp = mInfoList.get(position - 1);
        if(temp == null || temp.getId() < 0 || temp.Title.equals(mContext.getString(R.string.song_lose_effect))) {
            holder.mTitle.setText(R.string.song_lose_effect);
            holder.mColumnView.setVisibility(View.INVISIBLE);
            holder.mButton.setVisibility(View.INVISIBLE);
        } else {
            holder.mColumnView.setVisibility(View.VISIBLE);
            holder.mButton.setVisibility(View.VISIBLE);
            //获得正在播放的歌曲
            final MP3Item currentMP3 = MusicService.getCurrentMP3();
            //判断该歌曲是否是正在播放的歌曲
            //如果是,高亮该歌曲，并显示动画
//            if(SPUtil.getValue(mContext,"Setting","ShowHighLight",false))
//            if(currentMP3 != null){
//                boolean highlight = temp.getId() == currentMP3.getId();
//                holder.mTitle.setTextColor(highlight ?
//                        ThemeStore.getAccentColor() :
//                        ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
//                holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.INVISIBLE);
//
//                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
//                if(MusicService.isPlay() && !holder.mColumnView.getStatus() && highlight){
//                    holder.mColumnView.startAnim();
//                }
//                else if(!MusicService.isPlay() && holder.mColumnView.getStatus()){
//                    holder.mColumnView.stopAnim();
//                }
//            }

            //是否无损
            String prefix = temp.getDisplayname().substring(temp.getDisplayname().lastIndexOf(".") + 1);
            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);

            //设置标题
            holder.mTitle.setText(CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE));

            if(holder.mButton != null) {
                //设置按钮着色
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);

                //item点击效果
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
                        if (mType == Constants.PLAYLIST) {
                            intent.putExtra("IsDeletePlayList", true);
                            intent.putExtra("PlayListName", mArg);
                        }
                        mContext.startActivity(intent);
                    }
                });
            }
        }

        //背景点击效果
        holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        if(holder.mContainer != null && mOnItemClickLitener != null){
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.getAdapterPosition() - 1 < 0){
                        ToastUtil.show(mContext,"参数错误");
                        return;
                    }
                    mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition() - 1);
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

        if(MultiChoice.TAG.equals(ChildHolderActivity.TAG) &&
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            mMultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return mInfoList != null ? mInfoList.size() + 1 : 0;
    }

    public void OnClick(SongAdapter.HeaderHolder headerHolder, View v){
        switch (v.getId()){
            case R.id.play_shuffle:
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                //设置正在播放列表
                ArrayList<Integer> IDList = new ArrayList<>();
                for (MP3Item info : mInfoList)
                    IDList.add(info.getId());
                if(IDList == null || IDList.size() == 0){
                    ToastUtil.show(mContext,R.string.no_song);
                    return;
                }
                Global.setPlayQueue(IDList,mContext,intent);
                break;
            case R.id.sort:
                if(SORT == NAME){
                    SORT = ADDTIME;
                } else if(SORT == ADDTIME){
                    SORT = NAME;
                }
                SPUtil.putValue(mContext,"Setting","SubDirSort",SORT);
                headerHolder.mSort.setText(SORT != NAME ?  "按添加时间" : "按字母");
                mCallback.SortChange();
                break;
            case R.id.asc_desc:
                if(ASC_DESC == ASC){
                    ASC_DESC = DESC;
                } else if(ASC_DESC == DESC){
                    ASC_DESC = ASC;
                }
                SPUtil.putValue(mContext,"Setting","SubDirAscDesc",ASC_DESC);
                headerHolder.mAscDesc.setText(ASC_DESC != ASC ? "降序" : "升序");
                mCallback.SortChange();
                break;
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        if(position == 0)
            return "";
        if(mInfoList != null && mInfoList.size() > 0 && position < mInfoList.size() && mInfoList.get(position - 1) != null){
            String title = mInfoList.get(position).getTitle();
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
    }

    static class ChildHolderViewHoler extends BaseViewHolder {
        @BindView(R.id.sq)
        View mSQ;
        @BindView(R.id.album_holder_item_title)
        TextView mTitle;
        @BindView(R.id.song_item_button)
        public ImageButton mButton;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        public View mContainer;
        ChildHolderViewHoler(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }
}