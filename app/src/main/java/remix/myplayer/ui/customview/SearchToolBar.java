package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import remix.myplayer.R;
import remix.myplayer.theme.Theme;

/**
 * Created by taeja on 16-2-29.
 */

/**
 * 自定义搜索控件
 */
public class SearchToolBar extends Toolbar {
    private static final String TAG = "SearchView";
    private Context mContext;
    private EditText mEditText;
    private ImageButton mButtonClear;
    private SearchListener mSearchListener;
    public SearchToolBar(Context context) {
        super(context);
        mContext = context;
    }
    public SearchToolBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public SearchToolBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    private void init(){
        mEditText = (EditText)findViewById(R.id.search_input);
        //设置EditText光标与下划线颜色
        mEditText.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
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
        });

        mButtonClear = (ImageButton)findViewById(R.id.search_clear);
        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.setText("");
                mButtonClear.setVisibility(INVISIBLE);
                if(mSearchListener != null)
                    mSearchListener.onClear();
            }
        });

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        init();
    }
    public void UpdateContent(String text){
        mEditText.setText(text);
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
