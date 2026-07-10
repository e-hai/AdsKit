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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "AdsKit"
            }
        }
    }

    tasks.named("bundleReleaseAar").configure {
        doLast {
            val aarDir = layout.buildDirectory.dir("outputs/aar").get().asFile
            val source = File(aarDir, "ads-release.aar")
            val target = File(aarDir, "ads-v${libs.versions.sdkVersion.get()}.aar")
            if (!source.exists()) {
                return@doLast
            }
            source.copyTo(target, overwrite = true)
        }
    }
}

dependencies {
    api(project(":ads-core"))
    api(project(":ads-admob"))
    api(project(":ads-applovin"))
}
