package remix.myplayer.helper

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import remix.myplayer.data.prefs.PrefKeys
import java.util.Locale

object LanguageHelper {

  const val AUTO = 0
  private const val CHINESE_SIMPLE = 1
  private const val CHINESE_TRADITIONAL = 2
  private const val ENGLISH = 3
  private const val JAPANESE = 4
  var current = -1

  private var sLocal: Locale = Locale.getDefault()

  private val TAG = "LanguageHelper"

  private fun sp(context: Context) =
    context.getSharedPreferences(PrefKeys.Setting.NAME, Context.MODE_PRIVATE)

  private fun readLanguage(context: Context): Int =
    sp(context).getInt(PrefKeys.Setting.LANGUAGE, AUTO)

  private fun writeLanguage(context: Context, select: Int) {
    sp(context).edit(commit = true) {
      putInt(
        PrefKeys.Setting.LANGUAGE,
        select
      )
    }
  }

  /**
   * 获取选择的语言设置
   */
  private fun selectLanguageLocale(context: Context): Locale? {
    if (current == -1) {
      current = readLanguage(context)
    }
    return when (current) {
      AUTO -> sLocal
      CHINESE_SIMPLE -> Locale.SIMPLIFIED_CHINESE
      ENGLISH -> Locale.ENGLISH
      CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
      JAPANESE -> Locale.JAPANESE
      else -> sLocal
    }
  }

  @JvmStatic
  fun saveSelectLanguage(context: Context, select: Int) {
    writeLanguage(context, select)
    current = select
    setApplicationLanguage(context)
  }

  @JvmStatic
  fun setLocal(context: Context): Context {
    return updateResources(context, selectLanguageLocale(context))
  }

  private fun updateResources(context: Context, locale: Locale?): Context {
    Locale.setDefault(locale)

    val res = context.resources
    val config = Configuration(res.configuration)
    return if (Build.VERSION.SDK_INT >= 17) {
      config.setLocale(locale)
      context.createConfigurationContext(config)
    } else {
      config.locale = locale
      res.updateConfiguration(config, res.displayMetrics)
      context
    }
  }

  /**
   * 设置语言类型
   */
  @JvmStatic
  fun setApplicationLanguage(context: Context) {
    val resources = context.applicationContext.resources
    val dm = resources.displayMetrics
    val config = resources.configuration
    val locale = selectLanguageLocale(context)
    config.locale = locale
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val localeList = LocaleList(locale ?: return)
      LocaleList.setDefault(localeList)
      config.setLocales(localeList)
      context.applicationContext.createConfigurationContext(config)
      Locale.setDefault(locale)
    }
    resources.updateConfiguration(config, dm)
  }

  @JvmStatic
  fun saveSystemCurrentLanguage() {
    val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      LocaleList.getDefault().get(0)
    } else {
      Locale.getDefault()
    }
    sLocal = locale
  }

  @JvmStatic
  fun onConfigurationChanged(context: Context) {
    saveSystemCurrentLanguage()
    setLocal(context)
    setApplicationLanguage(context)
  }

  fun isChinese(): Boolean {
    val lang = if (current == -1) AUTO else current
    return when (lang) {
      CHINESE_SIMPLE, CHINESE_TRADITIONAL -> {
        true
      }

      AUTO -> {
        sLocal.language == "zh"
      }

      else -> {
        false
      }
    }
  }
}
