package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.ui.activities.RecordShareActivity;
import remix.myplayer.services.MusicService;

/**
 * Created by Remix on 2015/12/28.
 */

/**
 * 心情记录的Fragment
 */
public class RecordFragment extends Fragment{
    private EditText mEdit;
    private static String mRecord = "";
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_record,null);
        mEdit = (EditText)rootView.findViewById(R.id.edit_record);
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
        (rootView.findViewById(R.id.sharebtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEdit.getText().toString().equals("")) {
                    Toast.makeText(getContext(), getString(R.string.plz_input_sharecontent), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(getActivity(), RecordShareActivity.class);
                Bundle arg = new Bundle();
                arg.putString("Content", mEdit.getText().toString());
                arg.putSerializable("MP3Info", MusicService.getCurrentMP3());
                intent.putExtras(arg);
                startActivity(intent);
            }
        });

        return rootView;
    }


}

