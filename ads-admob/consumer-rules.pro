# AdsKit-admob — keep SPI registrar so ServiceLoader works under R8
-keep class com.kit.ads.provider.admob.AdmobProviderAdapterRegistrar { <init>(); }
-keep class com.kit.ads.provider.AdsProviderAdapterRegistrar
-keep class * implements com.kit.ads.provider.AdsProviderAdapterRegistrar { <init>(); }
-keepclassmembers class * implements com.kit.ads.provider.AdsProviderAdapterRegistrar {
    public <init>();
}
-keepnames class com.kit.ads.provider.admob.**
