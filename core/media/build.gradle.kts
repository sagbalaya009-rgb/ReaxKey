plugins { id("com.android.library") }
android {
    namespace = "com.reaxkey.media"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":core:model"))
    implementation("androidx.core:core-ktx:1.19.0")
}
