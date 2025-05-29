plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sneaker_shop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sneaker_shop"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/NOTICE.md",
                "/META-INF/LICENSE.md",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("org.jsoup:jsoup:1.14.3")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("jp.wasabeef:recyclerview-animators:4.0.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("org.apmem.tools:layouts:1.10@aar")
    implementation("com.yandex.android:maps.mobile:4.6.1-lite")
    implementation("androidx.work:work-runtime:2.7.1")
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("at.favre.lib:bcrypt:0.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}