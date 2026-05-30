# =============================================================================
# Contact Manager - ProGuard / R8 rules
# Adapted from dialer app; scoped to com.phonecall.dialcontacts.calldialer
# =============================================================================

# --- Debug stack traces (Crashlytics / Play Console) ---
-renamesourcefileattribute SourceFile
-keepattributes SourceFile, LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# --- General Android components (manifest + reflection) ---
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class * extends java.lang.Exception

# --- Obfuscation ---
# Do not repackage into the app package (R8: duplicate / merge errors with app classes).
-repackageclasses 'obf'

# --- Strip Log calls in release (safe with proguard-android.txt) ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** w(...);
    public static *** i(...);
}

# =============================================================================
# App-specific: call / telecom / call-end flow
# =============================================================================
-keep class com.phonecall.dialcontacts.calldialer.services.** { *; }
-keep class com.phonecall.dialcontacts.calldialer.receivers.** { *; }
-keep class com.phonecall.dialcontacts.calldialer.callEndUtils.** { *; }
-keep class com.phonecall.dialcontacts.calldialer.utils.NewCallManager { *; }
-keep class com.phonecall.dialcontacts.calldialer.utils.NewCallManager$* { *; }
-keep class com.phonecall.dialcontacts.calldialer.utils.CallNotificationManager { *; }
-keep class com.phonecall.dialcontacts.calldialer.activities.call.CallActivity { *; }
-keep class com.phonecall.dialcontacts.calldialer.activities.endCall.** { *; }

# Advertisement / remote config (Gson TypeToken, WorkManager)
-keep class com.phonecall.dialcontacts.calldialer.Advertisement.** { *; }

# AES / Jersey Base64 (remote config decryption)
-keep class com.phonecall.dialcontacts.calldialer.utils.AESHelper { *; }
-keep class com.phonecall.dialcontacts.calldialer.utils.ManegeParameter { *; }
-keep class com.phonecall.dialcontacts.calldialer.utils.ManegeUtilsView { *; }
-dontwarn com.sun.jersey.**
-keep class com.sun.jersey.** { *; }

# Room database entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class com.phonecall.dialcontacts.calldialer.database.** { *; }
-keep class com.phonecall.dialcontacts.calldialer.models.** { *; }

# Data binding generated classes
-keep class com.phonecall.dialcontacts.calldialer.databinding.** { *; }
-keep class * extends androidx.databinding.ViewDataBinding {
    public static *** inflate(...);
    public static *** bind(...);
}
-keep class androidx.databinding.** { *; }

# =============================================================================
# Kotlin
# =============================================================================
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# =============================================================================
# Dagger Hilt
# =============================================================================
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
    @dagger.hilt.* <fields>;
}
-keep class com.phonecall.dialcontacts.calldialer.di.** { *; }
-keep class com.phonecall.dialcontacts.calldialer.viewmodels.** { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_GeneratedInjector { *; }

# =============================================================================
# UI sizing (sdp)
# =============================================================================
-keep class com.intuit.sdp.** { *; }

# =============================================================================
# Glide
# =============================================================================
-dontwarn com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
-dontwarn com.bumptech.glide.load.resource.bitmap.Downsampler
-dontwarn com.bumptech.glide.load.resource.bitmap.HardwareConfigState
-dontwarn com.bumptech.glide.manager.RequestManagerRetriever

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# =============================================================================
# Gson
# https://github.com/google/gson/blob/main/gson/src/main/resources/META-INF/proguard/gson.pro
# =============================================================================
-if class com.google.gson.reflect.TypeToken
-keep,allowobfuscation class com.google.gson.reflect.TypeToken

-keep,allowobfuscation class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowoptimization @com.google.gson.annotations.JsonAdapter class *

-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.Expose <fields>;
    @com.google.gson.annotations.JsonAdapter <fields>;
    @com.google.gson.annotations.Since <fields>;
    @com.google.gson.annotations.Until <fields>;
}

-keepclassmembers class * extends com.google.gson.TypeAdapter {
    <init>();
}
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory {
    <init>();
}
-keepclassmembers class * implements com.google.gson.JsonSerializer {
    <init>();
}
-keepclassmembers class * implements com.google.gson.JsonDeserializer {
    <init>();
}

-if class *
-keepclasseswithmembers,allowobfuscation class <1> {
    @com.google.gson.annotations.SerializedName <fields>;
}
-if class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers,allowobfuscation,allowoptimization class <1> {
    <init>();
}

# =============================================================================
# Lottie
# =============================================================================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# =============================================================================
# UCrop
# =============================================================================
-dontwarn com.yalantis.ucrop.**
-keep class com.yalantis.ucrop.** { *; }

# =============================================================================
# Firebase / Google Play Services / Ads
# =============================================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# =============================================================================
# Facebook SDK / Audience Network / Shimmer
# =============================================================================
-keep class com.facebook.** { *; }
-dontwarn com.facebook.**

# =============================================================================
# PostHog analytics
# =============================================================================
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# =============================================================================
# WorkManager
# =============================================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# =============================================================================
# Play In-App Update
# =============================================================================
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# =============================================================================
# Misc warnings (Java 9+ / unused transitive deps)
# =============================================================================
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.swing.tree.TreeNode
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Jacoco (sometimes bundled with ad mediation SDKs; not available on Android)
-dontwarn org.jacoco.**
-dontwarn java.lang.instrument.**
