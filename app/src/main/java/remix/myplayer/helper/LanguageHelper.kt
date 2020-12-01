package remix.myplayer.helper

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import java.util.*

object LanguageHelper {
  const val AUTO = 0
  private const val CHINESE_SIMPLE = 1
  private const val CHINESE_TRADITIONAL = 2
  private const val ENGLISH = 3
  var current = -1

  private var sLocal: Locale = Locale.getDefault()

  private val TAG = "LanguageHelper"

  /**
   * 获取选择的语言设置
   */
  private fun selectLanguageLocale(context: Context): Locale? {
    if (current == -1) {
      current = SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO)
    }
    return when (current) {
      AUTO -> sLocal
      CHINESE_SIMPLE -> Locale.SIMPLIFIED_CHINESE
      ENGLISH -> Locale.ENGLISH
      CHINESE_TRADITIONAL -> Locale.TRADITIONAL_CHINESE
      else -> sLocal
    }
  }

  @JvmStatic
  fun saveSelectLanguage(context: Context, select: Int) {
    SPUtil.putValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, select)
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
}
