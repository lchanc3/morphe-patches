rootProject.name = "jptt-morphe-patches"

pluginManagement {
    // Gradle compiles this block in isolation from the rest of the file, so the
    // GitHub CLI fallback has to be defined inline.
    val gh: (List<String>) -> String? = { args ->
        try {
            val exe = if (System.getProperty("os.name").startsWith("Windows")) "gh.exe" else "gh"
            val process = ProcessBuilder(listOf(exe) + args).start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.errorStream.close()
            if (process.waitFor() == 0 && output.isNotEmpty()) output else null
        } catch (_: Exception) {
            null
        }
    }

    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                // Any GitHub account works; the token only needs the `read:packages`
                // scope. Looked up in ~/.gradle/gradle.properties, then the
                // environment, then whatever `gh` is logged in as.
                val missing = "No GitHub credentials for maven.pkg.github.com. " +
                    "Either run `gh auth refresh -s read:packages`, or put gpr.user " +
                    "and gpr.key in ~/.gradle/gradle.properties. Gradle only reports " +
                    "this as an unhelpful IllegalArgumentException otherwise."

                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: gh(listOf("api", "user", "--jq", ".login"))
                    ?: error(missing)
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: gh(listOf("auth", "token"))
                    ?: error(missing)
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "1.3.4"
}

settings {
    extensions {
        // Must be an absolute path, otherwise extensions in subfolders fail to
        // find the config.
        proguardFiles(rootProject.projectDir.resolve("extensions/proguard-rules.pro").toString())
    }
}
