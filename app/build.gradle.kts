plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "app.kwela"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "app.kwela"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.2"
        // Store listing links. Set these in gradle.properties; an empty value hides the matching row in Settings.
        fun link(name: String): String {
            val value = providers.gradleProperty(name).orNull.orEmpty()
            if (value.isBlank()) logger.warn("Store listing property '$name' is empty. Set it in gradle.properties before publishing.")
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
        buildConfigField("String", "API_BASE_URL", link("kwela.apiUrl"))
        buildConfigField("String", "PRIVACY_URL", link("kwela.privacyUrl"))
        buildConfigField("String", "TERMS_URL", link("kwela.termsUrl"))
        buildConfigField("String", "SUPPORT_EMAIL", link("kwela.supportEmail"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", link("kwela.googleWebClientId"))
        buildConfigField("String", "POSTHOG_API_KEY", link("kwela.posthogApiKey"))
        buildConfigField("String", "POSTHOG_HOST", link("kwela.posthogHost"))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val uploadStoreFile = providers.gradleProperty("kwela.uploadStoreFile").orNull
    val uploadStorePassword = providers.gradleProperty("kwela.uploadStorePassword").orNull
    val uploadKeyAlias = providers.gradleProperty("kwela.uploadKeyAlias").orNull
    val uploadKeyPassword = providers.gradleProperty("kwela.uploadKeyPassword").orNull
    val hasPlayUploadSigning = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { !it.isNullOrBlank() }

    if (hasPlayUploadSigning) {
        signingConfigs.create("playUpload") {
            storeFile = file(uploadStoreFile!!)
            storePassword = uploadStorePassword
            keyAlias = uploadKeyAlias
            keyPassword = uploadKeyPassword
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasPlayUploadSigning) signingConfigs.getByName("playUpload") else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.composables:icons-lucide-android:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.posthog:posthog-android:3.72.9")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
