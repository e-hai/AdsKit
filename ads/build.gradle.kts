plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = findProperty("group")?.toString() ?: "com.github.AdvertisingKit"
version = findProperty("version")?.toString() ?: libs.versions.sdkVersion.get()

android {
    namespace = "com.kit.ads.bundle"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release")
    }
}

val adsBundleAarDir = layout.buildDirectory.dir("outputs/aar")
val adsBundleSdkVersion = libs.versions.sdkVersion.get()

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "AdsKit"
            }
        }
    }

    // Capture DirectoryProperty / String only — Project must not be closed over in doLast (CC).
    tasks.named("bundleReleaseAar").configure {
        val aarDir = adsBundleAarDir
        val versionedName = "ads-v$adsBundleSdkVersion.aar"
        doLast {
            val dir = aarDir.get().asFile
            val source = dir.resolve("ads-release.aar")
            if (!source.exists()) return@doLast
            source.copyTo(dir.resolve(versionedName), overwrite = true)
        }
    }
}

dependencies {
    api(project(":ads-core"))
    api(project(":ads-admob"))
    api(project(":ads-applovin"))
}
