package app.jptt.patches.search

import app.jptt.patches.shared.Constants.COMPATIBILITY_JPTT
import app.jptt.patches.shared.Constants.EXTENSION_SEARCH_HISTORY_CLASS
import app.jptt.patches.shared.extensionHookPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val wrapRecentSearchesPatch = bytecodePatch(
    name = "Wrap recent searches",
    description = "Lays the 最近看板搜尋 / 最近搜尋 keywords out over several lines in the " +
        "article search dialog, instead of one line you have to scroll sideways through. " +
        "A list longer than a quarter of the screen scrolls within its own strip.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    dependsOn(extensionHookPatch)

    execute {
        // p1 is the container the keywords are about to be added to. The
        // extension swaps the HorizontalScrollView around it for a vertical one
        // holding a flow layout, and hands back the container to fill instead.
        AddSearchHistoryListToLayoutFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_SEARCH_HISTORY_CLASS->useFlowLayout(Landroid/view/ViewGroup;)Landroid/view/ViewGroup;
                move-result-object p1
            """,
        )
    }
}
