import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
}

val keystoreProperties = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.ubopod.uboapp.wear"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Matches phone-app's applicationId — Play Console's Wear OS form
        // factor track lives under the same app listing as the phone app,
        // which requires every track to share one package name.
        applicationId = "com.ubopod.uboapp.phone"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // versionCode is a single sequence shared across every form-factor
        // track under that one listing — phone-app is already at 2, so
        // this must be higher, not restarted from 1.
        versionCode = 3
        versionName = "0.1.0"
    }

    // Same upload key as phone-app — the standard setup for a phone+Wear
    // app pair distributed together.
    signingConfigs {
        create("release") {
            if (keystoreProperties.containsKey("storeFile")) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/io.netty.versions.properties",
            )
        }
    }
}

dependencies {
    implementation(project(":lib"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    // Compose Foundation + UI graphics + tooling-preview come from the
    // shared BOM. Material 3 is intentionally absent — Wear has its own.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)

    // Real QR bitmap generation for RenderKind.QrCode/QrCodeCarousel — same
    // library the phone-app already uses (see ui/common/QrCode.kt there).
    implementation(libs.zxing.core)

    // Wear Tile — compact CPU/RAM tile in the watch's tile carousel.
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)
    implementation(libs.coroutines.guava)

    debugImplementation(libs.compose.ui.tooling)
}
