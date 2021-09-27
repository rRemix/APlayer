package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.databinding.ActivityAboutBinding

class AboutActivity : ToolbarActivity() {
  private lateinit var binding: ActivityAboutBinding

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAboutBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.aboutText.text =
        "v${BuildConfig.VERSION_NAME}" +
            " (${BuildConfig.VERSION_CODE})" +
            " (${BuildConfig.FLAVOR})"
    setUpToolbar(getString(R.string.about))
  }
}