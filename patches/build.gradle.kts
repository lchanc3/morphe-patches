group = "app.jptt"

patches {
    about {
        name = "JPTT Patches"
        description = "Personal quality-of-life patches for JPTT"
        source = "git@github.com:lchanc3/morphe-patches.git"
        author = "lchanc3"
        contact = "na"
        website = "https://github.com/lchanc3/morphe-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Regenerate patches-list.json from the built bundle"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }
}
