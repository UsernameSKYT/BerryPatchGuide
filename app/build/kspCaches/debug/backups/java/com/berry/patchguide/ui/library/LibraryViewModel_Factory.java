package com.berry.patchguide.ui.library;

import com.berry.patchguide.data.repository.FavoriteRepository;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<FavoriteRepository> favoriteRepositoryProvider;

  public LibraryViewModel_Factory(Provider<FavoriteRepository> favoriteRepositoryProvider) {
    this.favoriteRepositoryProvider = favoriteRepositoryProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(favoriteRepositoryProvider.get());
  }

  public static LibraryViewModel_Factory create(
      javax.inject.Provider<FavoriteRepository> favoriteRepositoryProvider) {
    return new LibraryViewModel_Factory(Providers.asDaggerProvider(favoriteRepositoryProvider));
  }

  public static LibraryViewModel_Factory create(
      Provider<FavoriteRepository> favoriteRepositoryProvider) {
    return new LibraryViewModel_Factory(favoriteRepositoryProvider);
  }

  public static LibraryViewModel newInstance(FavoriteRepository favoriteRepository) {
    return new LibraryViewModel(favoriteRepository);
  }
}
