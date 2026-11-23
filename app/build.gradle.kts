import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.3.21"
}

// Release signing is only configured when all credentials are provided by CI.
// No keystore or password belongs in source control.
val releaseKeystorePath = System.getenv("NETSPEED_RELEASE_KEYSTORE")
val releaseStorePassword = System.getenv("NETSPEED_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("NETSPEED_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("NETSPEED_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.netspeed.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.netspeed.app"
        minSdk = 31
        targetSdk = 31
        versionCode = 3
        versionName = "1.0.0"
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("netspeedRelease") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("netspeedRelease")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
