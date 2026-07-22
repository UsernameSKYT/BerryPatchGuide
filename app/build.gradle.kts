import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// On Windows with Korean username, CMake crashes on non-ASCII paths.
// Redirect build outputs to ASCII-safe path only when running on Windows.
if (System.getProperty("os.name").lowercase().contains("windows")) {
    layout.buildDirectory.set(file("C:/tmp/bpo"))
}

// 실제 AdMob 광고 ID 로딩 (local.properties, git에 커밋되지 않음)
// AdMob 콘솔에서 앱/광고 단위를 만든 후 아래 3개 값을 local.properties에 추가하면
// release 빌드부터 자동으로 테스트 광고 대신 실제(수익화) 광고가 적용됩니다.
//   ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
//   ADMOB_BANNER_AD_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy
//   ADMOB_NATIVE_AD_UNIT_ID=ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

// 값이 없으면 Google 공식 테스트 ID로 안전하게 폴백
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
val testNativeAdUnitId = "ca-app-pub-3940256099942544/2247696110"

val realAdmobAppId = localProperties.getProperty("ADMOB_APP_ID") ?: testAdmobAppId
val realBannerAdUnitId = localProperties.getProperty("ADMOB_BANNER_AD_UNIT_ID") ?: testBannerAdUnitId
val realNativeAdUnitId = localProperties.getProperty("ADMOB_NATIVE_AD_UNIT_ID") ?: testNativeAdUnitId

// 릴리즈 서명 키 (CI 환경변수로 주입, 로컬에는 없을 수 있음 -> 없으면 debug 서명으로 폴백)
val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.berry.patchguide"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.berry.patchguide"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // 기본값(디버그 등): 항상 Google 공식 테스트 광고 ID 사용 (안전)
        manifestPlaceholders["admobAppId"] = testAdmobAppId
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$testBannerAdUnitId\"")
        buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"$testNativeAdUnitId\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 정식 릴리즈 키스토어가 CI 환경변수로 주입되면 그것으로 서명하고,
            // 로컬 등 키가 없는 환경에서는 debug 키로 폴백해 테스트 설치는 계속 가능하게 함.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            // 릴리즈 빌드: local.properties에 실제 광고 ID가 있으면 그 값을 사용 (없으면 테스트 ID 유지)
            manifestPlaceholders["admobAppId"] = realAdmobAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$realBannerAdUnitId\"")
            buildConfigField("String", "NATIVE_AD_UNIT_ID", "\"$realNativeAdUnitId\"")
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
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

    // AdMob
    implementation(libs.admob)

    // DocumentFile (모드 설치: SAF 폴더에 파일 복사)
    implementation(libs.androidx.documentfile)

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
