package remix.myplayer.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
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

        //启动分享心情的Activity
        (rootView.findViewById(R.id.sharebtn)).setOnClickListener(v -> {
            if (mEdit.getText().toString().equals("")) {
                ToastUtil.show(getActivity(),R.string.plz_input_sharecontent);
                return;
            }
            Intent intent = new Intent(getActivity(), RecordShareActivity.class);
            Bundle arg = new Bundle();
            arg.putString("Content", mEdit.getText().toString());
            arg.putParcelable("Song", MusicService.getCurrentMP3());
            intent.putExtras(arg);
            startActivity(intent);
        });
        mRecordContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mRecordContainer.getViewTreeObserver().removeOnPreDrawListener(this);

                int containerWidth = mRecordContainer.getWidth();
                int containerHeight = mRecordContainer.getHeight();
                ViewGroup.LayoutParams lp = mRecordContainer.getLayoutParams();
                lp.width = lp.height = containerWidth > containerHeight ? mRecordContainer.getHeight() : mRecordContainer.getWidth();
                mRecordContainer.setLayoutParams(lp);
                return true;
            }
        });

        return rootView;
    }

    @OnTextChanged(value = R.id.edit_record,callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterExplainChanged(Editable s){
    }

}

