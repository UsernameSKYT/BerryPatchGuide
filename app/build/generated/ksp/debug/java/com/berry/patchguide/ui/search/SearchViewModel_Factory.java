package com.berry.patchguide.ui.search;

import com.berry.patchguide.data.repository.FavoriteRepository;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<PatchRepository> patchRepositoryProvider;

  private final Provider<FavoriteRepository> favoriteRepositoryProvider;

  public SearchViewModel_Factory(Provider<PatchRepository> patchRepositoryProvider,
      Provider<FavoriteRepository> favoriteRepositoryProvider) {
    this.patchRepositoryProvider = patchRepositoryProvider;
    this.favoriteRepositoryProvider = favoriteRepositoryProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(patchRepositoryProvider.get(), favoriteRepositoryProvider.get());
  }

  public static SearchViewModel_Factory create(
      javax.inject.Provider<PatchRepository> patchRepositoryProvider,
      javax.inject.Provider<FavoriteRepository> favoriteRepositoryProvider) {
    return new SearchViewModel_Factory(Providers.asDaggerProvider(patchRepositoryProvider), Providers.asDaggerProvider(favoriteRepositoryProvider));
  }

  public static SearchViewModel_Factory create(Provider<PatchRepository> patchRepositoryProvider,
      Provider<FavoriteRepository> favoriteRepositoryProvider) {
    return new SearchViewModel_Factory(patchRepositoryProvider, favoriteRepositoryProvider);
  }

  public static SearchViewModel newInstance(PatchRepository patchRepository,
      FavoriteRepository favoriteRepository) {
    return new SearchViewModel(patchRepository, favoriteRepository);
  }
}
