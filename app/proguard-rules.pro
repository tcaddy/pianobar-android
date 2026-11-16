# Preserve line numbers and anonymized source file attribute for Play Console crash deobfuscation
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Kotlin metadata and model classes
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep class com.aistudio.pianobar.kxuvzq.model.** { *; }

# Keep AndroidManifest entry points cleanly
-keep class com.aistudio.pianobar.kxuvzq.PianobarApp { *; }
-keep class com.aistudio.pianobar.kxuvzq.MainActivity { *; }
-keep class com.aistudio.pianobar.kxuvzq.service.PianobarPlaybackService { *; }

# Suppress harmless compile-only annotation warnings from transitive dependencies
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
