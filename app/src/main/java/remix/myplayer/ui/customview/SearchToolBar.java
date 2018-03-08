package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;

/**
 * Created by taeja on 16-2-29.
 */

/**
 * 自定义搜索控件
 */
public class SearchToolBar extends Toolbar {
    private static final String TAG = "SearchView";

    @BindView(R.id.search_input)
    EditText mEditText;
    @BindView(R.id.search_clear)
    ImageButton mButtonClear;

    private SearchListener mSearchListener;
    private Unbinder mUnBinder;

    public SearchToolBar(Context context) {
        super(context);
    }
    public SearchToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SearchToolBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUnBinder.unbind();
    }

    @OnTextChanged(value = R.id.search_input,callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterExplainChanged(Editable s){
        //EditText不为空时显示尾部的删除按钮
        if(s != null){
            if(s.toString().equals("")){
                if(mSearchListener != null) {
                    mSearchListener.onClear();
                    mButtonClear.setVisibility(INVISIBLE);
                }
            }else {
                if (mSearchListener != null) {
                    mSearchListener.onSearch(s.toString(),false);
                    mButtonClear.setVisibility(VISIBLE);
                }
            }
        }
    }

    private void init(){
        //设置EditText光标与下划线颜色
        mUnBinder = ButterKnife.bind(this);
        mEditText.getBackground().setColorFilter(
                ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white),
                PorterDuff.Mode.SRC_ATOP);
        mEditText.setTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
        mEditText.setHintTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor : R.color.search_hint_text_color));
        mEditText.setOnEditorActionListener((v, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH){
                afterExplainChanged(v.getEditableText());
                return true;
            }
            return false;
        });
        mButtonClear.setOnClickListener(v -> {
            mEditText.setText("");
            mButtonClear.setVisibility(INVISIBLE);
            if(mSearchListener != null)
                mSearchListener.onClear();
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        init();
    }

    public void addSearchListener(SearchListener listener){
        mSearchListener = listener;
    }
    public interface SearchListener{
        void onSearch(String key, boolean isclick);
        void onClear();
        void onBack();
    }
}
