plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.room)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.play.publisher)
}

fun stringConfig(
    name: String,
    defaultValue: String,
): String = providers.gradleProperty(name).orElse(defaultValue).get()

fun optionalStringConfig(name: String): String? = providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }

android {
    namespace = "com.parsfilo.astrology"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.parsfilo.astrology"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.parsfilo.astrology.HiltTestRunner"

        buildConfigField(
            "String",
            "BASE_URL",
            "\"https://astrology.parsfilo.com/\"",
        )
        buildConfigField("String", "API_PREFIX", "\"api/v1/\"")
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"${stringConfig("PRIVACY_POLICY_URL", "https://astrology.parsfilo.com/privacy")}\"",
        )
        buildConfigField(
            "String",
            "TERMS_OF_USE_URL",
            "\"${stringConfig("TERMS_OF_USE_URL", "https://astrology.parsfilo.com/terms")}\"",
        )
        buildConfigField(
            "String",
            "SUPPORT_EMAIL",
            "\"${stringConfig("SUPPORT_EMAIL", "support@parsfilo.com")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_APP_ID",
            "\"${stringConfig("ADMOB_APP_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_ID",
            "\"${stringConfig("ADMOB_BANNER_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_ID",
            "\"${stringConfig("ADMOB_INTERSTITIAL_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_REWARDED_ID",
            "\"${stringConfig("ADMOB_REWARDED_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_REWARDED_INTERSTITIAL_ID",
            "\"${stringConfig("ADMOB_REWARDED_INTERSTITIAL_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_APP_OPEN_ID",
            "\"${stringConfig("ADMOB_APP_OPEN_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_NATIVE_ADVANCED_ID",
            "\"${stringConfig("ADMOB_NATIVE_ADVANCED_ID", "")}\"",
        )
        buildConfigField(
            "boolean",
            "ADMOB_USE_TEST_IDS",
            stringConfig("ADMOB_USE_TEST_IDS", "false"),
        )
        manifestPlaceholders["ADMOB_APP_ID"] =
            stringConfig(
                "ADMOB_APP_ID",
                "",
            )
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/5354046379\"")
            buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "ADMOB_NATIVE_ADVANCED_ID", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("boolean", "ADMOB_USE_TEST_IDS", "true")
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
//            applicationIdSuffix = ".debug"
//            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ADMOB_USE_TEST_IDS", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

play {
    defaultToAppBundles.set(true)
    track.set(stringConfig("PLAY_TRACK", "internal"))
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    optionalStringConfig("PLAY_SERVICE_ACCOUNT_JSON_PATH")?.let { credentialsPath ->
        serviceAccountCredentials.set(file(credentialsPath))
    }
}

detekt {
    buildUponDefaultConfig = true
    baseline = file("$projectDir/detekt-baseline.xml")
}

dependencies {

    // ── Core ──────────────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // ── Compose ───────────────────────────────────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ── Glance (Widget) ───────────────────────────────────────────────────────
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // ── Hilt ──────────────────────────────────────────────────────────────────
    implementation(libs.hilt.android)
    implementation(libs.guava)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ── Room ──────────────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Firebase ──────────────────────────────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // ── Google Play Services ──────────────────────────────────────────────────
    implementation(libs.play.services.ads)
    implementation(libs.play.services.appset)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.billing.ktx)

    // ── WorkManager ───────────────────────────────────────────────────────────
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ── Network ───────────────────────────────────────────────────────────────
    implementation(libs.okhttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)

    // ── Serialization ─────────────────────────────────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Image Loading ─────────────────────────────────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ── Animation ─────────────────────────────────────────────────────────────
    implementation(libs.lottie.compose)

    // ── Logging ───────────────────────────────────────────────────────────────
    implementation(libs.timber)

    // ── Unit Testing ──────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)

    // ── Instrumented Testing ──────────────────────────────────────────────────
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.hilt.android)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // ── Debug ─────────────────────────────────────────────────────────────────
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
