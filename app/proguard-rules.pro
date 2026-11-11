# ==============================================================================
# Velocita R8 / ProGuard Strict Configuration (< 3MB Binary Footprint Target)
# ==============================================================================

# Strip Kotlin metadata and Intrinsics null-checks for smaller DEX size
-dontwarn kotlin.reflect.**
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(java.lang.Object);
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
}

# Completely remove all android.util.Log calls in release artifact (NFR 6.3)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Aggressive R8 class repackaging and optimization
-repackageclasses 'com.velocita.a'
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-optimizations !code/allocation/variable
