package remix.myplayer.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.facebook.common.util.ByteConstants;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.ui.widget.FilterSizeSeekBar;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by taeja on 16-3-7.
 */
public class ScanActivity extends ToolbarActivity {
    @BindView(R.id.custom_seekbar)
    FilterSizeSeekBar mFilterSizeSeekbar;
    private int mPosition;
    public static ArrayList<Integer> mSizeList = new ArrayList<>();

    //几种扫描大小
    static {
        mSizeList.add(0);
        mSizeList.add(500 * ByteConstants.KB);
        mSizeList.add(ByteConstants.MB);
        mSizeList.add(2 * ByteConstants.KB);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mFilterSizeSeekbar.setPosition(msg.arg1);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        setUpToolbar(getString(R.string.back));
        initSeekbar();
    }

    private void initSeekbar() {
        //获得之前设置的大小对应的索引
        for (int i = 0; i < mSizeList.size(); i++) {
            mPosition = i;
            if (mSizeList.get(i) == MediaStoreUtil.SCAN_SIZE)
                break;
        }
        mFilterSizeSeekbar.setOnSeekBarChangeListener(new FilterSizeSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(FilterSizeSeekBar seekBar, int position, boolean fromUser) {
                int size = mSizeList.get(position);
                if (size >= 0) {
                    //纪录下设置的大小
                    SPUtil.putValue(ScanActivity.this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE, size);
                    MediaStoreUtil.SCAN_SIZE = size;
                }
            }

            @Override
            public void onStartTrackingTouch(FilterSizeSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(FilterSizeSeekBar seekBar) {
            }
        });
        new Thread() {
            @Override
            public void run() {
                while (!mFilterSizeSeekbar.isInit()) {
                }
                Message msg = new Message();
                msg.arg1 = mPosition;
                mHandler.sendMessage(msg);
            }
        }.start();
    }

}
