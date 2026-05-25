package com.berry.patchguide.data.repository;

import com.berry.patchguide.data.remote.api.BerryPatchApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class PatchRepository_Factory implements Factory<PatchRepository> {
  private final Provider<BerryPatchApi> apiProvider;

  private final Provider<OkHttpClient> okHttpClientProvider;

  public PatchRepository_Factory(Provider<BerryPatchApi> apiProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    this.apiProvider = apiProvider;
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public PatchRepository get() {
    return newInstance(apiProvider.get(), okHttpClientProvider.get());
  }

  public static PatchRepository_Factory create(javax.inject.Provider<BerryPatchApi> apiProvider,
      javax.inject.Provider<OkHttpClient> okHttpClientProvider) {
    return new PatchRepository_Factory(Providers.asDaggerProvider(apiProvider), Providers.asDaggerProvider(okHttpClientProvider));
  }

  public static PatchRepository_Factory create(Provider<BerryPatchApi> apiProvider,
      Provider<OkHttpClient> okHttpClientProvider) {
    return new PatchRepository_Factory(apiProvider, okHttpClientProvider);
  }

  public static PatchRepository newInstance(BerryPatchApi api, OkHttpClient okHttpClient) {
    return new PatchRepository(api, okHttpClient);
  }
}
