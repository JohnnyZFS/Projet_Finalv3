// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    repositories {
        google()
        mavenCentral()
        jcenter()  // Jcenter est obsolète, mais certains anciens paquets peuvent encore l'utiliser
    }

    dependencies {
        // Add or update the Android Gradle plugin version here
        classpath("com.android.tools.build:gradle:8.3.2") // or higher version

    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
