package app.jptt.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {

    /** Extension classes merged into the app by [extensionHookPatch]. */
    const val EXTENSION_CONTEXT_CLASS = "Lapp/jptt/extension/JpttContext;"
    const val EXTENSION_PRELOAD_CLASS = "Lapp/jptt/extension/PreloadArticleImagesPatch;"
    const val EXTENSION_FILE_PROVIDER_CLASS = "Lapp/jptt/extension/FileProviderAuthorityPatch;"
    const val EXTENSION_TERMINAL_ESCAPE_CLASS = "Lapp/jptt/extension/TerminalEscapePatch;"
    const val EXTENSION_SEARCH_HISTORY_CLASS = "Lapp/jptt/extension/SearchHistoryLayoutPatch;"

    const val ARTICLE_FRAGMENT_CLASS = "Lcom/joshua/jptt/ArticleFragment;"
    const val BOARD_FRAGMENT_CLASS = "Lcom/joshua/jptt/BoardFragment;"
    const val DB_HELPER_CLASS = "Lcom/joshua/jptt/DBHelper;"
    const val JSOCKET_SIMPLE_CLASS = "Lcom/joshua/jptt/JSocketSimple;"

    /** Type prefix shared by JSocket, JSocketSimple and their inner classes. */
    const val JSOCKET_CLASS_PREFIX = "Lcom/joshua/jptt/JSocket"

    val COMPATIBILITY_JPTT = Compatibility(
        name = "JPTT",
        packageName = "com.joshua.jptt",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFCFCFC,
        targets = listOf(
            // The version these patches were written against.
            AppTarget(version = "3.8.4"),
            // Later versions are worth trying: nothing in JPTT is obfuscated, so
            // the fingerprints keep matching unless the code itself changes.
            AppTarget(version = null, isExperimental = true),
        ),
    )
}
