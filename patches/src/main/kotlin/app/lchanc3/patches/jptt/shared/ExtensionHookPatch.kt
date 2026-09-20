package app.lchanc3.patches.jptt.shared

import app.lchanc3.patches.jptt.shared.Constants.EXTENSION_CONTEXT_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

/**
 * Merges the extension classes into the app and hands them the application
 * context. Has no name on purpose: it is a dependency, not something to pick in
 * Morphe Manager.
 */
internal val extensionHookPatch = bytecodePatch {

    extendWith("extensions/jptt.mpe")

    execute {
        // `p0` is the JpttApplication instance. Injecting before super.onCreate()
        // is fine, the base context is attached before onCreate runs.
        JpttApplicationOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CONTEXT_CLASS->setApplication(Landroid/app/Application;)V",
        )
    }
}
