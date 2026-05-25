package com.berry.patchguide.ui.home;

import com.berry.patchguide.data.repository.PatchRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<PatchRepository> patchRepositoryProvider;

  public HomeViewModel_Factory(Provider<PatchRepository> patchRepositoryProvider) {
    this.patchRepositoryProvider = patchRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(patchRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      javax.inject.Provider<PatchRepository> patchRepositoryProvider) {
    return new HomeViewModel_Factory(Providers.asDaggerProvider(patchRepositoryProvider));
  }

  public static HomeViewModel_Factory create(Provider<PatchRepository> patchRepositoryProvider) {
    return new HomeViewModel_Factory(patchRepositoryProvider);
  }

  public static HomeViewModel newInstance(PatchRepository patchRepository) {
    return new HomeViewModel(patchRepository);
  }
}
