package com.berry.patchguide.di;

import com.berry.patchguide.data.local.AppDatabase;
import com.berry.patchguide.data.local.dao.FavoritePatchDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideFavoritePatchDaoFactory implements Factory<FavoritePatchDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideFavoritePatchDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public FavoritePatchDao get() {
    return provideFavoritePatchDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideFavoritePatchDaoFactory create(
      javax.inject.Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideFavoritePatchDaoFactory(Providers.asDaggerProvider(dbProvider));
  }

  public static DatabaseModule_ProvideFavoritePatchDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideFavoritePatchDaoFactory(dbProvider);
  }

  public static FavoritePatchDao provideFavoritePatchDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideFavoritePatchDao(db));
  }
}
