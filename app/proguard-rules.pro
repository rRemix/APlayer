# bugly
# https://bugly.qq.com/docs/user-guide/instruction-manual-android/
-keep public class com.tencent.bugly.** { *; }
-dontwarn com.tencent.bugly.**

# FOSS/Google disable updates, so R8 can remove all GithubApi methods and Retrofit's
# conditional keep rule no longer matches. Keep the dynamically implemented interface
# to preserve Retrofit.create() casts in any remaining injection/worker entry points.
-keep,allowobfuscation interface remix.myplayer.request.network.GithubApi

# smbj
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn net.engio.mbassy.**
-dontwarn javax.el.**
-keepclassmembers,allowshrinking,allowobfuscation class com.hierynomus.msdfsc.ReferralCache$ReferralCacheNode {
    static final java.util.concurrent.atomic.AtomicReferenceFieldUpdater ENTRY_UPDATER;
}

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
