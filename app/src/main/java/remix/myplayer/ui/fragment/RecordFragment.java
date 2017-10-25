package remix.myplayer.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private static String mRecord = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = RecordFragment.class.getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_record,null);
        mUnBinder = ButterKnife.bind(this,rootView);

//        if(mRecord != null && !mRecord.equals("")){
//            mEdit.setText(mRecord);
//        }
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

        return rootView;
    }


}

