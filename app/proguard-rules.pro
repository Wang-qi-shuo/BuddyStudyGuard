# 默认 ProGuard 规则
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Retrofit / Gson 保留
-keep class com.buddy.studyguard.ai.data.remote.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**
