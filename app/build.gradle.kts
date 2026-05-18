import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.android)

    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.phonecall.dialcontacts.calldialer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.phonecall.dialcontacts.calldialer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //lifeCycleScopes
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    //UI size
    implementation("com.intuit.sdp:sdp-android:1.1.1")

    //Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")

    //for viewModels()
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")

    //Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //Room Database
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    //for crop image
    implementation("com.github.yalantis:ucrop:2.2.2")

    //load lotti file
    implementation("com.airbnb.android:lottie:6.4.0")

    //FaceBook
    implementation("com.facebook.android:facebook-android-sdk:18.2.3")
    implementation("com.facebook.android:audience-network-sdk:6.21.0")
    implementation("com.facebook.infer.annotation:infer-annotation:0.18.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //google
    implementation("com.google.android.gms:play-services-ads:23.3.0")

    //firebase
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    //gson
    implementation("com.google.code.gson:gson:2.13.1")

    //onesignal
    implementation("com.onesignal:OneSignal:5.1.6")

    //PostHog
    implementation("com.posthog:posthog-android:3.+")

    //for encrypted & decrypted
    implementation(libs.jersey.core)

}