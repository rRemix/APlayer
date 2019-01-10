package remix.myplayer.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的适配器
 */
public class AddtoPlayListAdapter extends
    RecyclerView.Adapter<AddtoPlayListAdapter.PlayListAddToHolder> {

  private Context mContext;
  private OnItemClickListener mOnItemClickLitener;
  private Cursor mCursor;

  public AddtoPlayListAdapter(Context Context) {
    this.mContext = Context;
  }

  public void setOnItemClickLitener(OnItemClickListener l) {
    this.mOnItemClickLitener = l;
  }

  public void setCursor(Cursor cursor) {
    mCursor = cursor;
    notifyDataSetChanged();
  }

  @Override
  public PlayListAddToHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new PlayListAddToHolder(
        LayoutInflater.from(mContext).inflate(R.layout.item_playlist_addto, null));
  }

  @Override
  public void onBindViewHolder(PlayListAddToHolder holder, final int position) {
    if (mCursor.moveToPosition(position)) {
      PlayList info = PlayListUtil.getPlayListInfo(mCursor);
      if (info == null) {
        holder.mText.setText(R.string.load_playlist_error);
        return;
      }
      holder.mText.setText(info.Name);
      holder.mText.setTag(info._Id);
      if (mOnItemClickLitener != null) {
        holder.mContainer.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v, position));
      }

    }
  }

  @Override
  public int getItemCount() {
    return mCursor != null ? mCursor.getCount() : 0;
  }

  public static class PlayListAddToHolder extends BaseViewHolder {

    @BindView(R.id.playlist_addto_text)
    TextView mText;
    @BindView(R.id.item_root)
    RelativeLayout mContainer;

    PlayListAddToHolder(View itemView) {
      super(itemView);
    }
  }
}
