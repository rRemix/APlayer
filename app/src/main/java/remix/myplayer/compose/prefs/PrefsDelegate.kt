package remix.myplayer.compose.prefs

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefsDelegate<T>(
  private val pref: SharedPreferences,
  private val key: String? = null,
  private val defaultVal: T
) : ReadWriteProperty<Any?, T> {
  private fun key(property: KProperty<*>): String = key ?: property.name

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return when (defaultVal) {
      is Set<*> -> pref.getStringSet(key(property), defaultVal as Set<String>) as T
      is String -> pref.getString(key(property), defaultVal) as T
      is Int -> pref.getInt(key(property), defaultVal) as T
      is Boolean -> pref.getBoolean(key(property), defaultVal) as T
      is Float -> pref.getFloat(key(property), defaultVal) as T
      is Long -> pref.getLong(key(property), defaultVal) as T
      else -> throw IllegalArgumentException("Unsupported type: $defaultVal")
    }
  }

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    pref.edit(commit = true) {
      when (value) {
        is Set<*> -> putStringSet(key(property), value as Set<String>)
        is String -> putString(key(property), value)
        is Int -> putInt(key(property), value)
        is Boolean -> putBoolean(key(property), value)
        is Float -> putFloat(key(property), value)
        is Long -> putLong(key(property), value)
        else -> throw IllegalArgumentException("Unsupported type: $value")
      }
    }
  }

}

fun <T> SharedPreferences.delegate(key: String, defaultVal: T) =
  PrefsDelegate(this, key, defaultVal)

inline fun <reified T> SharedPreferences.delegate(
  defaultVal: T,
  crossinline keyProvider: (KProperty<*>) -> String = { it.name }
) = object : ReadWriteProperty<Any?, T> {
  private var property: KProperty<*>? = null

  private val delegate by lazy {
    val prop = property ?: error("property not initialized")
    delegate(keyProvider(prop), defaultVal)
  }

  override fun getValue(thisRef: Any?, property: KProperty<*>): T {
    this.property = property
    return delegate.getValue(thisRef, property)
  }

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.property = property
    delegate.setValue(thisRef, property, value)
  }

}