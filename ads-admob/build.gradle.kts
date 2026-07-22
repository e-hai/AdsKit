plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = findProperty("group")?.toString() ?: "com.github.e-hai"
version = findProperty("version")?.toString() ?: libs.versions.sdkVersion.get()

android {
    namespace = "com.kit.ads.provider.admob"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release")
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "AdsKit-admob"
            }
        }
    }
}

dependencies {
    api(project(":ads-core"))

    // Admob start
    implementation(libs.admob.mediation.play.services.ads)
    implementation(libs.admob.mediation.applovin)
    implementation(libs.admob.mediation.chartboost)
    implementation(libs.admob.mediation.fyber)
    implementation(libs.admob.mediation.imobile)
    implementation(libs.admob.mediation.inmobi)
    implementation(libs.admob.mediation.ironsource)
    implementation(libs.admob.mediation.vungle)
    implementation(libs.admob.mediation.maio) {
        exclude(group = "com.maio", module = "android-sdk-v2")
    }
    implementation(libs.admob.mediation.facebook)
    implementation(libs.admob.mediation.mintegral)
    implementation(libs.admob.mediation.mytarget)
    implementation(libs.admob.mediation.pangle)
    implementation(libs.admob.mediation.unity.ads)
    implementation(libs.admob.mediation.unity)
    // Admob end
}
