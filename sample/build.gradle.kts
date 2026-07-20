import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Read AdMob / AppLovin keys from root local.properties (gitignored).
// Keys:
//   admob.app.id=ca-app-pub-...
//   applovin.sdk.key=...
//   admob.test.device.id=YOUR_HASHED_DEVICE_ID   # optional, from Logcat after first ad request
//   applovin.ad.unit.banner=YOUR_MAX_BANNER_AD_UNIT_ID
//   applovin.ad.unit.rewarded=YOUR_MAX_REWARDED_AD_UNIT_ID
//   applovin.ad.unit.splash=YOUR_MAX_APP_OPEN_AD_UNIT_ID
//   applovin.ad.unit.interstitial=YOUR_MAX_INTERSTITIAL_AD_UNIT_ID
//   applovin.ad.unit.native=YOUR_MAX_NATIVE_AD_UNIT_ID
//   applovin.ad.unit.mrec=YOUR_MAX_MREC_AD_UNIT_ID
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { stream -> load(stream) }
    }
}

fun localProp(key: String, default: String): String =
    localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: default

// Defaults keep CI / first clone buildable; override in local.properties for real keys.
val admobAppId = localProp(
    "admob.app.id",
    "ca-app-pub-3940256099942544~3347511713",
)
val applovinSdkKey = localProp(
    "applovin.sdk.key",
    "05TMDQ5tZabpXQ45_UTbmEGNUtVAzSTzT6KmWQc5_CuWdzccS4DCITZoL3yIWUG3bbq60QC_d4WF28tUC4gVTF",
)
val admobTestDeviceId = localProperties.getProperty("admob.test.device.id")?.trim().orEmpty()
val applovinBannerUnit = localProperties.getProperty("applovin.ad.unit.banner")?.trim().orEmpty()
val applovinRewardedUnit = localProperties.getProperty("applovin.ad.unit.rewarded")?.trim().orEmpty()
val applovinSplashUnit = localProperties.getProperty("applovin.ad.unit.splash")?.trim().orEmpty()
val applovinInterstitialUnit = localProperties.getProperty("applovin.ad.unit.interstitial")?.trim().orEmpty()
val applovinNativeUnit = localProperties.getProperty("applovin.ad.unit.native")?.trim().orEmpty()
val applovinMrecUnit = localProperties.getProperty("applovin.ad.unit.mrec")?.trim().orEmpty()

android {
    namespace = "com.kit.ads.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kit.ads.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = libs.versions.sdkVersion.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
        manifestPlaceholders["APPLOVIN_SDK_KEY"] = applovinSdkKey

        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "APPLOVIN_SDK_KEY", "\"$applovinSdkKey\"")
        buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"$admobTestDeviceId\"")
        buildConfigField("String", "APPLOVIN_BANNER_UNIT", "\"$applovinBannerUnit\"")
        buildConfigField("String", "APPLOVIN_REWARDED_UNIT", "\"$applovinRewardedUnit\"")
        buildConfigField("String", "APPLOVIN_SPLASH_UNIT", "\"$applovinSplashUnit\"")
        buildConfigField("String", "APPLOVIN_INTERSTITIAL_UNIT", "\"$applovinInterstitialUnit\"")
        buildConfigField("String", "APPLOVIN_NATIVE_UNIT", "\"$applovinNativeUnit\"")
        buildConfigField("String", "APPLOVIN_MREC_UNIT", "\"$applovinMrecUnit\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling.preview)

    implementation(project(":ads"))

    // 测试线上 aar（本地开发请使用 project(":ads")）
//     implementation("com.github.e-hai:AdsKit:v1.0.0")
}
