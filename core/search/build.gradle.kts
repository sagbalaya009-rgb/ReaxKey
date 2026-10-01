plugins { id("com.android.library") }
android {
    namespace = "com.reaxkey.search"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation(project(":core:model")); testImplementation("junit:junit:4.13.2") }
