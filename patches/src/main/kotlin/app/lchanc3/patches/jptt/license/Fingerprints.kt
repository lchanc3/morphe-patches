package app.lchanc3.patches.jptt.license

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

/**
 * `LicenseContentProvider.onCreate()`, the only thing that starts PairIP's
 * license check. It runs before any of the app's own code, because a
 * ContentProvider is created during application startup.
 */
internal object LicenseContentProviderOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseContentProvider;",
    name = "onCreate",
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(smali = "Lcom/pairip/licensecheck/LicenseClient;->initializeLicenseCheck()V"),
    ),
)
