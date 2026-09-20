package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.loadPatchesFromJar
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess

/**
 * Applies the built bundle to a JPTT APK the way Morphe Manager would, so a
 * patch whose fingerprint stopped matching fails here rather than on your phone.
 * Run it with `./gradlew verifyAgainstApk`.
 *
 * Nothing is signed or installed: this stops at the patched dex files, which are
 * left in the output directory to disassemble.
 *
 *     args[0] = APK to patch, empty when none was found
 *     args[1] = directory for the patched dex files
 */

/** `base.apk` of JPTT 3.8.4, the version these patches were written against. */
private const val KNOWN_APK_SHA256 =
    "7b65298d00d8219d49b8d4dfac739f2bf63b6187a00f7697e3c67861fcf2d605"

private val MISSING_APK = """
    No APK to verify against. Pass one with -Papk=<path>, point ${'$'}JPTT_APK at one,
    or leave one in the project root. It is deliberately not in the repository.

    On a phone that still has JPTT 3.8.4 installed, its base.apk is byte for byte
    the APK these patches were written against:

        adb pull "${'$'}(adb shell pm path com.joshua.jptt | grep base.apk | sed 's/package://' | tr -d '\r')" JPTT_3.8.4.apk
""".trimIndent()

fun main(args: Array<String>) {
    val apk = args[0].takeIf { it.isNotEmpty() }?.let(::File)
    if (apk == null || !apk.isFile) {
        println(MISSING_APK)
        exitProcess(1)
    }

    val bundle = File("build/libs/")
        .listFiles { file ->
            file.name.endsWith(".mpp") &&
                !file.name.contains("javadoc") &&
                !file.name.contains("sources")
        }
        ?.firstOrNull()
        ?: error("No bundle in patches/build/libs. Run `./gradlew buildAndroid` first.")

    val outputDirectory = File(args[1]).apply { mkdirs() }
    val temporaryFiles = File(outputDirectory, "tmp").apply { mkdirs() }

    println("bundle: ${bundle.name}")
    println("apk:    ${apk.absolutePath}")

    val digest = apk.sha256()
    if (digest != KNOWN_APK_SHA256) {
        println("note:   not the 3.8.4 APK these patches were written against ($digest)")
    }

    var failed = 0

    Patcher(PatcherConfig(apkFile = apk, temporaryFilesPath = temporaryFiles)).use { patcher ->
        patcher += loadPatchesFromJar(setOf(bundle)).toSet()

        runBlocking {
            patcher().collect { result ->
                val name = result.patch.name ?: result.patch.toString()
                val exception = result.exception
                if (exception == null) {
                    println("  ok      $name")
                } else {
                    failed++
                    println("  FAILED  $name: $exception")
                    exception.printStackTrace()
                }
            }
        }

        // Compiling the dex is part of the check: it is where the smali the
        // patches assembled has to survive being written back out.
        patcher.get().dexFiles.forEach { dexFile ->
            File(outputDirectory, dexFile.name).outputStream().use { dexFile.stream.copyTo(it) }
        }
    }

    if (failed != 0) {
        println("$failed patch(es) failed")
        exitProcess(1)
    }
    println("all patches applied, dex written to $outputDirectory")
}

private fun File.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(readBytes())
        .joinToString("") { "%02x".format(it) }
