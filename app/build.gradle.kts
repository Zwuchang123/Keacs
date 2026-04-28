import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.keacs.app"
    compileSdk = 35

    val releaseStoreFilePath = providers.environmentVariable("KEACS_RELEASE_STORE_FILE")
        .orElse(providers.gradleProperty("keacs.release.storeFile"))
        .orNull
    val releaseStorePassword = providers.environmentVariable("KEACS_RELEASE_STORE_PASSWORD")
        .orElse(providers.gradleProperty("keacs.release.storePassword"))
        .orNull
    val releaseKeyAlias = providers.environmentVariable("KEACS_RELEASE_KEY_ALIAS")
        .orElse(providers.gradleProperty("keacs.release.keyAlias"))
        .orNull
    val releaseKeyPassword = providers.environmentVariable("KEACS_RELEASE_KEY_PASSWORD")
        .orElse(providers.gradleProperty("keacs.release.keyPassword"))
        .orNull
    val hasReleaseSigning = listOf(
        releaseStoreFilePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.keacs.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "UPDATE_URL",
            "\"https://gitee.com/zwuc/keacs/releases\"",
        )
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "128m"
}

val releaseVersionName = android.defaultConfig.versionName ?: "0.0.0"

tasks.register("packageReleaseForPublish") {
    group = "distribution"
    description = "构建 release APK 并生成带版本号的发布文件名"
    dependsOn("assembleRelease")

    doLast {
        val hasReleaseSigning = listOf(
            providers.environmentVariable("KEACS_RELEASE_STORE_FILE")
                .orElse(providers.gradleProperty("keacs.release.storeFile"))
                .orNull,
            providers.environmentVariable("KEACS_RELEASE_STORE_PASSWORD")
                .orElse(providers.gradleProperty("keacs.release.storePassword"))
                .orNull,
            providers.environmentVariable("KEACS_RELEASE_KEY_ALIAS")
                .orElse(providers.gradleProperty("keacs.release.keyAlias"))
                .orNull,
            providers.environmentVariable("KEACS_RELEASE_KEY_PASSWORD")
                .orElse(providers.gradleProperty("keacs.release.keyPassword"))
                .orNull,
        ).all { !it.isNullOrBlank() }

        check(hasReleaseSigning) {
            "未配置发布签名，不能生成正式发布 APK。请配置 KEACS_RELEASE_STORE_FILE、KEACS_RELEASE_STORE_PASSWORD、KEACS_RELEASE_KEY_ALIAS、KEACS_RELEASE_KEY_PASSWORD。"
        }
        val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val sourceApk = releaseDir
            .listFiles()
            ?.firstOrNull {
                it.isFile &&
                    it.extension == "apk" &&
                    !it.name.contains("unsigned", ignoreCase = true)
            }
            ?: error("未找到已签名 release APK，请检查发布签名配置。")
        val publishDir = layout.buildDirectory.dir("outputs/publish/release").get().asFile
        if (!publishDir.exists()) {
            publishDir.mkdirs()
        }
        val targetApk = publishDir.resolve("keacs-v$releaseVersionName.apk")
        // 发布文件单独复制一份，避免不同构建环境下默认 APK 命名不一致。
        sourceApk.copyTo(targetApk, overwrite = true)
        println("Published APK: ${targetApk.absolutePath}")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.13")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
