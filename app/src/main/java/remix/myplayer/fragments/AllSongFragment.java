package remix.myplayer.fragments;


import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.adapters.SongListAdapter;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/11/30.
 */
public class AllSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private LoaderManager mManager;
    private SongListAdapter mAdapter;
    private MusicService.PlayerReceiver receiver;
    private int mPrev = -1;
    private Cursor mCursor = null;
    private ListView mListView = null;
    public static int mDisPlayNameIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1000, null, this);
        mAdapter = new SongListAdapter(getContext(),R.layout.allsong_item,null,new String[]{},new int[]{},0);
//        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE || scrollState == SCROLL_STATE_FLING) {
//                    mAdapter.setScrollState(false);
//                    int count = view.getChildCount();
//                    for(int i = 0 ; i < count ;i++)
//                    {
//                        ImageView imageView = (ImageView)view.getChildAt(i).findViewById(R.id.homepage_head_image);
//                        if(imageView.getTag() == null) continue;
//                        if(!imageView.getTag().equals(""))
//                        {
//                            AsynLoadImage task = new AsynLoadImage(imageView);
//                            int albumid = Integer.valueOf(imageView.getTag().toString());
//                            task.execute(albumid);
//                        }
//                    }
//                }
//                else
//                    mAdapter.setScrollState(true);
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//
//            }
//        });
        mListView.setAdapter(mAdapter);
    }

    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final WeakReference mImageView;
        //        private ImageView mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = new WeakReference(imageView);
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return Utility.CheckBitmapByAlbumId(params[0],true);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                ((ImageView)mImageView.get()).setImageBitmap(bitmap);
            else
                ((ImageView)mImageView.get()).setImageResource(R.drawable.default_recommend);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //注册broadcastreceiver
        MusicService service = new MusicService(getContext());
        receiver = service.new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Utility.CTL_ACTION);
        getContext().registerReceiver(receiver, filter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().unregisterReceiver(receiver);
        if(mAdapter != null)
            mAdapter.changeCursor(null);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        final View rootView = inflater.inflate(R.layout.allsong_list,null);
        mListView = (ListView)rootView.findViewById(R.id.list);
        mListView.setOnItemClickListener(new ListViewListener());
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getContext(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,MediaStore.Audio.Media.SIZE + ">80000",null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        return  loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        mCursor = data;
        mDisPlayNameIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        mArtistIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        mAlbumIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        mSongId = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        mAlbumIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        if(mCursor != null)
        {
            mAdapter.changeCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
    }

    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(Utility.CTL_ACTION);
            Bundle arg = new Bundle();
            arg.putInt("Control", Utility.PLAYSELECTEDSONG);
            arg.putInt("Position", position);
            intent.putExtras(arg);
            getContext().sendBroadcast(intent);
            Utility.mPlayList = Utility.mAllSongList;
            MainActivity.mInstance.getService().UpdateNextSong(position);

            //将当前选中的歌曲的歌曲名设置为红色
//            TextView title = (TextView)view.findViewById(R.id.displayname);
//            title.setTextColor(Color.RED);
//            //取消上次选中的红色
//            if(mPrev != -1)
//            {
//                adapter = (SongListAdapter)parent.getAdapter();
//                TextView prevtitle = (TextView)parent.getAdapter().getView(mPrev,view,null).findViewById(R.id.displayname);
//                prevtitle.setBackgroundColor(Color.BLACK);
//                adapter.notifyDataSetChanged();
//                System.out.println(prevtitle.getText().toString());
//            }
//            mPrev = position;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case 1:
                Toast.makeText(getContext(),"单击了测试1",Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(getContext(),"单击了测试2",Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(getContext(),"单击了测试3",Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }
}
