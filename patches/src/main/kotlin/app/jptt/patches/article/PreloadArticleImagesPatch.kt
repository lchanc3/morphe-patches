package app.jptt.patches.article

import app.jptt.patches.shared.Constants.ARTICLE_FRAGMENT_CLASS
import app.jptt.patches.shared.Constants.COMPATIBILITY_JPTT
import app.jptt.patches.shared.Constants.EXTENSION_PRELOAD_CLASS
import app.jptt.patches.shared.JpttApplicationOnCreateFingerprint
import app.jptt.patches.shared.extensionHookPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intSliderOption

@Suppress("unused")
val preloadArticleImagesPatch = bytecodePatch(
    name = "Preload article images",
    description = "Downloads every image of the article you are reading up front, " +
        "instead of starting each download only once you scroll it into view. " +
        "Obeys the app's own 自動載入圖片 / 只在 Wi-Fi 下載入 settings.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_JPTT)

    dependsOn(extensionHookPatch)

    val preloadLimit by intSliderOption(
        key = "preloadLimit",
        min = 5,
        max = 300,
        default = 60,
        step = 5,
        title = "Images to preload per article",
        description = "How many of an article's images are downloaded ahead of time. " +
            "Images past this limit still load the normal way when scrolled to.",
    )

    execute {
        // Configure the extension. onCreate has six local registers, so v0 is free
        // here and is overwritten by the original code right after.
        JpttApplicationOnCreateFingerprint.method.addInstructions(
            0,
            """
                const/16 v0, $preloadLimit
                invoke-static { v0 }, $EXTENSION_PRELOAD_CLASS->setMaxImagesPerArticle(I)V
            """,
        )

        // Both methods run whenever the article's item list changes. getAllPicUrl()
        // returns every image URL known so far; the extension skips the ones it has
        // already requested.
        //
        // getAllPicUrl() dereferences `adapter`, and notifyDataSetChanged() is
        // reached with a null adapter (it null checks it), so check it here too.
        //
        // Both methods have at least one local register, so v0 is free at index 0
        // and the original code overwrites it right after.
        listOf(
            ArticleFragmentNotifyDataSetChangedFingerprint,
            ArticleFragmentShowListPartialFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, $ARTICLE_FRAGMENT_CLASS->adapter:Lcom/joshua/jptt/ArticleFragment${'$'}IntextAdapter;
                    if-eqz v0, :no_adapter
                    invoke-virtual { p0 }, $ARTICLE_FRAGMENT_CLASS->getAllPicUrl()Ljava/util/ArrayList;
                    move-result-object v0
                    invoke-static { v0 }, $EXTENSION_PRELOAD_CLASS->preload(Ljava/util/ArrayList;)V
                    :no_adapter
                    nop
                """,
            )
        }
    }
}
