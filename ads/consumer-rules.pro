# AdsKit SDK - Consumer ProGuard/R8 Rules
# These rules are embedded in the AAR and automatically applied
# when the host app runs minification (R8/ProGuard).

# === AdsKit Public API ===
# Keep all public API classes so the host app can call them by name.
-keep class com.kit.ads.** { *; }

# === AdMob / Google Play Services ===
-keep class com.google.android.gms.** { *; }
-keep class com.google.ads.mediation.** { *; }

# === AppLovin ===
-keep class com.applovin.** { *; }
-keep class com.amazon.** { *; }

# === Kotlin Coroutines (used by internal SDK) ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
