package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.RecentlyAdapter;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-3-4.
 */
public class RecentlyFragment extends Fragment {
    private ArrayList<MP3Info> mInfoList = new ArrayList<>();
    private int mType;
    private ListView mListView;
    private RecentlyAdapter mAdapter;
    private TextView mTextTip;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mType = getArguments().getInt("Type");
        mInfoList = DBUtil.getMP3ListByIds(DBUtil.mWeekList);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recently,null);
        mTextTip = (TextView)rootView.findViewById(R.id.dayweek_text);
        mListView = (ListView)rootView.findViewById(R.id.dayweek_list);
        if(mInfoList == null || mInfoList.size() == 0){
            mListView.setVisibility(View.GONE);
            mTextTip.setVisibility(View.VISIBLE);
            return rootView;
        }

        mAdapter = new RecentlyAdapter(getActivity(),mInfoList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                getActivity().sendBroadcast(intent);
                view.setSelected(true);
                DBUtil.setPlayingList(mType == Constants.DAY ? DBUtil.mTodayList : DBUtil.mWeekList);
            }
        });

        return rootView;
    }

    public RecentlyAdapter getAdapter(){
        return mAdapter;
    }
}
