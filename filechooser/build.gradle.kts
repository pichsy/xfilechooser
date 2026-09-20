plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pichs.filechooser"
    compileSdk = rootProject.ext.get("compileSdk") as Int

    defaultConfig {
        minSdk = rootProject.ext.get("minSdk") as Int
        consumerProguardFiles("consumer-rules.pro")
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
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.annotation)
    api(libs.androidx.fragment.ktx)
}

// 生成 javadoc（仅处理 Java 源码）
tasks.register<Javadoc>("javadoc") {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).charSet = "UTF-8"
    val javaSrc = fileTree("src/main/java") { include("**/*.java") }
    source = javaSrc
    exclude("**/BuildConfig.java")
    exclude("**/R.java")
    isFailOnError = false
}

// 与 xwidget 保持一致：发布时禁用 javadoc（Android 库的依赖是 AAR，javadoc 无法直接读取）
tasks.withType<Javadoc> {
    enabled = false
}

tasks.register<Jar>("androidJavadocsJar") {
    dependsOn("javadoc")
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc").get().outputs.files)
}

// 此写法可忽略文件夹层级带来的影响
apply(from = "${rootProject.rootDir}/maven.gradle")
