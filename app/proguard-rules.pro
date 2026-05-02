# 并保留源文件名为"Proguard"字符串，而非原始的类名 并保留行号 // blog from sodino.com
-keepattributes SourceFile,LineNumberTable

# Keep fragment name
-keepnames class me.ykrank.s1next.** implements androidx.fragment.app.Fragment
-keepnames class me.ykrank.s1next.** implements android.app.Fragment

# Jackson Model
-keep public class me.ykrank.s1next.data.api.model.** { *; }
-keep public class me.ykrank.s1next.data.api.app.model.** { *; }
-keep public class me.ykrank.s1next.data.cache.** { *; }
-keep public class me.ykrank.s1next.data.db.dbmodel.ReadProgress { *; }
-keep public class me.ykrank.s1next.widget.uploadimg.model.** { *; }
# db model
-keep public class me.ykrank.s1next.data.db.dbmodel.** { *; }
-keep public class me.ykrank.s1next.data.cache.dbmodel.Cache { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class me.ykrank.s1next.data.db.AppDatabase_Impl { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public *;
}
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.InvalidationTracker.InvalidationTrackerErrorHandler <methods>;
}
# Keep DAOs
-keep interface * extends androidx.room.Dao { *; }
-keepclassmembers class * implements androidx.room.Dao { *; }
# Keep entities
-keep class me.ykrank.s1next.data.db.dbmodel.** { *; }

# Keep androidtools json widgets
-keep class com.github.ykrank.androidtools.widget.json.** { *; }
# Keep Jackson deserializers
-keep class com.fasterxml.jackson.databind.JsonDeserializer { *; }
-keep class * extends com.fasterxml.jackson.databind.JsonDeserializer { *; }
