# AttendSmartly (2026)
# © Animesh Gupta — github.com/agupta07505
# Licensed under the GNU GPL v3 License
#
# ProGuard & R8 Optimization and Obfuscation Rules for AttendSmartly

# ==============================================================================
# 1. Stack Traces & Annotations Preservation
# ==============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==============================================================================
# 2. Application Entry Points & Components
# ==============================================================================
-keep class com.agupta07505.attendsmartly.AttendSmartlyApplication { *; }
-keep class com.agupta07505.attendsmartly.MainActivity { *; }
-keep class com.agupta07505.attendsmartly.receiver.** { *; }

# ==============================================================================
# 3. Room Database & Local Entities
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep class com.agupta07505.attendsmartly.data.local.dao.** { *; }
-keep class com.agupta07505.attendsmartly.data.local.entity.** { *; }
-dontwarn androidx.room.paging.**

# ==============================================================================
# 4. Domain Models & JSON Serialization (Gson / Moshi)
# ==============================================================================
-keep class com.agupta07505.attendsmartly.domain.model.** { *; }
-keep class com.agupta07505.attendsmartly.util.AttendSmartlyBackup { *; }
-keep class com.agupta07505.attendsmartly.util.TimetableSharePackage { *; }
-keep class com.agupta07505.attendsmartly.util.ExportImportUtils { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Gson Rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.* <fields>;
}

# Moshi Rules
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-dontwarn com.squareup.moshi.**

# ==============================================================================
# 5. WorkManager & Background Tasks
# ==============================================================================
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.agupta07505.attendsmartly.worker.** { *; }

# ==============================================================================
# 6. Retrofit & OkHttp Networking
# ==============================================================================
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep class com.agupta07505.attendsmartly.data.remote.** { *; }

# ==============================================================================
# 7. Jetpack Compose UI
# ==============================================================================
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
-dontwarn androidx.compose.**

# ==============================================================================
# 8. DataStore Preferences
# ==============================================================================
-keep class androidx.datastore.** { *; }
