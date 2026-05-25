package com.berry.patchguide.data.repository;

import com.berry.patchguide.data.local.dao.FavoritePatchDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class FavoriteRepository_Factory implements Factory<FavoriteRepository> {
  private final Provider<FavoritePatchDao> favoritePatchDaoProvider;

  public FavoriteRepository_Factory(Provider<FavoritePatchDao> favoritePatchDaoProvider) {
    this.favoritePatchDaoProvider = favoritePatchDaoProvider;
  }

  @Override
  public FavoriteRepository get() {
    return newInstance(favoritePatchDaoProvider.get());
  }

  public static FavoriteRepository_Factory create(
      javax.inject.Provider<FavoritePatchDao> favoritePatchDaoProvider) {
    return new FavoriteRepository_Factory(Providers.asDaggerProvider(favoritePatchDaoProvider));
  }

  public static FavoriteRepository_Factory create(
      Provider<FavoritePatchDao> favoritePatchDaoProvider) {
    return new FavoriteRepository_Factory(favoritePatchDaoProvider);
  }

  public static FavoriteRepository newInstance(FavoritePatchDao favoritePatchDao) {
    return new FavoriteRepository(favoritePatchDao);
  }
}
