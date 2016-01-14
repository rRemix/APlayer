package remix.myplayer.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.adapters.SlideMenuAdapter;

/**
 * Created by Remix on 2015/12/10.
 */
public class SlideMenuFragment extends Fragment{
    private ListView mListView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = getActivity().getLayoutInflater().inflate(R.layout.slide_menu,null);
//        mListView = (ListView)rootView.findViewById(R.id.slide);
//        mListView.setAdapter(new SlideMenuAdapter(getActivity().getLayoutInflater()));
//        return rootView;
        return null;
    }
}
