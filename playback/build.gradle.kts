plugins { alias(libs.plugins.android.library) }
android {
    namespace = "org.ttpod.recreation.playback"
    compileSdk = 37
    defaultConfig { minSdk = 23; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    api(project(":domain"))
    api(libs.media3.session)
    implementation(libs.media3.exoplayer)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
}
