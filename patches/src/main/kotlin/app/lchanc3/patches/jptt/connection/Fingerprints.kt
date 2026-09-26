package app.lchanc3.patches.jptt.connection

import app.lchanc3.patches.jptt.shared.Constants.JSOCKET_CLASS
import app.lchanc3.patches.jptt.shared.Constants.MAIN_ACTIVITY_CLASS
import app.morphe.patcher.Fingerprint

/**
 * `MainActivity.onResume()`, which runs after `onStart()` has set
 * `activityIsActive`, so the reconnect the hook triggers is allowed to proceed.
 */
internal object MainActivityOnResumeFingerprint : Fingerprint(
    definingClass = MAIN_ACTIVITY_CLASS,
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
)

/**
 * `JSocket.processReadCharacter(char)`, which reads the login screens and
 * answers them. Found by the one login refusal it already knows.
 */
internal object ProcessReadCharacterFingerprint : Fingerprint(
    definingClass = JSOCKET_CLASS,
    name = "processReadCharacter",
    returnType = "Z",
    parameters = listOf("C"),
    strings = listOf("登入太頻繁"),
)
