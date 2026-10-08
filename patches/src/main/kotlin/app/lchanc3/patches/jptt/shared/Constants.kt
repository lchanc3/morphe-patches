package app.lchanc3.patches.jptt.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {

    /** Extension classes merged into the app by [extensionHookPatch]. */
    const val EXTENSION_CONTEXT_CLASS = "Lapp/lchanc3/extension/jptt/JpttContext;"
    const val EXTENSION_PRELOAD_CLASS = "Lapp/lchanc3/extension/jptt/PreloadArticleImagesPatch;"
    const val EXTENSION_FILE_PROVIDER_CLASS = "Lapp/lchanc3/extension/jptt/FileProviderAuthorityPatch;"
    const val EXTENSION_TERMINAL_ESCAPE_CLASS = "Lapp/lchanc3/extension/jptt/TerminalEscapePatch;"
    const val EXTENSION_SEARCH_HISTORY_CLASS = "Lapp/lchanc3/extension/jptt/SearchHistoryLayoutPatch;"
    const val EXTENSION_SEARCH_HISTORY_ACTIONS_CLASS = "Lapp/lchanc3/extension/jptt/SearchHistoryActionsPatch;"
    const val EXTENSION_IMAGE_LINK_CLASS = "Lapp/lchanc3/extension/jptt/ImageLinkPatch;"
    const val EXTENSION_SETTINGS_TAB_CLASS = "Lapp/lchanc3/extension/jptt/SettingsTab;"
    const val EXTENSION_PATCH_SETTINGS_CLASS = "Lapp/lchanc3/extension/jptt/PatchSettings;"
    const val EXTENSION_RECONNECT_CLASS = "Lapp/lchanc3/extension/jptt/ReconnectPatch;"
    const val EXTENSION_LOGIN_BAN_CLASS = "Lapp/lchanc3/extension/jptt/LoginBanPatch;"
    const val EXTENSION_BOT_BLOCK_CLASS = "Lapp/lchanc3/extension/jptt/BotBlockPatch;"
    const val EXTENSION_ANIMATED_IMAGE_CLASS = "Lapp/lchanc3/extension/jptt/AnimatedImagePatch;"
    const val EXTENSION_IMAGE_MEMORY_CACHE_CLASS = "Lapp/lchanc3/extension/jptt/ImageMemoryCachePatch;"

    const val ARTICLE_FRAGMENT_CLASS = "Lcom/joshua/jptt/ArticleFragment;"
    const val ARTICLE_URL_TASK_CLASS = "Lcom/joshua/jptt/ArticleUrlAsyncTask;"
    const val BOARD_FRAGMENT_CLASS = "Lcom/joshua/jptt/BoardFragment;"
    const val CONTENT_PAGER_ADAPTER_CLASS = "Lcom/joshua/jptt/ContentPagerAdapter;"
    const val DB_HELPER_CLASS = "Lcom/joshua/jptt/DBHelper;"
    const val MAIN_ACTIVITY_CLASS = "Lcom/joshua/jptt/MainActivity;"
    const val PICTURE_VIEW_CLASS = "Lcom/joshua/jptt/ui/PictureView;"
    const val SETTINGS_PAGER_ADAPTER_CLASS = "Lcom/joshua/jptt/SettingsPagerAdapter;"
    const val SPLASH_ACTIVITY_CLASS = "Lcom/joshua/jptt/SplashActivity;"
    const val UTIL_CLASS = "Lcom/joshua/jptt/Util;"
    const val JSOCKET_CLASS = "Lcom/joshua/jptt/JSocket;"
    const val JSOCKET_SIMPLE_CLASS = "Lcom/joshua/jptt/JSocketSimple;"

    /** Type prefix shared by JSocket, JSocketSimple and their inner classes. */
    const val JSOCKET_CLASS_PREFIX = "Lcom/joshua/jptt/JSocket"

    /** The JPTT release new patches are written and verified against. */
    private val LATEST_TARGET = AppTarget(version = "3.9.2")

    /**
     * Says out loud that untested versions are worth trying, since nothing in
     * JPTT is obfuscated and the fingerprints keep matching unless the code
     * itself changes. Manager drops null versions when it collects both the
     * supported and the experimental set, so this changes nothing there; it only
     * shows up as the 🧪 column in the README.
     */
    private val UNTESTED_TARGET = AppTarget(version = null, isExperimental = true)

    /**
     * For the patches that were written before [LATEST_TARGET], which keep
     * working on every release they were verified against.
     *
     * Morphe Manager builds its supported set from these version strings alone,
     * so anything not listed lands in "incompatible" and has to be patched
     * through the unsupported-version dialog. A new JPTT release is added here
     * once these patches have been run against it with `verifyAgainstApk`, and
     * the older ones stay. Newest first, as Morphe asks.
     */
    val COMPATIBILITY_JPTT = jptt(
        LATEST_TARGET,
        AppTarget(version = "3.8.5"),
        AppTarget(version = "3.8.4"),
        UNTESTED_TARGET,
    )

    /**
     * For new patches: they are only written and verified against the latest
     * JPTT release, so they claim nothing older.
     */
    val COMPATIBILITY_JPTT_LATEST = jptt(LATEST_TARGET, UNTESTED_TARGET)

    private fun jptt(vararg targets: AppTarget) = Compatibility(
        name = "JPTT",
        packageName = "com.joshua.jptt",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFCFCFC,
        targets = targets.toList(),
    )
}
