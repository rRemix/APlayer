package remix.myplayer.ui.customviews;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-2-29.
 */

/**
 * 自定义搜索控件
 */
public class SearchView extends LinearLayout {
    private static final String TAG = "SearchView";
    private Context mContext;
    private EditText mEditText;
    private ImageButton mButtonBack;
    private ImageButton mButtonClear;
    private SearchListener mSearchListener;
    private TextView mButtonSearch;
    public SearchView(Context context) {
        super(context);
        mContext = context;
    }
    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    private void init(){
        mEditText = (EditText)findViewById(R.id.search_text);
        //设置EditText光标与下划线颜色
//        mEditText.getBackground().setColorFilter(getResources().getColor(R.color.progress_complete), PorterDuff.Mode.SRC_ATOP);

//        final int size = DBUtil.mSearchKeyList.size();
//        String[] strs = (String[]) DBUtil.mSearchKeyList.toArray(new String[size]);
//        ArrayAdapter adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, strs);
//        mEditText.setAdapter(adapter);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG,"onTextChanged --- CharSequence:" + s);
                //EditText不为空时显示尾部的删除按钮
                if(s != null){
                    if(s.toString().equals("")){
                        if(mSearchListener != null) {
                            mSearchListener.onClear();
                            mButtonClear.setVisibility(INVISIBLE);
                            mButtonSearch.setEnabled(false);
                        }
                    }else {
                        if (mSearchListener != null) {
                            mSearchListener.onSearch(s.toString(),false);
                            mButtonClear.setVisibility(VISIBLE);
                            mButtonSearch.setEnabled(true);
                        }
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mButtonBack = (ImageButton)findViewById(R.id.btn_search_back);
        mButtonBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSearchListener != null){
                    mSearchListener.onBack();
                }
            }
        });
        mButtonClear = (ImageButton)findViewById(R.id.btn_search_clear);
        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.setText("");
                mButtonClear.setVisibility(INVISIBLE);
                if(mSearchListener != null)
                    mSearchListener.onClear();
            }
        });
        mButtonSearch = (TextView)findViewById(R.id.btn_search_go);
        mButtonSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSearchListener != null)
                    mSearchListener.onSearch(mEditText.getText().toString(),true);
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
        public void onSearch(String key,boolean isclick);
        public void onClear();
        public void onBack();
    }
}
