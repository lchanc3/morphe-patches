package app.jptt.patches.license

import app.jptt.patches.shared.Constants.COMPATIBILITY_JPTT
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableLicenseCheckPatch = bytecodePatch(
    name = "Disable Play license check",
    description = "Stops PairIP's license check from running. It verifies with the Play Store " +
        "that the install is the one Google shipped, which a re-signed build never is, so " +
        "without this the app shows \"Something went wrong\" on launch and closes itself.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    execute {
        // A ContentProvider is created during application startup, so this runs
        // before anything else. Returning true straight away reports the provider
        // as created without starting the check, which also stops the repeated
        // background checks and the System.exit() they can trigger.
        //
        // The method has two local registers, so v0 is free.
        LicenseContentProviderOnCreateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
