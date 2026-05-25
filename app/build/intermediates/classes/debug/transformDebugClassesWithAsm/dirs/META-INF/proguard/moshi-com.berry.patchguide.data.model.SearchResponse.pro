-keepnames class com.berry.patchguide.data.model.SearchResponse
-if class com.berry.patchguide.data.model.SearchResponse
-keep class com.berry.patchguide.data.model.SearchResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
