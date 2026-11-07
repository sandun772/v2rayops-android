# Proguard rules for V2RayOps Android Client
-keep class com.v2rayops.client.data.api.dto.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
