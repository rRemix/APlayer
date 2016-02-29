package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.activities.SearchActivity;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-2-29.
 */
public class SearchView extends LinearLayout {
    private static final String TAG = "SearchView";
    private Context mContext;
    private AppCompatAutoCompleteTextView mEditText;
    private ImageButton mButtonBack;
    private ImageButton mButtonClear;
    private SearchListener mSearchListener;
    private TextView mButtonSearch;
    private ListView mListView;
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
        mEditText = (AppCompatAutoCompleteTextView)findViewById(R.id.search_text);
//        final int size = DBUtil.mSearchKeyList.size();
//        String[] strs = (String[])DBUtil.mSearchKeyList.toArray(new String[size]);
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
                    if(mSearchListener != null)
                        mSearchListener.onSearch(s.toString());
                    mButtonClear.setVisibility(VISIBLE);
                    mButtonSearch.setEnabled(true);
                }
                else {
                    mButtonClear.setVisibility(INVISIBLE);
                    mButtonSearch.setEnabled(true);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mButtonBack = (ImageButton)findViewById(R.id.btn_search_back);
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
                    mSearchListener.onSearch(mEditText.getText().toString());
            }
        });
//        mListView = (ListView)findViewById(R.id.search_list_hint);
    }

//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
//        init();
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        init();
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        init();
    }
    public void addSearchListener(SearchListener listener){
        mSearchListener = listener;
    }
    public interface SearchListener{
        public void onSearch(String key);
        public void onClear();
    }
}
