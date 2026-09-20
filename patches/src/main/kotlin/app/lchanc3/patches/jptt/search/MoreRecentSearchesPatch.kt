package app.lchanc3.patches.jptt.search

import app.lchanc3.patches.jptt.shared.Constants.COMPATIBILITY_JPTT
import app.lchanc3.patches.jptt.shared.Constants.DB_HELPER_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intSliderOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val moreRecentSearchesPatch = bytecodePatch(
    name = "More recent searches",
    description = "Shows more of your recent search keywords in the article search dialog.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    val boardKeywordCount by intSliderOption(
        key = "boardKeywordCount",
        min = 1,
        max = 50,
        default = 15,
        title = "最近看板搜尋 entries",
        description = "Recent keywords used on the board you are currently in. " +
            "JPTT shows 5 of them.",
    )

    val allKeywordCount by intSliderOption(
        key = "allKeywordCount",
        min = 1,
        max = 100,
        default = 30,
        title = "最近搜尋 entries",
        description = "Recent keywords used on any board. JPTT shows 15 of them.",
    )

    execute {
        val method = ShowSearchDialogFingerprint.method

        // The two calls, in source order: first the board specific history, then
        // the history across all boards.
        val callIndices = method.instructions.withIndex().filter { (_, instruction) ->
            if (instruction.opcode != Opcode.INVOKE_STATIC) return@filter false

            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference != null &&
                reference.definingClass == DB_HELPER_CLASS &&
                reference.name == "getBoardHistory" &&
                reference.parameterTypes.size == 5
        }.map { it.index }

        val counts = listOf(boardKeywordCount!!, allKeywordCount!!)
        if (callIndices.size != counts.size) {
            throw PatchException(
                "Expected ${counts.size} DBHelper.getBoardHistory calls in " +
                    "showSearchDialog, found ${callIndices.size}",
            )
        }

        // The row limit is the fifth argument of the call. Overwrite that register
        // right before the call instead of editing the original constant, which is
        // a const/4 in one case and a const/16 in the other.
        //
        // Walk backwards so the earlier index is still correct after the insert.
        callIndices.zip(counts).reversed().forEach { (index, count) ->
            val limitRegister = method.getInstruction<FiveRegisterInstruction>(index).registerG

            method.addInstruction(index, "const/16 v$limitRegister, $count")
        }
    }
}
