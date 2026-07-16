-keepattributes *Annotation*

-keepclasseswithmembers class * implements java.io.Serializable {
    <fields>;
    <methods>;
}

-keep class remix.myplayer.data.model.** { *; }

# bugly
# https://bugly.qq.com/docs/user-guide/instruction-manual-android/
-keep public class com.tencent.bugly.** { *; }
-dontwarn com.tencent.bugly.**

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

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowoptimization,allowshrinking,allowobfuscation class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept.
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowoptimization,allowshrinking,allowobfuscation class retrofit2.Response

# prfofile
-keep class androidx.profileinstaller.ProfileInstallReceiver { *; }
-keep class androidx.profileinstaller.ProfileInstallerInitializer { *; }
-keep class androidx.profileinstaller.** { *; }
-keep class androidx.startup.AppInitializer { *; }
-keep class androidx.startup.InitializationProvider { *; }
-keep class androidx.startup.Initializer { *; }

# smbj
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn net.engio.mbassy.**
-dontwarn javax.el.**

-keepclassmembers class * {
    @net.engio.mbassy.listener.Handler <methods>;
}

-keep class net.engio.mbassy.dispatch.HandlerInvocation { *; }
-keep class net.engio.mbassy.dispatch.ReflectiveHandlerInvocation { *; }
-keep class net.engio.mbassy.subscription.SubscriptionContext { *; }
-keepclassmembers class * extends net.engio.mbassy.dispatch.HandlerInvocation {
    <init>(net.engio.mbassy.subscription.SubscriptionContext);
}

-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }

# 修复Android5.0 VerifyError
-keepclassmembers class androidx.compose.ui.platform.** { *; }
