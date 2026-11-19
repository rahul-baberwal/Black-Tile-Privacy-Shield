import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
  val localPropFile = rootProject.file("local.properties")
  if (localPropFile.exists()) {
    localPropFile.inputStream().use { load(it) }
  }
}

android {
  namespace = "com.rahulbaberwal.blacktile"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.rahulbaberwal.blacktile"
    minSdk = 26
    targetSdk = 34
    versionCode = 5
    versionName = "1.1.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val rawKeystore = System.getenv("RELEASE_STORE_FILE")
        ?: System.getenv("KEYSTORE_PATH")
        ?: localProperties.getProperty("RELEASE_STORE_FILE")
        ?: "my-upload-key.jks"
      val resolvedKeystore = file(rawKeystore).let {
        if (it.isAbsolute && it.exists()) it else rootProject.file(rawKeystore)
      }
      storeFile = resolvedKeystore
      storePassword = System.getenv("RELEASE_STORE_PASSWORD")
        ?: System.getenv("STORE_PASSWORD")
        ?: localProperties.getProperty("RELEASE_STORE_PASSWORD")
        ?: ""
      keyAlias = System.getenv("RELEASE_KEY_ALIAS")
        ?: System.getenv("KEY_ALIAS")
        ?: localProperties.getProperty("RELEASE_KEY_ALIAS")
        ?: "upload"
      keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        ?: System.getenv("KEY_PASSWORD")
        ?: localProperties.getProperty("RELEASE_KEY_PASSWORD")
        ?: ""
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore").takeIf { it.exists() }
        ?: file("${System.getProperty("user.home")}/.android/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = false
    buildConfig = true
  }
}

dependencies {
  testImplementation(libs.junit)
}
