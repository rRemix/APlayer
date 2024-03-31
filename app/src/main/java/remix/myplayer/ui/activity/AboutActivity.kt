package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.databinding.ActivityAboutBinding
import remix.myplayer.misc.AppInfo
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper

class AboutActivity : ToolbarActivity() {
  private lateinit var binding: ActivityAboutBinding

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAboutBinding.inflate(layoutInflater)
    setContentView(binding.root)

    binding.aboutText.text = "v${BuildConfig.VERSION_NAME}"
    binding.aboutText.setOnLongClickListener {
      Theme.getBaseDialog(this)
        .content(AppInfo.prettyPrinted)
        .positiveText(R.string.close)
        .build()
        .run {
          with(contentView ?: return@setOnLongClickListener false) {
            TintHelper.setTint(this, ThemeStore.accentColor, false)
            setTextIsSelectable(true)
          }
          show()
        }
      true
    }

    setUpToolbar(getString(R.string.about))
  }
}