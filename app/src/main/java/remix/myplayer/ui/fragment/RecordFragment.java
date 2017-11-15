package remix.myplayer.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.RecordShareActivity;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/28.
 */

/**
 * 心情记录的Fragment
 */
public class RecordFragment extends BaseFragment{
    @BindView(R.id.edit_record)
    EditText mEdit;
    @BindView(R.id.record_container)
    View mRecordContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = RecordFragment.class.getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_record,container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        mEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                mRecord = s.toString();
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //启动分享心情的Activity
        (rootView.findViewById(R.id.sharebtn)).setOnClickListener(v -> {
            if (mEdit.getText().toString().equals("")) {
                ToastUtil.show(getActivity(),R.string.plz_input_sharecontent);
                return;
            }
            Intent intent = new Intent(getActivity(), RecordShareActivity.class);
            Bundle arg = new Bundle();
            arg.putString("Content", mEdit.getText().toString());
            arg.putSerializable("Song", MusicService.getCurrentMP3());
            intent.putExtras(arg);
            startActivity(intent);
        });

        mRecordContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecordContainer.getViewTreeObserver().removeOnPreDrawListener(this);

//                //左右各11dp的间距
//                int coverSize = Math.min(mRecordContainer.getWidth(),mRecordContainer.getHeight()) - DensityUtil.dip2px(mContext,11) * 2;
//                ViewGroup.LayoutParams lp = mRecordContainer.getLayoutParams();
//                lp.width = lp.height = coverSize;
//                mRecordContainer.setLayoutParams(lp);
                ViewGroup.LayoutParams lp = mRecordContainer.getLayoutParams();
                lp.width = lp.height;
                mRecordContainer.setLayoutParams(lp);

                return true;
            }
        });

        return rootView;
    }


}

