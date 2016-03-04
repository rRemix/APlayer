package remix.myplayer.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.DayWeekAdapter;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-3-4.
 */
public class DayWeekFragment extends Fragment {
    private ArrayList<MP3Info> mInfoList = new ArrayList<>();
    private int mType;
    private ListView mListView;
    private DayWeekAdapter mAdapter;
    private TextView mTextTip;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = getArguments().getInt("Type");
        mInfoList = DBUtil.getMP3ListByIds(mType == Constants.DAY ? DBUtil.mTodayList : DBUtil.mWeekList);
        System.out.println("infolist:" + mInfoList.size());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dayweek,null);
        mTextTip = (TextView)rootView.findViewById(R.id.dayweek_text);
        mListView = (ListView)rootView.findViewById(R.id.dayweek_list);
        if(mInfoList.size() == 0){
            mListView.setVisibility(View.GONE);
            mTextTip.setVisibility(View.VISIBLE);
            return rootView;
        }

        mAdapter = new DayWeekAdapter(getActivity(),mInfoList);
        mListView.setAdapter(mAdapter);
        return rootView;
    }
}
