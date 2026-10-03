plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
    namespace = "com.reaxkey.keyboard"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.reaxkey.keyboard"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("VERSION_CODE").orElse("1").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").orElse("1.0.0").get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    val uploadKey=providers.environmentVariable("REAXKEY_KEYSTORE").orNull
    if(uploadKey!=null) signingConfigs.create("upload") {
        storeFile=file(uploadKey)
        storePassword=providers.environmentVariable("REAXKEY_STORE_PASSWORD").get()
        keyAlias=providers.environmentVariable("REAXKEY_KEY_ALIAS").get()
        keyPassword=providers.environmentVariable("REAXKEY_KEY_PASSWORD").get()
    }
    buildTypes.getByName("release") {
        isMinifyEnabled=true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        if(uploadKey!=null) signingConfig=signingConfigs.getByName("upload")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core:1.7.0")
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":feature:library"))
    implementation(project(":feature:settings"))
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("io.coil-kt.coil3:coil:3.6.3")
    implementation("io.coil-kt.coil3:coil-gif:3.6.3")
    implementation(project(":core:model"))
    implementation(project(":ime"))
    implementation(project(":core:ui"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.material3:material3")
}
