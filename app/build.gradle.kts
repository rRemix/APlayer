import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

fun readProperties(file: File): Properties {
    val properties = Properties()
    var stream: FileInputStream? = null
    try {
        stream = FileInputStream(file)
        properties.load(stream)
    } catch (throwable: Throwable) {
        logger.warn("Fail to read properties from file $file: $throwable")
    } finally {
        stream?.close()
    }
    return properties
}

val properties = readProperties(rootProject.file("local.properties"))

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "remix.myplayer"

    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "25.2.9519653"

    defaultConfig {
        applicationId = "remix.myplayer"
        minSdk = 19
        targetSdk = 33

        versionCode = 16500
        versionName = "1.6.5.0"

        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        buildConfigField(
            "String",
            "LASTFM_API_KEY",
            "\"${properties.getProperty("LASTFM_API_KEY")}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_PLAY_LICENSE_KEY",
            "\"${properties.getProperty("GOOGLE_PLAY_LICENSE_KEY")}\""
        )
        buildConfigField(
            "String",
            "BUGLY_APPID",
            "\"${properties.getProperty("BUGLY_APPID")}\""
        )
        buildConfigField(
            "String",
            "GITHUB_SHA",
            "\"${System.getenv("GITHUB_SHA")}\""
        )

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )
        }

        setProperty("archivesBaseName", "APlayer-v${versionName}")
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = project.file("Debug.jks")
            storePassword = "123456"
            keyAlias = "Debug"
            keyPassword = "123456"

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }

        create("releaseConfig") {
            storeFile = File(properties.getProperty("keystore.storeFile") ?: "")
            storePassword = properties.getProperty("keystore.storePassword")
            keyAlias = properties.getProperty("keystore.keyAlias")
            keyPassword = properties.getProperty("keystore.keyPassword")

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs["debugConfig"]
            isDebuggable = true
            isMinifyEnabled = false

            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }

        release {
            signingConfig = signingConfigs["releaseConfig"]
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }

        // For CI builds only
        create("ciRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs["debugConfig"]
            applicationIdSuffix = ".ci"
            versionNameSuffix = "-CI"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/third-party/jaudiotagger-android/src")
        }
    }

    externalNativeBuild {
        cmake {
            path("CMakeLists.txt")
        }
    }

    flavorDimensions += listOf("channel", "updater")
    productFlavors {
        create("nonGoogle") {
            dimension = "channel"
            isDefault = true
        }
        create("google") {
            dimension = "channel"
        }

        create("withUpdater") {
            dimension = "updater"
            isDefault = true
            buildConfigField("boolean", "ENABLE_UPDATER", "true")
        }
        create("withoutUpdater") {
            dimension = "updater"
            buildConfigField("boolean", "ENABLE_UPDATER", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf("MissingTranslation", "InvalidPackage")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    dependenciesInfo {
        includeInApk = false
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        if (variantBuilder.productFlavors.containsAll(
                listOf(
                    "channel" to "google",
                    "updater" to "withUpdater"
                )
            )
        ) {
            variantBuilder.enable = false
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.appcompat)
    implementation(libs.cardview)
    implementation(libs.constraintlayout)
    implementation(libs.media)
    implementation(libs.multidex)
    implementation(libs.palette.ktx)
    implementation(libs.swiperefreshlayout)

    implementation(libs.material)

    implementation(libs.glide)
    ksp(libs.glide.ksp)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava2)

    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.rxjava2)

    implementation(libs.android.crop) {
        artifact {
            type = "aar"
        }
    }
    implementation(libs.bugly)
    implementation(libs.logback.android)
    implementation(libs.material.dialog)
    implementation(libs.rebound)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxpermissions)
    implementation(libs.sardine.android) {
        // https://github.com/thegrizzlylabs/sardine-android/issues/70
        // 上游已经exclude了，但是不知道为什么还是会有
        // https://github.com/thegrizzlylabs/sardine-android/blob/d0af7ae8e7ee0654a763c4c6f638a5e98b1782e9/build.gradle#L46
        exclude(group = "xpp3", module = "xpp3")
    }
    implementation(libs.slf4j)
    implementation(libs.timber)
    implementation(libs.tinypinyin)

    debugImplementation(libs.leakcanary)

    val googleImplementation by configurations
    googleImplementation(libs.billingclient)
}
