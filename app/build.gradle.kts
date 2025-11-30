import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
    alias(libs.plugins.parcelize)
}

android {
    namespace = "com.xandy.lite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xandy.lite"
        minSdk = 26
        targetSdk = 36
        versionCode = 52
        versionName = "1.1.300"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { this.debugSymbolLevel = "SYMBOL_TABLE" }
        buildConfigField(
            "String", "PART_ONE", "\"" + gradleLocalProperties(rootDir, providers)
                .getProperty("PART_ONE", "") + "\""
        )
        buildConfigField(
            "String", "PART_TWO", "\"" + gradleLocalProperties(rootDir, providers)
                .getProperty("PART_TWO", "") + "\""
        )
        buildConfigField(
            "String", "PART_THREE", "\"" + gradleLocalProperties(rootDir, providers)
                .getProperty("PART_THREE", "") + "\""
        )
        buildConfigField(
            "String", "PART_FOUR", "\"" + gradleLocalProperties(rootDir, providers)
                .getProperty("PART_FOUR", "") + "\""
        )
        buildConfigField(
            "String", "PART_FIVE", "\"" + gradleLocalProperties(rootDir, providers)
                .getProperty("PART_FIVE", "") + "\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("normal") {
            dimension = "version"
            versionNameSuffix = "-normal"
        }
        create("demo") {
            applicationIdSuffix = ".demo"
            dimension = "version"
            versionNameSuffix = "-demo"
        }
        create("full") {
            applicationIdSuffix = ".full"
            dimension = "version"
            versionNameSuffix = "-full"
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
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // Media Player
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)

    /** File permissions */
    implementation(libs.accompanist.permissions)
    /** Vertical scroll bar */
    //implementation(libs.androidx.foundation.desktop)
    implementation(libs.lazycolumnscrollbar)
    implementation(files("libs/taglib-release.aar"))
    implementation(libs.androidx.room.runtime)


    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3.session)


    // If this project uses any Kotlin source, use Kotlin Symbol Processing (KSP)
    // See Add the KSP plugin to your project
    ksp(libs.androidx.room.compiler)

    // If this project only uses Java source, use the Java annotationProcessor
    // No additional plugins are necessary
    annotationProcessor(libs.androidx.room.compiler)

    // optional - Kotlin Extensions and Coroutines support for Room
    implementation(libs.androidx.room.ktx)

    // optional - RxJava2 support for Room
    implementation(libs.androidx.room.rxjava2)

    // optional - RxJava3 support for Room
    implementation(libs.androidx.room.rxjava3)

    // optional - Guava support for Room, including Optional and ListenableFuture
    implementation(libs.androidx.room.guava)

    // optional - Test helpers
    testImplementation(libs.androidx.room.testing)

    // optional - Paging 3 Integration
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.datastore.preferences)
    // optional - RxJava2 support
    implementation(libs.androidx.datastore.preferences.rxjava2)
    // optional - RxJava3 support
    implementation(libs.androidx.datastore.preferences.rxjava3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    implementation(libs.logging.interceptor)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}