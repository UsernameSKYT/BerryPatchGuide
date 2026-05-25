package com.berry.patchguide.di;

import com.berry.patchguide.data.remote.api.BerryPatchApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import retrofit2.Retrofit;

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
public final class NetworkModule_ProvideBerryPatchApiFactory implements Factory<BerryPatchApi> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideBerryPatchApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public BerryPatchApi get() {
    return provideBerryPatchApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideBerryPatchApiFactory create(
      javax.inject.Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideBerryPatchApiFactory(Providers.asDaggerProvider(retrofitProvider));
  }

  public static NetworkModule_ProvideBerryPatchApiFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideBerryPatchApiFactory(retrofitProvider);
  }

  public static BerryPatchApi provideBerryPatchApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBerryPatchApi(retrofit));
  }
}
