package remix.myplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityWebdavDetailBinding
import remix.myplayer.db.room.AppDatabase
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.misc.isAudio
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.adapter.OnItemClickListener
import remix.myplayer.ui.adapter.WebDavDetailAdapter
import remix.myplayer.util.MusicUtil
import timber.log.Timber

class WebDavDetailActivity : MenuActivity(), SwipeRefreshLayout.OnRefreshListener {
  private val webdav by lazy {
    intent.getSerializableExtra(EXTRA_WEBDAV) as WebDav
  }
  private val sardine by lazy {
    OkHttpSardine().apply {
      setCredentials(webdav.account, webdav.pwd)
    }
  }
  private var currentUrl: String = ""
  private val adapter by lazy {
    WebDavDetailAdapter(webdav).apply {
      onItemClickListener = object : OnItemClickListener<DavResource> {
        override fun onItemClick(view: View, resource: DavResource, position: Int) {
          if (resource.isDirectory) {
            reload(resource.path)
          } else {
            val resources = getWebDavResources()
            if (resources.isEmpty()) {
              return
            }
            var select: Song.Remote? = null
            val remotes = resources
              .filter { it.isAudio() }
              .map {
                val remote = Song.Remote(
                  title = it.name.substringBeforeLast('.'),
                  data = webdav.base().plus(it.path),
                  size = it.contentLength,
                  dateModified = it.creation?.time ?: 0,
                  account = webdav.account ?: "",
                  pwd = webdav.pwd ?: ""
                )
                if (it == resource) {
                  select = remote
                }
                remote
              }
            MusicServiceRemote.setPlayQueue(
              remotes,
              MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, remotes.indexOfFirst {
                  it.data == select?.data
                })
            )
          }
        }

        override fun onItemLongClick(view: View, data: DavResource, position: Int) {
        }

      }
    }
  }
  private lateinit var binding: ActivityWebdavDetailBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityWebdavDetailBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setUpToolbar(webdav.alias)

    binding.refresh.setColorSchemeColors(ThemeStore.materialPrimaryColor)
    binding.refresh.setOnRefreshListener(this)
    binding.rv.adapter = adapter

    fetchWebDav(webdav.last())
  }

  override fun onRefresh() {
    reload(webdav.lastPath ?: return)
    binding.refresh.isRefreshing = false
  }

  override fun getMenuLayoutId(): Int {
    return R.menu.menu_webdav_detail
  }

  override fun onClickNavigation() {
    if (webdav.isRoot(currentUrl)) {
      super.onClickNavigation()
      return
    }

    var url = currentUrl.removeSuffix("/")
    url = url.substring(0, url.lastIndexOf('/'))

    if (url == webdav.root()) {
      reload(webdav.initialPath())
    } else {
      reload(url.replace(webdav.root(), ""))
    }
  }

  override fun onBackPressed() {
    onClickNavigation()
  }

  private fun reload(path: String, showLoading: Boolean = true) {
    fetchWebDav(webdav.base().plus(path), showLoading)
    webdav.lastPath = path
    launch {
      AppDatabase.getInstance(applicationContext).webDavDao().insertOrReplace(webdav)
    }
  }

  private fun fetchWebDav(url: String, showLoading: Boolean = true) {
    currentUrl = url.removeSuffix("/")

    if (showLoading) {
      showLoading()
    }
    launch {
      try {
        val davResources = withContext(Dispatchers.IO) {
          sardine.list(url)
        }
        if (davResources.isNullOrEmpty()) {
          return@launch
        }
        adapter.setWebDavResources(davResources.takeLast(davResources.size - 1))
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        if (showLoading) {
          dismissLoading()
        }
      }
    }
  }

  companion object {
    private const val EXTRA_WEBDAV = "extra_webdav"
    fun start(context: Context, webDav: WebDav) {
      context.startActivity(
        Intent(context, WebDavDetailActivity::class.java)
          .putExtra(EXTRA_WEBDAV, webDav)
      )
    }
  }
}