# R8 rules for the release build.
#
# Most of the app needs none: Compose, Hilt, Room and Retrofit all ship their
# own consumer rules. What is listed here is the part R8 cannot see - the
# reflection kotlinx.serialization and Retrofit rely on - plus one rule that
# exists to protect a promise rather than a build.

# --- kotlinx.serialization ---------------------------------------------------
# Serializers are generated as nested classes and looked up reflectively, so a
# @Serializable class stripped of its serializer fails at runtime rather than at
# build time. Keeping the serializer members is the documented requirement.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# The bundled archive and the API responses are both deserialized by name, and
# renaming their fields would silently empty the app of Jerusalem.
-keep,allowobfuscation,allowshrinking class ps.hikayatalquds.data.asset.** { *; }
-keep,allowobfuscation,allowshrinking class ps.hikayatalquds.data.remote.**Dto { *; }
-keep class ps.hikayatalquds.data.remote.**Request { *; }
-keepclassmembers class ps.hikayatalquds.data.repository.PreferencesRecord { *; }
-keepclassmembers class ps.hikayatalquds.data.repository.ItineraryRecord { *; }
-keepclassmembers class ps.hikayatalquds.data.repository.StopRecord { *; }

# --- type-safe navigation ---------------------------------------------------
# Routes are @Serializable objects resolved by their class names.
-keep class ps.hikayatalquds.ui.navigation.** { *; }

# --- Retrofit ---------------------------------------------------------------
# Generic signatures on suspend functions are read reflectively.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# --- OkHttp -----------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Room -------------------------------------------------------------------
# Entities are constructed reflectively by the generated DAO implementations.
-keep class ps.hikayatalquds.data.local.*Entity { *; }

# --- WorkManager ------------------------------------------------------------
# Workers are instantiated by name from a JobService.
-keep class * extends androidx.work.CoroutineWorker { *; }

# --- keep the line numbers in a crash readable ------------------------------
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
