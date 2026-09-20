package app.lchanc3.patches.jptt.cache

import app.lchanc3.patches.jptt.shared.Constants.COMPATIBILITY_JPTT
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intSliderOption
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val increaseImageCacheSizePatch = bytecodePatch(
    name = "Increase image cache size",
    description = "Raises the image cache limit so images you have already seen are " +
        "not downloaded again when you scroll back.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    val cacheSizeMb by intSliderOption(
        key = "cacheSizeMb",
        min = 40,
        max = 4096,
        default = 512,
        step = 8,
        title = "Image cache size (MB)",
        description = "Maximum disk space the image cache may use.",
    )

    execute {
        val match = DiskCacheConfigBuilderFingerprint.instructionMatches.first()
        val register = DiskCacheConfigBuilderFingerprint.method
            .getInstruction<OneRegisterInstruction>(match.index).registerA

        val sizeInBytes = cacheSizeMb!!.toLong() * 1024L * 1024L

        DiskCacheConfigBuilderFingerprint.method.replaceInstruction(
            match.index,
            "const-wide v$register, 0x${sizeInBytes.toString(16)}L",
        )
    }
}
