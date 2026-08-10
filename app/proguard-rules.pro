-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-dontwarn javax.script.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class com.makkispacejam.fluxa.data.local.** { *; }

# NewPipe Extractor
-keep class org.schabi.newpipe.extractor.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Mozilla Rhino
-dontwarn java.beans.**
-dontwarn javax.script.**
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.**

# App classes
-keep class com.makkispacejam.fluxa.data.** { *; }
-keep class com.makkispacejam.fluxa.video.** { *; }
-keep class com.makkispacejam.fluxa.viewmodels.** { *; }
-keep class com.makkispacejam.fluxa.ui.components.** { *; }
-keep class com.makkispacejam.fluxa.ui.screens.** { *; }
-keep class com.makkispacejam.fluxa.utils.** { *; }

# Application & Activity
-keep class com.makkispacejam.fluxa.MainActivity { *; }
-keep class com.makkispacejam.fluxa.FluxaApplication { *; }
-keep class com.makkispacejam.fluxa.FluxaPlaybackService { *; }

# Compose
-keep class androidx.compose.** { *; }

# Coil
-keep class coil.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Google Guava (ListenableFuture)
-keep class com.google.common.util.concurrent.** { *; }

# Google ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.android.gms.internal.mlkit_translation_** { *; }
-keep class com.google.android.gms.internal.mlkit_languageid_** { *; }
-keep class com.google.android.gms.mlkit.** { *; }
-keep class com.google.mlkit.nl.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common_** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode_** { *; }
-dontwarn com.google.android.gms.internal.mlkit_vision_**
-dontwarn com.google.android.gms.internal.mlkit_translation_**
-dontwarn com.google.android.gms.internal.mlkit_languageid_**

# Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
