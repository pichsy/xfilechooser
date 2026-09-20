plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.pichs.app.xfilechooser"
    compileSdk = rootProject.ext.get("compileSdk") as Int

    defaultConfig {
        applicationId = "com.pichs.app.xfilechooser"
        minSdk = rootProject.ext.get("minSdk") as Int
        targetSdk = rootProject.ext.get("targetSdk") as Int
        versionCode = rootProject.ext.get("versionCode") as Int
        versionName = rootProject.ext.get("versionName").toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(rootProject.ext.get("javaVersion") as String)
        targetCompatibility = JavaVersion.toVersion(rootProject.ext.get("javaVersion") as String)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.xwidget)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":filechooser"))
}
