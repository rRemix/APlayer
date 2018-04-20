# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in d:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#指定代码的压缩级别
-optimizationpasses 5
#包明不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
#优化  不优化输入的类文件
-dontoptimize
#预校验
-dontpreverify
#混淆时是否记录日志
-verbose
#混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#保持哪些类不被混淆
#保护注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
#崩溃信息
-keepattributes SourceFile,LineNumberTable

#basedata
-keepclasseswithmembers class * implements java.io.Serializable{
    <fields>;
    <methods>;
}
# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclasseswithmembers class * implements android.os.Parcelable{
    <fields>;
    <methods>;
}
#baseAdapter
-keepclassmembers class remix.myplayer.adapter.holder.BaseViewHolder
-keepclasseswithmembers class remix.myplayer.adapter.holder.BaseViewHolder {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class * extends remix.myplayer.adapter.holder.BaseViewHolder{
    <fields>;
    <methods>;
}

-keep class **.R$* {*;}
-keep public class remix.myplayer.R$*{
public static final int *;
}
-keepclasseswithmembers class * extends android.app.Activity{
    <methods>;
}
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-keep public class * extends java.lang.annotation.Annotation
-keep public class * extends android.os.Handler

-keep public class **.R$*{
   public static final int *;
}

#友盟统计
-keep class com.umeng.update.protobuffer.** {
        public <fields>;
        public <methods>;
}
-keep class com.umeng.update.UmengUpdateAgent {
        public <methods>;
}
-keep public class com.umeng.example.R$*{
    public static final int *;
}
-keepclassmembers class * {
    public <init> (org.json.JSONObject);
}

# fresco
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.app.** { *; }
-dontwarn okio.**
-dontwarn com.squareup.wire.**
-dontwarn com.umeng.update.**
-dontwarn android.support.v4.**
-keep class okio.** {*;}
-keep class com.squareup.wire.** {*;}
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn com.android.volley.toolbox.**
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-dontwarn com.android.volley.toolbox.**
-dontwarn com.facebook.infer.**
-keepclassmembers class * {
    native <methods>;
}
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}
-keepclassmembers class * {
    native <methods>;
}
-keep class com.facebook.imagepipeline.animated.factory.AnimatedFactoryImpl {
    public AnimatedFactoryImpl(com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory,com.facebook.imagepipeline.core.ExecutorSupplier);
}

# bomb
-dontwarn cn.bmob.v3.**
-keep class cn.bmob.v3.** {*;}

# 确保JavaBean不被混淆-否则gson将无法将数据解析成具体对象
-keep class * extends cn.bmob.v3.BmobObject {
    *;
}
-keep class com.example.bmobexample.bean.BankCard{*;}
-keep class com.example.bmobexample.bean.GameScore{*;}
-keep class com.example.bmobexample.bean.MyUser{*;}
-keep class com.example.bmobexample.bean.Person{*;}
-keep class com.example.bmobexample.file.Movie{*;}
-keep class com.example.bmobexample.file.Song{*;}
-keep class com.example.bmobexample.relation.Post{*;}
-keep class com.example.bmobexample.relation.Comment{*;}

# keep okhttp3、okio
-dontwarn javax.annotation.**
-dontwarn okhttp3.**
-keep class okhttp3.** { *;}
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# keep rx
-dontwarn sun.misc.**
-keep class io.reactivex.**{*;}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
 long producerIndex;
 long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
 rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
 rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# FastScrollRecycleView
-keep class com.simplecityapps.recyclerview_fastscroll.** { *; }

#rxpermission
-keep class com.tbruyelle.rxpermissions.**{*;}

#lambda
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*

# 如果你需要兼容6.0系统，请不要混淆org.apache.http.legacy.jar
-dontwarn android.net.compatibility.**
-dontwarn android.net.http.**
-dontwarn com.android.internal.http.multipart.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**
-keep class android.net.compatibility.**{*;}
-keep class android.net.http.**{*;}
-keep class com.android.internal.http.multipart.**{*;}
-keep class org.apache.commons.**{*;}
-keep class org.apache.http.**{*;}

#kotlin
-dontwarn kotlin.**

