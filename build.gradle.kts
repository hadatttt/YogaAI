// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    dependencies {
        classpath(libs.google.services)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin.v277)

    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}