package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.adapters.FolderAdapter;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.ui.activities.ChildHolderActivity;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 文件夹Fragment
 */
public class FolderFragment extends Fragment {
    private static boolean mIsRunning = false;
    public static FolderFragment mInstance;
    private RecyclerView mRecyclerView;
    private FolderAdapter mAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_folder,null);

        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(getContext(),RecyclerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new FolderAdapter(getContext());
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                intent.putExtra("Id", position);
                intent.putExtra("Type", Constants.FOLDER_HOLDER);
                if(Global.mFolderMap == null || Global.mFolderMap.size() < 0)
                    return;
                Iterator it = Global.mFolderMap.keySet().iterator();
                String full_path = null;
                for(int i = 0 ; i <= position ; i++)
                    full_path = it.next().toString();
                intent.putExtra("Title", full_path);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }


    public void UpdateAdapter() {
        if(mRecyclerView != null && mRecyclerView.getAdapter() != null){
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsRunning = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mIsRunning = false;
    }


}
