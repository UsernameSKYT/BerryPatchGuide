-keepnames class com.berry.patchguide.data.model.PatchItem
-if class com.berry.patchguide.data.model.PatchItem
-keep class com.berry.patchguide.data.model.PatchItemJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.berry.patchguide.data.model.PatchItem
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-keepclassmembers class com.berry.patchguide.data.model.PatchItem {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.util.List,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
