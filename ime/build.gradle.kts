plugins { id("com.android.library") }
android {
    namespace = "com.reaxkey.ime"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(project(":core:model")); implementation(project(":core:ui"))
    implementation(project(":core:data")); implementation(project(":core:media"))
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("io.coil-kt.coil3:coil:3.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
