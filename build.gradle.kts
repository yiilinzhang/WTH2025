// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level build file for configuration common to all modules
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0") // Needed for Firebase
    }
}
