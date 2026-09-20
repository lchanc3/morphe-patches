import com.android.build.api.dsl.ApplicationExtension

dependencies {
    // Classes that already live inside the patched APK. compileOnly, so none of
    // this ends up in jptt.mpe -- it only lets the code below compile against
    // the exact signatures JPTT 3.8.4 ships.
    compileOnly(project(":extensions:jptt:stub"))
}

configure<ApplicationExtension> {
    namespace = "app.lchanc3.extension.jptt"
    compileSdk = 36

    defaultConfig {
        // JPTT itself supports API 21+.
        minSdk = 21
    }
}
