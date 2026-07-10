# AdsKit SDK - Consumer ProGuard/R8 Rules
# Embedded in the AAR and applied when the host app runs minification (R8/ProGuard).
# Third-party ad SDKs ship their own consumer rules via transitive AARs — do not duplicate broad keeps here.

# === Public API ===
-keep class com.kit.ads.AdsManager { *; }
-keep class com.kit.ads.AdsEntity { *; }
-keep class com.kit.ads.AdsRequest { *; }
-keep class com.kit.ads.AdsType { *; }
-keep class com.kit.ads.AdsListener { *; }
-keep class com.kit.ads.AdCallback { *; }
-keep class com.kit.ads.ump.UMP { *; }
-keep class com.kit.ads.AdsInitState { *; }
-keep class com.kit.ads.AdsPaidEvent { *; }
-keep class com.kit.ads.provider.AdsProviderConfig { *; }
-keep class com.kit.ads.provider.AdsProviderType { *; }
-keep class com.kit.ads.provider.AdsProviderAdapterRegistrar { *; }

# Host app callback implementations
-keep class * extends com.kit.ads.AdCallback { *; }
-keep class * implements com.kit.ads.AdsListener { *; }

# SPI: ServiceLoader needs registrar implementations + META-INF/services entries
-keep class * implements com.kit.ads.provider.AdsProviderAdapterRegistrar { <init>(); }
-keepclassmembers class * implements com.kit.ads.provider.AdsProviderAdapterRegistrar {
    public <init>();
}

# Enum constants used across the public API
-keepclassmembers enum com.kit.ads.AdsType { *; }
-keepclassmembers enum com.kit.ads.AdsInitState { *; }
-keepclassmembers enum com.kit.ads.provider.AdsProviderType { *; }

# BuildConfig.DEBUG and AdsDebug.isEnabled are used for debug behavior
-keep class com.kit.ads.BuildConfig { *; }
-keep class com.kit.ads.AdsDebug { *; }
