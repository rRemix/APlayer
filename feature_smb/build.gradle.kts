plugins {
  alias(libs.plugins.android.dynamic.feature)
  alias(libs.plugins.kotlin)
}
android {
  namespace = "remix.myplayer.smb"
  compileSdk = 35

  defaultConfig {
    minSdk = 21
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  flavorDimensions += "distribution"
  productFlavors {
    create("normal") {
      dimension = "distribution"
    }
    create("foss") {
      dimension = "distribution"
    }
    create("google") {
      dimension = "distribution"
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles("proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  implementation(project(":app"))

  implementation(libs.smbj)
  implementation(libs.timber)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.androidx.annotation)

//  implementation(libs.androidx.core.ktx)
//  testImplementation(libs.junit)
//  androidTestImplementation(libs.androidx.junit)
//  androidTestImplementation(libs.androidx.espresso.core)
}