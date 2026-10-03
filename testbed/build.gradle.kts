plugins { id("com.android.application") }
android {
    namespace = "com.reaxkey.testbed"
    compileSdk = 37
    defaultConfig { applicationId = "com.reaxkey.testbed"; minSdk = 26; targetSdk = 37; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("androidx.core:core-ktx:1.19.0") }
