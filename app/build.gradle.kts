import java.io.FileInputStream
import java.util.Properties
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.gradleVersionsPlugin)
    alias(libs.plugins.androidKsp)
    alias(libs.plugins.legacy.kapt)
    id("kotlin-parcelize")
}

val properties = gradleLocalProperties(rootDir, providers)
val mStoreFile: String? = properties.getProperty("storeFile")
val mStorePassword: String? = properties.getProperty("storePassword")
val mKeyAlias: String? = properties.getProperty("keyAlias")
val mKeyPassword: String? = properties.getProperty("keyPassword")
val httpDnsId = properties.getProperty("httpDnsId") ?: "\"\""
val httpDnsSecret = properties.getProperty("httpDnsSecret") ?: "\"\""

val appVersionCode = 99
val appVersionName = "3.5"
val appVersionSuffix = ""

android {
    namespace = "me.ykrank.s1next"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // 16KB 页面大小对齐支持（Android 15+）
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    defaultConfig {
        applicationId = "me.ykrank.s1next"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = "${appVersionName}.${appVersionCode}${appVersionSuffix}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    signingConfigs {
        if (!mStoreFile.isNullOrEmpty()) {
            create("release") {
                keyAlias = mKeyAlias
                keyPassword = mKeyPassword
                storeFile = file(mStoreFile)
                storePassword = mStorePassword
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
    
    // Lint 配置
    lint {
        enable += "ParcelCreator"
        abortOnError = false
    }

    // 测试配置
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildTypes {
        debug {
            multiDexEnabled = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        create("alpha") {
            multiDexEnabled = true
            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha"
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            matchingFallbacks += listOf("release", "debug")
        }

        release {
            multiDexEnabled = true
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildTypes.forEach {
        it.buildConfigField("String", "DB_NAME", "\"s1.db\"")
        it.buildConfigField("String", "HTTP_DNS_ID", httpDnsId)
        it.buildConfigField("String", "HTTP_DNS_SECRET", httpDnsSecret)
    }

    flavorDimensions += "market"
    productFlavors {
        create("play") {
            dimension = "market"
            manifestPlaceholders["APP_CHANNEL"] = "play.google.com"
            versionNameSuffix = "-play"
        }
        create("normal") {
            dimension = "market"
            manifestPlaceholders["APP_CHANNEL"] = "normal"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // 本地库
    implementation(fileTree("libs") { include("*.jar", "*.aar") })

    // 项目模块
    implementation(project(":library"))
    implementation(project(":JKeyboardPanelSwitch"))

    // DataBinding (仍需 kapt)
    kapt(libs.databinding.compiler)

    // Paging
    implementation(libs.paging)

    // Bugly
    implementation(libs.bugly.nativecrashreport)

    // Dagger (使用 kapt)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    // AndroidX
    implementation(libs.androidx.transition)

    // 网络库
    implementation(libs.okhttp.urlconnection)
    implementation(libs.okhttp.coroutines)
    implementation(libs.okhttp)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.adapter.rxjava2)
    implementation(libs.retrofit2.converter.jackson)
    implementation(libs.retrofit2.converter.scalars)

    // JSON
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)

    // Parcel
    implementation(libs.paperparcel)
    implementation(libs.paperparcel.kotlin)
    implementation(libs.paperparcel.api)
    kapt(libs.paperparcel.compiler)

    // Glide
    ksp(libs.glide.ksp)

    // UI
    implementation(libs.photoview)
    implementation(libs.quicksidebar)

    // Flipper (调试工具)
    debugImplementation(libs.flipper)
    debugImplementation(libs.soloader)
    debugImplementation(libs.flipper.network.plugin)
    debugImplementation(libs.flipper.leakcanary.plugin)
    // release 和 alpha 使用 noop 版本
    releaseImplementation(libs.flipper.noop)
    // 为 alpha build type 添加 noop（如果配置存在）
    configurations.findByName("alphaImplementation")?.let {
        add("alphaImplementation", libs.flipper.noop)
    }

    // 阿里云 HTTP DNS
    implementation(libs.alicloud.android.httpdns)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
}
