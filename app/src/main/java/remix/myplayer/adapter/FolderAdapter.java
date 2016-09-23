package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.fragment.FolderFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.AlbumArtistFolderListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderHolder> {
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;

    public FolderAdapter(Context context) {
        this.mContext = context;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }

    @Override
    public FolderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderHolder(LayoutInflater.from(mContext).inflate(R.layout.folder_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final FolderHolder holder, final int position) {
        if(Global.mFolderMap == null || Global.mFolderMap.size() < 0)
            return ;
        //根据当前索引 获得对应的歌曲列表
        Iterator it = Global.mFolderMap.keySet().iterator();
        String temp = null;
        for(int i = 0 ; i <= position ; i++)
            temp = it.next().toString();
        //设置文件夹名字 路径名 歌曲数量
        if(temp != null){
            holder.mName.setText(temp.substring(temp.lastIndexOf("/")+ 1,temp.length()));
            holder.mPath.setText(temp);
            holder.mCount.setText(Global.mFolderMap.get(temp).size()+ "首");
        }
        //根据主题模式 设置图片
        if(holder.mImg != null) {
            holder.mImg.setImageResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.scan_icn_folder_day : R.drawable.scan_icn_folder);
        }

        final String full_path = temp;
        if(holder.mButton != null) {
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                    final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                    popupMenu.getMenuInflater().inflate(R.menu.folder_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new AlbumArtistFolderListener(mContext,
                            holder.getAdapterPosition(),
                            Constants.FOLDER_HOLDER,
                            full_path));
                    popupMenu.setGravity(Gravity.END);
                    popupMenu.show();
                }
            });
        }

        if(mOnItemClickLitener != null && holder.mContainer != null) {
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition());
                    return true;
                }
            });
        }

        if(MultiChoice.TAG.equals(FolderFragment.TAG) &&
                MainActivity.MultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            MainActivity.MultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return Global.mFolderMap == null ? 0 : Global.mFolderMap.size();
    }

    public static class FolderHolder extends BaseViewHolder {
        public View mContainer;
        @BindView(R.id.folder_image)
        public ImageView mImg;
        @BindView(R.id.folder_name)
        public TextView mName;
        @BindView(R.id.folder_path)
        public TextView mPath;
        @BindView(R.id.folder_num)
        public TextView mCount;
        @BindView(R.id.folder_button)
        public ImageButton mButton;
        public FolderHolder(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }
}
