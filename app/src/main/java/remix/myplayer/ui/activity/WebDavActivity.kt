package remix.myplayer.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import remix.myplayer.R
import remix.myplayer.databinding.ActivityWebdavBinding
import remix.myplayer.databinding.DialogCreateWebdavBinding
import remix.myplayer.db.room.AppDatabase
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.theme.TextInputLayoutUtil
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.adapter.WebDavAdapter
import remix.myplayer.ui.misc.TextInputEditWatcher
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class WebDavActivity : ToolbarActivity(), CoroutineScope by MainScope() {
  override val coroutineContext: CoroutineContext
    get() = super.coroutineContext

  private lateinit var binding: ActivityWebdavBinding
  private val adapter by lazy {
    WebDavAdapter()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityWebdavBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar(getString(R.string.webdav))

    binding.ivAdd.background = Theme.tintDrawable(
      R.drawable.bg_playlist_add,
      ThemeStore.accentColor
    )
    binding.ivAdd.setImageResource(R.drawable.icon_playlist_add)
    binding.ivAdd.setOnClickListener {
      showWebDavDialog(this@WebDavActivity, null)
    }

    binding.rv.adapter = adapter
    loadWebDav()
  }

  private fun loadWebDav() {
    launch(Dispatchers.IO) {
      AppDatabase.getInstance(applicationContext).webDavDao().queryAll()
//        .distinctUntilChanged()
        .collect {
          withContext(Dispatchers.Main) {
            adapter.setWebDavList(it)
          }
        }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  companion object {
    fun showWebDavDialog(
      activity: WebDavActivity,
      webDav: WebDav? = null
    ) {
      val binding = DialogCreateWebdavBinding.inflate(LayoutInflater.from(activity))
      val dialog = Theme.getBaseDialog(activity)
        .title(R.string.webdav)
        .customView(binding.root, true)
        .negativeText(R.string.cancel)
        .positiveText(R.string.confirm)
        .onPositive { dialog, which ->
          submit(activity, binding, webDav)
        }
        .build()

      val textInputTintColor = ThemeStore.accentColor
      val editTintColor = ThemeStore.accentColor

      TextInputLayoutUtil.setAccent(binding.aliasLayout, textInputTintColor)
      TintHelper.setTintAuto(binding.aliasLayout.editText!!, editTintColor, false)
      binding.aliasLayout.editText?.setText(webDav?.alias)
      binding.aliasLayout.editText?.addTextChangedListener(
        TextInputEditWatcher(
          binding.aliasLayout,
          activity.getString(R.string.can_t_be_empty, activity.getString(R.string.alias))
        )
      )

      TextInputLayoutUtil.setAccent(binding.accountLayout, textInputTintColor)
      TintHelper.setTintAuto(binding.accountLayout.editText!!, editTintColor, false)
      binding.accountLayout.editText?.setText(webDav?.account)
      binding.accountLayout.editText?.addTextChangedListener(
        TextInputEditWatcher(
          binding.accountLayout,
          activity.getString(R.string.can_t_be_empty, activity.getString(R.string.account))
        )
      )

      TextInputLayoutUtil.setAccent(binding.pwdLayout, textInputTintColor)
      TintHelper.setTintAuto(binding.pwdLayout.editText!!, editTintColor, false)
      binding.pwdLayout.editText?.setText(webDav?.pwd)
      binding.pwdLayout.editText?.addTextChangedListener(
        TextInputEditWatcher(
          binding.pwdLayout,
          activity.getString(R.string.can_t_be_empty, activity.getString(R.string.pwd))
        )
      )

      TextInputLayoutUtil.setAccent(binding.serverLayout, textInputTintColor)
      TintHelper.setTintAuto(binding.serverLayout.editText!!, editTintColor, false)
      binding.serverLayout.hint =
        activity.getString(R.string.webdav_hint_server) + " eg: https://dav.example.com"
      binding.serverLayout.editText?.setText(webDav?.server)
      binding.serverLayout.editText?.setOnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          submit(activity, binding, webDav)
          return@setOnEditorActionListener true
        }
        false
      }
      binding.serverLayout.editText?.addTextChangedListener(
        TextInputEditWatcher(
          binding.serverLayout,
          activity.getString(
            R.string.can_t_be_empty,
            activity.getString(R.string.webdav_hint_server)
          )
        )
      )

//      TextInputLayoutUtil.setAccent(binding.pathLayout, textInputTintColor)
//      TintHelper.setTintAuto(binding.pathLayout.editText!!, editTintColor, false)
//      binding.pathLayout.hint = activity.getString(R.string.webdav_hint_path) + " eg: /path1/path2"
//      binding.pathLayout.editText?.setText(webDav?.initialPath)
//      binding.pathLayout.editText?.setOnEditorActionListener { v, actionId, event ->
//        if (actionId == EditorInfo.IME_ACTION_DONE) {
//          submit(activity, binding, webDav)
//          return@setOnEditorActionListener true
//        }
//        false
//      }

      dialog.show()
    }

    private fun submit(
      activity: WebDavActivity,
      binding: DialogCreateWebdavBinding,
      webDav: WebDav? = null
    ) {
      val alias = binding.aliasLayout.editText?.text?.toString()
      if (alias.isNullOrEmpty()) {
        ToastUtil.show(activity, R.string.can_t_be_empty, activity.getString(R.string.alias))
        return
      }
      val server = binding.serverLayout.editText?.text?.toString()?.removeSuffix("/")
      if (server.isNullOrEmpty()) {
        ToastUtil.show(
          activity,
          R.string.can_t_be_empty,
          activity.getString(R.string.webdav_hint_server)
        )
        return
      }
      val account = binding.accountLayout.editText?.text?.toString()
      if (account.isNullOrEmpty()) {
        ToastUtil.show(activity, R.string.can_t_be_empty, activity.getString(R.string.account))
        return
      }
      val pwd = binding.pwdLayout.editText?.text?.toString()
      if (pwd.isNullOrEmpty()) {
        ToastUtil.show(activity, R.string.can_t_be_empty, activity.getString(R.string.pwd))
        return
      }

//      val initialPath = binding.pathLayout.editText?.text?.toString() ?: "/"
      if (webDav == null) {
        insertOrReplaceWebDav(activity, WebDav(alias, account, pwd, server))
      } else {
        webDav.alias = alias
        webDav.server = server
//        webDav.initialPath = initialPath
        webDav.account = account
        webDav.pwd = pwd
        webDav.lastPath = ""
        insertOrReplaceWebDav(activity, webDav)
      }
    }

    private fun insertOrReplaceWebDav(activity: WebDavActivity, webdav: WebDav) {
      activity.showLoading()
      activity.launch {
        val sardine = OkHttpSardine()
        sardine.setCredentials(webdav.account, webdav.pwd)
        try {
          val davResources = withContext(Dispatchers.IO) {
            sardine.list(webdav.root())
          }
          if (davResources.isNotEmpty()) {
            AppDatabase.getInstance(activity.applicationContext).webDavDao().insertOrReplace(webdav)
          }
        } catch (e: Exception) {
          Timber.e(e)
          ToastUtil.show(activity, e.toString())
        } finally {
          activity.dismissLoading()
        }
      }
    }
  }
}