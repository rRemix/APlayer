# bugly
# https://bugly.qq.com/docs/user-guide/instruction-manual-android/
-keep public class com.tencent.bugly.** { *; }
-dontwarn com.tencent.bugly.**

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
