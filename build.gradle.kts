import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidLegacyKapt) apply false
    alias(libs.plugins.kotlinParcelize) apply false
    alias(libs.plugins.androidKsp) apply false
    alias(libs.plugins.gradleVersionsPlugin) apply false
    alias(libs.plugins.hiltAndroid) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xlint:-options")
    }
}
