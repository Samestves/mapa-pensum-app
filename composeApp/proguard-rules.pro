# El índice de carreras se lee con kotlinx.serialization: que R8 no quite
# ni renombre los serializadores que genera el plugin para mpensum.datos.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class mpensum.datos.**$$serializer { *; }
-keepclassmembers class mpensum.datos.** {
    *** Companion;
}
-keepclasseswithmembers class mpensum.datos.** {
    kotlinx.serialization.KSerializer serializer(...);
}
