plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.berry.patchguide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.berry.patchguide"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + Moshi
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Billing
    implementation(libs.billing.client)

    // Unit Tests
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test> {
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dnative.encoding=UTF-8",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED"
    )
    systemProperty("file.encoding", "UTF-8")
    systemProperty("native.encoding", "UTF-8")
    // argfile 인코딩 버그 우회: 프로젝트 빌드 출력 경로를 ASCII junction 경로로 교체
    // 파일은 이동하지 않음 - junction C:/tmp/bpo -> app/build (심볼릭 링크)
    doFirst {
        val koreanBase = "C:/Users/김재경/Projects/BerryPatchGuide/app/build"
        val asciiBase = "C:/tmp/bpo"
        classpath = files(classpath.map { f ->
            val normalized = f.absolutePath.replace("\\", "/")
            if (normalized.startsWith(koreanBase)) {
                File(asciiBase + normalized.removePrefix(koreanBase))
            } else {
                f
            }
        })
    }
}
