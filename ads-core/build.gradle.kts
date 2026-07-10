plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = findProperty("group")?.toString() ?: "com.github.AdvertisingKit"
version = findProperty("version")?.toString() ?: libs.versions.sdkVersion.get()

android {
    namespace = "com.kit.ads"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            consumerProguardFiles("consumer-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
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
                artifactId = "AdsKit-core"
            }
        }
    }

    tasks.named("bundleReleaseAar").configure {
        doLast {
            val aarDir = layout.buildDirectory.dir("outputs/aar").get().asFile
            val source = File(aarDir, "ads-core-release.aar")
            val target = File(aarDir, "ads-core-v${libs.versions.sdkVersion.get()}.aar")
            if (!source.exists()) {
                return@doLast
            }
            source.copyTo(target, overwrite = true)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Google UMP — shared by AdMob and AppLovin (manual CMP path)
    api(libs.user.messaging.platform)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // SPI provider modules for unit tests that exercise real ServiceLoader wiring
    testImplementation(project(":ads-admob"))
    testImplementation(project(":ads-applovin"))
}
