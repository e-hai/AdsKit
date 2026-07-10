pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        // Admob Mediation start
        maven { url = uri("https://imobile.github.io/adnw-sdk-android") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven { url = uri("https://imobile-maio.github.io/maven") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle/") }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        // Admob Mediation end

        // Applovin Mediation start
        // 屏蔽一些跟 admob 重复的
        // maven { url = uri("https://android-sdk.is.com") }
        // maven { url = uri("https://imobile-maio.github.io/maven") }
        // maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        // maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        // maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        maven { url = uri("https://artifacts.applovin.com/android") }
        maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
        maven { url = uri("https://maven.ogury.co") }
        maven { url = uri("https://s3.amazonaws.com/smaato-sdk-releases/") }
        maven { url = uri("https://verve.jfrog.io/artifactory/verve-gradle-release") }
        // Applovin Mediation end
    }
}

rootProject.name = "AdsKit"
include(":sample")
include(":ads-core")
include(":ads-admob")
include(":ads-applovin")
include(":ads")
