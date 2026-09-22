plugins {
    id("com.android.application")
}

val appVersion = "1.1.1"

val envKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val envKeystoreAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
val envKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val envKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

android {
    namespace = "my.MrxSiN.syncthingliveupdate"
    compileSdk = 36

    /*
     * Release signing is supplied by the environment so that no credential ever
     * reaches version control. Local builds without those variables stay
     * unsigned instead of failing.
     */
    val releaseSigningConfig = if (
        !envKeystorePath.isNullOrBlank() &&
        !envKeystoreAlias.isNullOrBlank() &&
        !envKeystorePassword.isNullOrBlank() &&
        !envKeyPassword.isNullOrBlank() &&
        file(envKeystorePath).isFile
    ) {
        signingConfigs.create("release") {
            storeFile = file(envKeystorePath)
            storePassword = envKeystorePassword
            keyAlias = envKeystoreAlias
            keyPassword = envKeyPassword
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "io.github.mrxsin.syncthingliveupdate"
        minSdk = 36
        targetSdk = 36
        versionCode = 4
        versionName = appVersion

        /*
         * DexKit ships a native library per ABI. Android 16 is the module's
         * floor and requires a 64-bit device, so the 32-bit libraries would be
         * about half a megabyte that nothing can ever load.
         */
        ndk {
            abiFilters += setOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
            releaseSigningConfig?.let { signingConfig = it }
        }
    }

    packaging {
        jniLibs {
            /*
             * DexKit's library has to be loaded by absolute path, because inside a
             * host process System.loadLibrary searches the host's library path and
             * not the module's. That needs the library unpacked to a file at
             * install time, which is what the legacy packaging does; with the
             * modern packaging it stays compressed inside the APK and DexKitLibrary
             * has nothing to open.
             */
            useLegacyPackaging = true
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

    testOptions {
        unitTests {
            /*
             * The classes under test are plain Java. The few android.* types they
             * mention are never exercised by a test, so stubbing them out is
             * enough and no instrumentation or emulator is needed.
             */
            isReturnDefaultValues = true
            all { it.useJUnitPlatform() }
        }
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

    /*
     * Finds the hooked host methods by shape rather than by name, so an upstream
     * rename does not silently disable a feature. Ships a small native library
     * per ABI; see DexKitResolver for how and when it is used.
     */
    implementation("org.luckypray:dexkit:2.0.6")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
