# R8 rules for the release build. kotlinx.serialization and androidx ship their own consumer rules;
# these cover the two places Finio itself uses reflection.

# model/Model.kt `wireName(enum)` reads the @SerialName annotation off the enum constant's field,
# looked up by its source name — so enum fields must keep their names and their annotations.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep @interface kotlinx.serialization.SerialName
-keepclassmembers enum com.slowatcoding.finio.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ui/navigation/FinioNavigator.kt tells object routes from data-class routes via their INSTANCE
# field, and Navigation-Compose resolves routes through their generated serializers.
-keep class com.slowatcoding.finio.ui.navigation.** { *; }
