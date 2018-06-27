package remix.myplayer.ui.activity.artist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import kotlinx.android.synthetic.main.activity_artistdetail.*
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.adapter.BaseAdapter
import remix.myplayer.adapter.holder.BaseViewHolder
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityArtistdetailBinding
import remix.myplayer.interfaces.OnItemClickListener
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.ui.activity.ToolbarActivity
import remix.myplayer.util.ImageUriUtil
import remix.myplayer.util.Util

class ArtistDetailActivity : ToolbarActivity() {
    companion object {
        const val TAG = "ArtistDetailActivity"
        val IMAGE_SIZE = App.getContext().resources.getDimensionPixelSize(R.dimen.item_album_for_artist_img_size)
    }

    private lateinit var mSongAdapter:SongAdapter
    private lateinit var mAlbumAdapter:AlbumAdapter

    private lateinit var mBinding: ActivityArtistdetailBinding
    private val mViewModel by lazy {
        ViewModelProviders.of(this).get(ArtistViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_artistdetail)
        setUpView()
        observer()
    }

    private fun setUpView() {
        mAlbumAdapter = AlbumAdapter(this,R.layout.item_album_for_artist)
        mAlbumAdapter.setOnItemClickListener(object :OnItemClickListener{
            override fun onItemLongClick(view: View?, position: Int) {
            }

            override fun onItemClick(view: View?, position: Int) {
            }

        })

        mSongAdapter = SongAdapter(this,R.layout.item_song_for_artist)
        mSongAdapter.setOnItemClickListener(object :OnItemClickListener{
            override fun onItemLongClick(view: View?, position: Int) {
            }

            override fun onItemClick(view: View?, position: Int) {
            }

        })

        activity_albums_recyclerView.setHasFixedSize(true)
        activity_albums_recyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        activity_albums_recyclerView.adapter = mAlbumAdapter

        activity_songs_recyclerView.setHasFixedSize(true)
        activity_songs_recyclerView.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        activity_songs_recyclerView.adapter = mSongAdapter

    }

    private fun observer() {
        mViewModel.getArtist().observe(this, Observer {
            setUpToolbar(findViewById(R.id.toolbar),it?.artist)
            activity_artist_name.text = it?.artist

            val height = resources.getDimensionPixelSize(R.dimen.artist_detail_top_img_height)
            LibraryUriRequest(activity_img,
                    ImageUriUtil.getSearchRequest(it),
                    RequestConfig.Builder(resources.displayMetrics.widthPixels,height).build()).loadImage()
        })

        mViewModel.getAlbums().observe(this, Observer {
            mAlbumAdapter.setData(it)
            mAlbumAdapter.notifyDataSetChanged()

            var songCount = 0
            it?.forEach {
                songCount += it.count
            }
            activity_albums_and_songs.text = "${it?.size}专辑 ${songCount}音乐"
        })

        mViewModel.getSongs().observe(this, Observer {
            mSongAdapter.setData(it)
            mSongAdapter.notifyDataSetChanged()
        })

        mViewModel.getArtistIntro().observe(this, Observer {
            activity_artist_intro.text = it?.artist?.bio?.summary
        })

        val artistId = intent.getIntExtra("Id",-1)
        val artistName = intent.getStringExtra("Title")
        assert(artistId > 0)
        mViewModel.setArtistID(artistId)
        mViewModel.setArtistName(artistName)
    }

    internal class AlbumAdapter(context: Context, layoutId: Int) : BaseAdapter<Album, AlbumHolder>(context, layoutId) {

        override fun convert(holder:AlbumHolder, album: Album, position: Int) {
            val disposable = LibraryUriRequest(holder.mImage,
                    ImageUriUtil.getSearchRequest(album),
                    RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).loadImage()
            holder.mAlbum.text = album.album
        }
    }

    internal class AlbumHolder(itemView: View) : BaseViewHolder(itemView) {
        @BindView(R.id.item_img)
        lateinit var mImage: SimpleDraweeView
        @BindView(R.id.item_album)
        lateinit var mAlbum: TextView
    }

    internal class SongAdapter(context: Context, layoutId: Int) : BaseAdapter<Song, SongHolder>(context, layoutId) {

        override fun convert(holder:SongHolder, song: Song, position: Int) {
            holder.mTitle.text = song.Title
            holder.mDuration.text = Util.getTime(song.Duration)
        }
    }

    internal class SongHolder(itemView: View) : BaseViewHolder(itemView) {
        @BindView(R.id.item_song)
        lateinit var mTitle: TextView
        @BindView(R.id.item_duration)
        lateinit var mDuration: TextView
    }
}
