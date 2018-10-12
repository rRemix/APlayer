package remix.myplayer.ui.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.holder.BaseViewHolder;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2018/3/1.
 */

public class LyricAdapter extends BaseAdapter<LrcRow, LyricAdapter.LrcViewHolder> {
    private RecyclerView mReclcyerView;
    private LinearLayoutManager mLinearLayoutManager;
    private final int OTHER_COLOR = ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_normal_day : R.color.lrc_normal_night);
    private final int HIGHLIGHT_COLOR = ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_highlight_day : R.color.lrc_highlight_night);

    public LyricAdapter(Context context, int layoutId, RecyclerView recyclerView) {
        super(context, layoutId);
        mReclcyerView = recyclerView;
        mLinearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    @Override
    protected void convert(LrcViewHolder holder, LrcRow lrcRow, int position) {
        holder.mContent.setText(lrcRow.getContent());
        holder.mTranslation.setText(lrcRow.hasTranslate() ? lrcRow.getTranslate() : "");
        holder.mTranslation.setVisibility(lrcRow.hasTranslate() ? View.VISIBLE : View.GONE);
        int first = mLinearLayoutManager.findFirstVisibleItemPosition();
        int last = mLinearLayoutManager.findLastVisibleItemPosition();
//        LogUtil.d("LyricAdapter","first: " + first + " last: " + last + " position: " + position);
        if (first > 0 && last > 0 && position == (first + last) / 2) {
            holder.mContent.setTextColor(HIGHLIGHT_COLOR);
            holder.mContent.setTextSize(14);
        } else {
            holder.mContent.setTextColor(OTHER_COLOR);
            holder.mContent.setTextSize(14);
        }
        LogUtil.d("LyricAdapter", "Position: " + position + " Height: " + getHeightForPosition(position));
    }

    private int getHeightForPosition(final int position) {
        if (position >= mDatas.size() || position < 0)
            return 0;
        final LrcRow lrcRow = mDatas.get(position);
        final View itemRoot = LayoutInflater.from(mContext).inflate(R.layout.item_lyric, null);

        ((TextView) itemRoot.findViewById(R.id.item_content)).setText(lrcRow.getContent());
        final TextView translate = itemRoot.findViewById(R.id.item_translation);
        if (lrcRow.hasTranslate()) {
            translate.setVisibility(View.VISIBLE);
            translate.setText(lrcRow.getTranslate());
        } else {
            translate.setVisibility(View.GONE);
        }

        PopupWindow popupWindow = new PopupWindow(itemRoot, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemRoot.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = itemRoot.getMeasuredHeight();
        int width = itemRoot.getMeasuredWidth();
        return height;
    }

    static class LrcViewHolder extends BaseViewHolder {
        @BindView(R.id.item_content)
        TextView mContent;
        @BindView(R.id.item_translation)
        TextView mTranslation;
        @BindView(R.id.item_indicator)
        View mIndicator;

        public LrcViewHolder(View itemView) {
            super(itemView);
        }
    }
}
