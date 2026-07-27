import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
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

    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "25.2.9519653"

    defaultConfig {
        applicationId = "remix.myplayer"
        minSdk = 21
        targetSdk = 35

        versionCode = 21000
        versionName = "2.1.0.0"

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
//        buildConfigField(
//            "String",
//            "GITHUB_SHA",
//            "\"${System.getenv("GITHUB_SHA")}\""
//        )

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
//                "x86",
                "x86_64"
            )
        }

        setProperty("archivesBaseName", "APlayer-v${versionName}")
    }

    androidResources {
        localeFilters += listOf(
            "en",
            "ja",
            "ja-rJP",
            "zh",
            "zh-rCN",
            "zh-rHK",
            "zh-rTW"
        )
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
    }

    externalNativeBuild {
        cmake {
            path("CMakeLists.txt")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("normal") {
            dimension = "distribution"
            isDefault = true
            buildConfigField(
                "String",
                "BUGLY_APP_ID",
                "\"${properties.getProperty("BUGLY_APP_ID")}\""
            )
            buildConfigField("boolean", "ENABLE_UPDATE", "true")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_UPDATE", "false")
        }
        create("google") {
            dimension = "distribution"
            buildConfigField("boolean", "ENABLE_UPDATE", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-module-name", "remix.myplayer")
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += listOf("MissingTranslation", "InvalidPackage")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.13"
    // }
    dependenciesInfo {
        includeInApk = false
    }

    dynamicFeatures += setOf(":feature_smb")

    room {
        schemaDirectory("$projectDir/schemas")
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this
            val flavor = variant.productFlavors.firstOrNull()?.name
            if (variant.buildType.name == "release" && flavor != null) {
                val sortPrefix = when (flavor) {
                    "normal" -> "1"
                    "foss" -> "2"
                    else -> ""
                }
                if (sortPrefix.isNotEmpty()) {
                    (output as BaseVariantOutputImpl).outputFileName = "${sortPrefix}-APlayer-v${variant.versionName}-${flavor}-release.apk"
                }
            }
        }
    }
}

baselineProfile {
    saveInSrc = true

    warnings {
        disabledVariants = false
    }
//  variants {
//      maybeCreate("normalRelease").apply {
//          from(project(":baselineprofile"))
//      }
//  }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
//    implementation(libs.kotlinx.serialization)

    implementation(libs.appcompat)
    implementation(libs.media)
    implementation(libs.androidx.media3.exoplayer)
    implementation(files("libs/lib-decoder-ffmpeg-release.aar"))
    implementation(libs.multidex)
    implementation(libs.palette.ktx)

    implementation(libs.material)

    implementation(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)

    implementation(libs.image.cropper)
    implementation(libs.xxpermissions)
    implementation(libs.sardine.android) {
        // https://github.com/thegrizzlylabs/sardine-android/issues/70
        // 上游已经exclude了，但是不知道为什么还是会有
        // https://github.com/thegrizzlylabs/sardine-android/blob/d0af7ae8e7ee0654a763c4c6f638a5e98b1782e9/build.gradle#L46
        exclude(group = "xpp3", module = "xpp3")
    }
    implementation(libs.timber)

    debugImplementation(libs.leakcanary)

    val normalImplementation by configurations
    normalImplementation(libs.bugly)

    val googleImplementation by configurations
    googleImplementation(libs.billingclient)
    googleImplementation(libs.play.feature.delivery)
    googleImplementation(libs.play.feature.delivery.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.nav)
    implementation(libs.androidx.hilt.navi.compose)
    implementation(libs.reorderable)
    implementation(libs.kotlinx.serialization)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))

    implementation(project(":taglib"))
}

// 上传mapping文件
if (properties.getProperty("BUGLY_UPLOAD") == "1") {
    val uploadMapping by tasks.registering(Exec::class) {
        val jarFile = File(properties.getProperty("BUGLY_JAR") ?: "")
        if (!jarFile.exists()) {
            logger.warn("jarFile: ${jarFile.absolutePath} don't exist")
            return@registering
        }

        val appId = properties.getProperty("BUGLY_APP_ID")
        val appKey = properties.getProperty("BUGLY_APP_KEY")
        if (appId.isNullOrEmpty() || appKey.isNullOrEmpty()) {
            logger.warn("appId or appKey for bugly is invalid")
            return@registering
        }

        val mappingFile =
            file("${project.layout.buildDirectory.asFile.get()}/outputs/mapping/normalRelease/mapping.txt")
        val args = listOf(
            "-appid",
            appId,
            "-appkey",
            appKey,
            "-bundleid",
            android.defaultConfig.applicationId,
            "-version",
            android.defaultConfig.versionName,
            "-buildNo",
            android.defaultConfig.versionCode.toString(),
            "-platform",
            "Android",
            "-inputMapping",
            mappingFile.absolutePath
        )

        commandLine = listOf("java", "-jar", jarFile.absolutePath) + args
        standardOutput = System.out
        errorOutput = System.out
    }

    tasks.whenTaskAdded {
        if (name == "assembleNormalRelease") {
            finalizedBy(uploadMapping)
        }
    }
}
