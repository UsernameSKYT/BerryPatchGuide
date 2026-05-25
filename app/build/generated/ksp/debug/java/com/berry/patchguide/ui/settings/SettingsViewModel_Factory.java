package com.berry.patchguide.ui.settings;

import com.berry.patchguide.data.local.datastore.SettingsDataStore;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsDataStore> settingsDataStoreProvider;

  public SettingsViewModel_Factory(Provider<SettingsDataStore> settingsDataStoreProvider) {
    this.settingsDataStoreProvider = settingsDataStoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsDataStoreProvider.get());
  }

  public static SettingsViewModel_Factory create(
      javax.inject.Provider<SettingsDataStore> settingsDataStoreProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(settingsDataStoreProvider));
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsDataStore> settingsDataStoreProvider) {
    return new SettingsViewModel_Factory(settingsDataStoreProvider);
  }

  public static SettingsViewModel newInstance(SettingsDataStore settingsDataStore) {
    return new SettingsViewModel(settingsDataStore);
  }
}
