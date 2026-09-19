package app.jptt.patches.shared

import app.morphe.patcher.Fingerprint

internal object JpttApplicationOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/joshua/jptt/JpttApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)
