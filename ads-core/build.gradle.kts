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

val adsCoreAarDir = layout.buildDirectory.dir("outputs/aar")
val adsCoreSdkVersion = libs.versions.sdkVersion.get()

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "AdsKit-core"
            }
        }
    }

    // Capture DirectoryProperty / String only — Project must not be closed over in doLast (CC).
    tasks.named("bundleReleaseAar").configure {
        val aarDir = adsCoreAarDir
        val versionedName = "ads-core-v$adsCoreSdkVersion.aar"
        doLast {
            val dir = aarDir.get().asFile
            val source = dir.resolve("ads-core-release.aar")
            if (!source.exists()) return@doLast
            source.copyTo(dir.resolve(versionedName), overwrite = true)
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
