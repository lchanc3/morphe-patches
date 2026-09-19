import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
}

description = "Stubs for classes that already exist inside the JPTT APK."

configure<LibraryExtension> {
    namespace = "app.jptt.stub"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }
}
