package remix.myplayer.fragments;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;

import remix.myplayer.R;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/5.
 */
public class FolderFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout rootView = new LinearLayout(getContext());
        rootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ListView listView = new ListView(getContext());
        listView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,AbsListView.LayoutParams.MATCH_PARENT));

        listView.setAdapter(new FolderAdapter(inflater,getContext()));
        listView.setOnItemClickListener(new ListViewListener());
        rootView.addView(listView);
        return rootView;
    }
    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
            intent.putExtra("Id", position);
            intent.putExtra("Type",Utility.FOLDER_HOLDER);
            intent.putExtra("Title","文件夹");
            startActivity(intent);

//            AlbumHolderFragment fragment = new AlbumHolderFragment();
//            Bundle bundle = new Bundle();
//            bundle.putInt("Id", Utility.mAlbumList.get(position).getAlbumId());
//            fragment.setArguments(bundle);
//
//            FragmentManager fm = MainActivity.mInstance.getSupportFragmentManager();
//
//            List<Fragment> fragList = fm.getFragments();
//            for(Fragment fragment1 : fragList)
//            {
//                fm.beginTransaction().hide(fragment1);
//            }
//            fm.beginTransaction().replace(R.id.main_fragment_container, fragment)
//                    .addToBackStack(null).commit();
//
//            int count = MainActivity.mInstance.getSupportFragmentManager().getBackStackEntryCount();
//            System.out.println(count);
        }
    }
}
