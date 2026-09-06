plugins {
    id("com.android.application")
}

val appVersion = "1.0.0"

android {
    namespace = "my.MrxSiN.syncthingliveupdate"
    compileSdk = 36

    defaultConfig {
        applicationId = "my.MrxSiN.syncthingliveupdate"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = appVersion
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            merges += "META-INF/xposed/*"
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("SyncthingLiveUpdate-v$appVersion.apk")
        }
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
}
