package com.berry.patchguide.ui.apply;

import android.app.Application;
import androidx.lifecycle.SavedStateHandle;
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
public final class ApplyPatchViewModel_Factory implements Factory<ApplyPatchViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<PatchRepository> patchRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ApplyPatchViewModel_Factory(Provider<Application> applicationProvider,
      Provider<PatchRepository> patchRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.applicationProvider = applicationProvider;
    this.patchRepositoryProvider = patchRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ApplyPatchViewModel get() {
    return newInstance(applicationProvider.get(), patchRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ApplyPatchViewModel_Factory create(
      javax.inject.Provider<Application> applicationProvider,
      javax.inject.Provider<PatchRepository> patchRepositoryProvider,
      javax.inject.Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ApplyPatchViewModel_Factory(Providers.asDaggerProvider(applicationProvider), Providers.asDaggerProvider(patchRepositoryProvider), Providers.asDaggerProvider(savedStateHandleProvider));
  }

  public static ApplyPatchViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<PatchRepository> patchRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ApplyPatchViewModel_Factory(applicationProvider, patchRepositoryProvider, savedStateHandleProvider);
  }

  public static ApplyPatchViewModel newInstance(Application application,
      PatchRepository patchRepository, SavedStateHandle savedStateHandle) {
    return new ApplyPatchViewModel(application, patchRepository, savedStateHandle);
  }
}
