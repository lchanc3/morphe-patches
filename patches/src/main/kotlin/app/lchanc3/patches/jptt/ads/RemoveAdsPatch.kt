package app.lchanc3.patches.jptt.ads

import app.lchanc3.patches.jptt.shared.Constants.COMPATIBILITY_JPTT
import app.lchanc3.patches.jptt.shared.JpttApplicationOnCreateFingerprint
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** SDK entry points whose only job is to start talking to an ad network. */
private val AD_SDK_INITIALISERS = listOf(
    "Lcom/google/android/gms/ads/MobileAds;" to "initialize",
    "Lcom/aotter/net/trek/TrekAds;" to "initialize",
)

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Stops the banner, the rows inside articles and lists, and the ad the " +
        "app falls back to when it thinks AdMob is blocked. No ad is requested at all, so " +
        "nothing is downloaded and nothing is reported.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    execute {
        emptyBanner()
        silence(AddLocalAdFingerprint, AddTAMediaAdFingerprint)
        silence(ArticleNativeAdLoadFingerprint, DigestNativeAdLoadFingerprint)
        dropSdkInitialisers()
    }
}

/**
 * The banner factory still builds its AdView, because three of its five callers
 * use what it returns without checking it for null. It just never asks for an ad
 * and hands back a view that is gone, which takes no space in the layouts it is
 * added to.
 */
private fun app.morphe.patcher.patch.BytecodePatchContext.emptyBanner() {
    val method = GetAdmobBannerAdFingerprint.method
    val instructions = method.implementation?.instructions
        ?: throw PatchException("Util.getAdmobBannerAd() has no body.")

    val loadIndex = instructions.indexOfFirst { instruction ->
        ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "loadAd"
    }
    if (loadIndex < 0) {
        throw PatchException("Util.getAdmobBannerAd() no longer calls loadAd().")
    }

    // The AdView is the first register of the call, the request the second; the
    // request is dead once the call is gone, so its register is free to borrow.
    val load = method.getInstruction<FiveRegisterInstruction>(loadIndex)
    val adView = load.registerC
    val scratch = load.registerD

    method.removeInstruction(loadIndex)
    method.addInstructions(
        loadIndex,
        """
            const/16 v$scratch, 0x8
            invoke-virtual { v$adView, v$scratch }, Landroid/view/View;->setVisibility(I)V
        """,
    )
}

/** Turns each method into a no-op at its first instruction. */
private fun app.morphe.patcher.patch.BytecodePatchContext.silence(vararg fingerprints: Fingerprint) {
    fingerprints.forEach { fingerprint ->
        fingerprint.method.addInstruction(0, "return-void")
    }
}

/**
 * Neither SDK is asked for an ad any more, so neither needs starting. Only the
 * calls go: what was built to pass to them is left to be dead code, which is
 * cheaper than working out what else those registers were for.
 */
private fun app.morphe.patcher.patch.BytecodePatchContext.dropSdkInitialisers() {
    val method = JpttApplicationOnCreateFingerprint.method
    val instructions = method.implementation?.instructions
        ?: throw PatchException("JpttApplication.onCreate() has no body.")

    val indices = instructions.withIndex().filter { (_, instruction) ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        reference != null && AD_SDK_INITIALISERS.any { (definingClass, name) ->
            reference.definingClass == definingClass && reference.name == name
        }
    }.map { it.index }

    if (indices.isEmpty()) {
        throw PatchException(
            "JpttApplication.onCreate() starts neither ad SDK any more: " +
                AD_SDK_INITIALISERS.joinToString { "${it.first}->${it.second}" },
        )
    }

    // Descending, so an earlier index is still correct after a removal.
    indices.sortedDescending().forEach { index -> method.removeInstruction(index) }
}
