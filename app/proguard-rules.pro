# Room entities
-keep class com.qiuye.calendarkotlin.tasks.data.ReminderEntity { *; }
-keep class com.qiuye.calendarkotlin.diary.data.DiaryEntity { *; }

# Kotlin serialization
-keepattributes *Annotation*, *Serializable*
-keepclassmembers class com.qiuye.calendarkotlin.model.** {
    *** Companion;
}
-keepclassmembers class com.qiuye.calendarkotlin.domain.** {
    *** Companion;
}
-keep class com.qiuye.calendarkotlin.model.** { *; }

# Lunar library
-keep class com.nlf.calendar.** { *; }
-dontwarn com.nlf.calendar.**

# DataStore preferences
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <init>(...);
}
