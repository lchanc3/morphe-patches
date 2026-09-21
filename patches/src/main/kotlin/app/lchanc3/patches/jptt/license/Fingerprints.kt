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
        // Only that the check is started from here, not how. PairIP renames this
        // call between its own versions: 3.8.4 calls initializeLicenseCheck(),
        // 3.8.5 calls checkLicense(Context).
        methodCall(definingClass = "Lcom/pairip/licensecheck/LicenseClient;"),
    ),
)
