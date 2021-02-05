package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.databinding.ActivityAboutBinding

class AboutActivity : ToolbarActivity() {
  private lateinit var binding: ActivityAboutBinding

  @SuppressLint("SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAboutBinding.inflate(layoutInflater)
    setContentView(binding.root)
    try {
      val pm = App.getContext().packageManager
      val pi = pm.getPackageInfo(
        App.getContext().packageName, PackageManager.GET_ACTIVITIES
      )
      binding.aboutText.text = "v" + pi.versionName
    } catch (ignored: Exception) {
    }
    setUpToolbar(getString(R.string.about))
  }
}