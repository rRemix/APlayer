package remix.myplayer.fragments;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
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
    private Cursor mCursor = null;
    public static int mBucketNameIndex;
    public static int mSongIdIndex;
    public static int mDisPlayNameIndex;
    private FolderAdapter mAdapter;
    private ListView mListView;
    private LoaderManager mManager;
//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        mManager = getLoaderManager();
//        mManager.initLoader(1003, null, this);
//
//        mAdapter = new FolderAdapter(getContext(),R.layout.folder_item,null,new String[]{},new int[]{},0);
//
//
//        mListView.setAdapter(mAdapter);
//        mListView.setOnItemClickListener(new ListViewListener());
//    }

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

//    @Override
//    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
////        bucket_display_name
//        CursorLoader loader = new CursorLoader(getContext(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//               new String[]{MediaStore.Audio.Media._COUNT},
//                MediaStore.Audio.Media.SIZE + ">80000" /* and "+  MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " != 0"*/,
//                null,null);
//        return loader;
//    }

//    @Override
//    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        if(data == null)
//            return ;
//        mCursor = data;
//        mBucketNameIndex = data.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
//        mDisPlayNameIndex = data.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
//        while (mCursor.moveToNext())
//        {
//            for(int i = 0 ; i < mCursor.getColumnCount() ;i++)
//            {
//                Log.d("Cursor","name: " + mCursor.getColumnName(i) + " val: " + mCursor.getString(i));
//            }
//        }
//        mCursor = data;
//
//    }

//    @Override
//    public void onLoaderReset(Loader<Cursor> loader) {
//        if (mAdapter != null)
//            mAdapter.changeCursor(null);
//    }

    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
            intent.putExtra("Id", position);
            intent.putExtra("Type",Utility.FOLDER_HOLDER);
            intent.putExtra("Title",Utility.mFolderList.get(position));
            startActivity(intent);

        }
    }
}
