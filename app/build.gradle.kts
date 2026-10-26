import java.io.File

plugins { alias(libs.plugins.android.application); alias(libs.plugins.ksp) }

val releaseStoreFile = providers.environmentVariable("BOOMTRACK_KEYSTORE").orNull?.let(::File)
val releaseStorePassword = providers.environmentVariable("BOOMTRACK_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("BOOMTRACK_KEY_ALIAS").orElse("boomtrack").get()
val releaseKeyPassword = providers.environmentVariable("BOOMTRACK_KEY_PASSWORD").orNull
val releaseSigningReady = releaseStoreFile?.isFile == true && releaseStorePassword != null && releaseKeyPassword != null
android {
    namespace = "org.ttpod.recreation"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.ttpod.recreation.dev"
        minSdk = 23
        targetSdk = 37
        versionCode = 9
        versionName = "0.4.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { viewBinding = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    signingConfigs {
        if (releaseSigningReady) create("boomtrackRelease") {
            storeFile = releaseStoreFile
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }
    buildTypes { release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        if (releaseSigningReady) signingConfig = signingConfigs.getByName("boomtrackRelease")
    } }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(project(":domain")); implementation(project(":playback"))
    implementation(libs.appcompat); implementation(libs.activity); implementation(libs.lifecycle)
    implementation(libs.coroutines.android); implementation(libs.room.runtime)
    implementation(libs.sqlite)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.runner); androidTestImplementation(libs.test.junit)
}

