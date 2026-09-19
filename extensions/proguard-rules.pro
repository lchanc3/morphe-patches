# Applied to the extension before it is turned into jptt.mpe.
#
# Without this the extension dex carries the whole Kotlin standard library,
# which JPTT already ships -- merging a second copy into the APK is both 2 MB of
# dead weight and a duplicate class hazard.

-dontobfuscate
-dontoptimize
-keepattributes *

-keep class app.jptt.** {
  *;
}

# Proguard can strip away kotlin intrinsics methods that are used by extension
# Kotlin code. Unclear why.
-keep class kotlin.jvm.internal.Intrinsics {
    public static *;
}

-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.lang.model.element.Modifier
