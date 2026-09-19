package app.jptt.patches.search

import app.jptt.patches.shared.Constants.BOARD_FRAGMENT_CLASS
import app.morphe.patcher.Fingerprint

/**
 * `BoardFragment.showSearchDialog()`, which fills the two "最近搜尋" strips of the
 * search dialog with two calls to `DBHelper.getBoardHistory(...)`.
 */
internal object ShowSearchDialogFingerprint : Fingerprint(
    definingClass = BOARD_FRAGMENT_CLASS,
    name = "showSearchDialog",
    returnType = "V",
    parameters = emptyList(),
)
