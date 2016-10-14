//package remix.myplayer.fragment;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.annotation.Nullable;
//import android.support.v4.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import java.util.ArrayList;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import remix.myplayer.R;
//import remix.myplayer.adapter.RecentlyAdapter;
//import remix.myplayer.model.MP3Item;
//import remix.myplayer.util.Constants;
//import remix.myplayer.util.DBUtil;
//import remix.myplayer.util.Global;
//
///**
// * Created by taeja on 16-3-4.
// */
//public class RecentlyFragment extends BaseFragment {
//    private ArrayList<MP3Item> mInfoList = new ArrayList<>();
//    private int mType;
//    @BindView(R.id.dayweek_list)
//    ListView mListView;
//    @BindView(R.id.dayweek_text)
//    TextView mTextTip;
//
//    private RecentlyAdapter mAdapter;
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        mType = getArguments().getInt("Type");
//        mInfoList = DBUtil.getMP3ListByIds(Global.mWeekList);
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.fragment_recently,null);
//        mUnBinder = ButterKnife.bind(this,rootView);
//
//        if(mInfoList == null || mInfoList.size() == 0){
//            mListView.setVisibility(View.GONE);
//            mTextTip.setVisibility(View.VISIBLE);
//            return rootView;
//        }
//
//        mAdapter = new RecentlyAdapter(getActivity(),mInfoList);
//        mListView.setAdapter(mAdapter);
//        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(Constants.CTL_ACTION);
//                Bundle arg = new Bundle();
//                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
//                arg.putInt("Position", position);
//                intent.putExtras(arg);
//                getActivity().sendBroadcast(intent);
//                view.setSelected(true);
//                Global.setPlayQueue(mType == Constants.DAY ? Global.mTodayList : Global.mWeekList);
//            }
//        });
//
//        return rootView;
//    }
//
////    public RecentlyAdapter getAdapter(){
////        return mAdapter;
////    }
//}
