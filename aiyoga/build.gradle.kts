plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
}

apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.hadat.aiyoga"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.hadat.aiyoga"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters.add("arm64-v8a")
        }

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }
    androidResources {
        noCompress.add("tflite")
        noCompress.add("task")
    }

    packaging {
        resources {
            pickFirsts.add("lib/**/libtensorflowlite_jni.so")
            pickFirsts.add("lib/**/libtensorflowlite_gpu_jni.so")
        }
    }
}

dependencies {
    implementation(project(":base"))
//    implementation(project(":dailycheckin"))
//    implementation(project(":slotmachinegame"))
    implementation("com.airbnb.android:lottie:6.0.0")

    // Android Core
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.lifecycle.viewmodel.ktx)

    // Google Services
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    // Navigation & Permissions
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.ui.ktx)
    api(libs.permissionx)

    // UI Helpers
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Auth & Social
    implementation("com.facebook.android:facebook-login:16.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Utils
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    val tfliteVersion = "2.14.0"

    implementation("org.tensorflow:tensorflow-lite:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:$tfliteVersion")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:$tfliteVersion")

    // MediaPipe (Dùng để lấy Pose Landmarks)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.github.qamarelsafadi:CurvedBottomNavigation:0.1.3")
    // help
    implementation("com.github.takusemba:spotlight:2.0.3")
    // room
    val room_version = "2.6.1"
    ksp("androidx.room:room-compiler:$room_version")
    api("com.google.code.gson:gson:2.10.1")
    implementation("androidx.room:room-ktx:$room_version")
    //circle progress
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    //cloudiary
    implementation("com.cloudinary:cloudinary-android:2.3.1")
    //char
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    implementation("com.tbuonomo:dotsindicator:4.3")
    //ucrop
    implementation(project(":ucrop"))
    //photo zoom
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
}