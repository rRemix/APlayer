package remix.myplayer.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/12/5.
 */
public class FolderFragment extends Fragment {
    private ListView mListView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mListView = new ListView(getContext());
        mListView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,AbsListView.LayoutParams.MATCH_PARENT));
        mListView.setOnItemClickListener(new ListViewListener());
        mListView.setAdapter(new FolderAdapter(getContext(),inflater));
        rootView.addView(mListView);
        return rootView;
    }


    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
            intent.putExtra("Id", position);
            intent.putExtra("Type", Constants.FOLDER_HOLDER);
            intent.putExtra("Title", DBUtil.mFolderList.get(position));
            startActivity(intent);

        }
    }
}
