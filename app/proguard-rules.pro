# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* *;
}
-keepclassmembers class * {
    @com.squareup.moshi.JsonQualifier <methods>;
}
-keepclassmembers class * extends java.lang.Enum {
    @com.squareup.moshi.JsonClass <fields>;
    **[] values();
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Your app models
-keep class com.milywita.bookapp.data.model.** { *; }
-keep class com.milywita.bookapp.domain.model.** { *; }
-keep class com.milywita.bookapp.data.model.firebase.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keep class androidx.test.core.app.InstrumentationActivityInvoker$* { *; }
-dontwarn androidx.test.core.app.InstrumentationActivityInvoker$*
