plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

ext {
    set("compileSdk", 35)
    set("minSdk", 26)
    set("targetSdk", 35)
    set("javaVersion", "17")
    set("versionName", "2.0.0")
    set("versionCode", 200)
}
