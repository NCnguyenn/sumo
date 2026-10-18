plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val sumoDistribution = providers.gradleProperty("sumoDistribution").orNull == "true"
val sumoVersionCode = providers.gradleProperty("sumoVersionCode").orNull?.toIntOrNull() ?: 1
val sumoSigning = if (sumoDistribution) {
    require(sumoVersionCode >= 2) { "Distribution needs -PsumoVersionCode=<increasing value >=2>" }
    listOf("SUMO_KEYSTORE", "SUMO_KEY_ALIAS", "SUMO_STORE_PASSWORD", "SUMO_KEY_PASSWORD").associateWith {
        requireNotNull(providers.environmentVariable(it).orNull?.takeIf(String::isNotBlank)) { "Distribution signing is missing $it; emulator key will not be used" }
    }.also {
        val key = file(it.getValue("SUMO_KEYSTORE")).canonicalFile
        require(key.isFile && !key.toPath().startsWith(rootDir.canonicalFile.toPath())) { "Distribution keystore must exist outside the repository" }
    }
} else emptyMap()

android {
    namespace = "com.aistudy.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.aistudy.myapplication"
        minSdk = 36
        targetSdk = 37
        versionCode = sumoVersionCode
        versionName = if (sumoDistribution) "0.1.0-test.$sumoVersionCode" else "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (sumoDistribution) {
        val stableTest = signingConfigs.create("stableTest") {
            storeFile = file(sumoSigning.getValue("SUMO_KEYSTORE"))
            keyAlias = sumoSigning.getValue("SUMO_KEY_ALIAS")
            storePassword = sumoSigning.getValue("SUMO_STORE_PASSWORD")
            keyPassword = sumoSigning.getValue("SUMO_KEY_PASSWORD")
        }
        buildTypes.getByName("debug").signingConfig = stableTest
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
