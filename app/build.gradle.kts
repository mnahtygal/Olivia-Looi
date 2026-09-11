import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

fun configuredValue(environmentName: String, propertyName: String, defaultValue: String = "") =
    providers.environmentVariable(environmentName).orNull
        ?: localProperties.getProperty(propertyName, defaultValue)

fun String.asBuildConfigString() =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val jarvisBaseUrl = configuredValue(
    environmentName = "JARVIS_BASE_URL",
    propertyName = "jarvis.baseUrl",
    defaultValue = "https://10.0.0.213",
)
val jarvisHandheldToken = configuredValue(
    environmentName = "JARVIS_HANDHELD_TOKEN",
    propertyName = "jarvis.handheldToken",
)

android {
    namespace = "com.nahtygal.olivialooi"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nahtygal.olivialooi"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "v0.27"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "JARVIS_BASE_URL", jarvisBaseUrl.asBuildConfigString())
        buildConfigField(
            "String",
            "JARVIS_HANDHELD_TOKEN",
            jarvisHandheldToken.asBuildConfigString(),
        )
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
        buildConfig = true
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
