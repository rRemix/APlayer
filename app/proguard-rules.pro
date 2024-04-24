-keepattributes *Annotation*

-keepclasseswithmembers class * implements java.io.Serializable {
    <fields>;
    <methods>;
}

-keep class remix.myplayer.bean.** { *; }
-keepclassmembers class ** {
    @remix.myplayer.misc.handler.OnHandleMessage public *;
}

# bugly
# https://bugly.qq.com/docs/user-guide/instruction-manual-android/
-keep public class com.tencent.bugly.** { *; }
-dontwarn com.tencent.bugly.**

# gson
# https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# jaudiotagger
# Simply keep all classes as they use reflection
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# logback-android
# https://github.com/tony19/logback-android/issues/229
# They've added consumer-rules.pro, but it seems to be unused
-keepclassmembers class ch.qos.logback.classic.pattern.* { <init>(); }
-keepclassmembers class ch.qos.logback.** { *; }
-keepclassmembers class org.slf4j.impl.** { *; }
-dontwarn ch.qos.logback.core.net.*
# The classes used in app/src/main/assets/logback.xml
# We need these rules to avoid ClassNotFoundException
# Why this isn't mentioned in their document?
-keep class ch.qos.logback.classic.android.LogcatAppender
-keep class ch.qos.logback.core.rolling.RollingFileAppender
-keep class ch.qos.logback.core.rolling.TimeBasedRollingPolicy
