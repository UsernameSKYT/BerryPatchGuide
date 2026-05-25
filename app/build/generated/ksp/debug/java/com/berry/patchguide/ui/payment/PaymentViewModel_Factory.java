package com.berry.patchguide.ui.payment;

import com.berry.patchguide.data.billing.BillingManager;
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
public final class PaymentViewModel_Factory implements Factory<PaymentViewModel> {
  private final Provider<BillingManager> billingManagerProvider;

  public PaymentViewModel_Factory(Provider<BillingManager> billingManagerProvider) {
    this.billingManagerProvider = billingManagerProvider;
  }

  @Override
  public PaymentViewModel get() {
    return newInstance(billingManagerProvider.get());
  }

  public static PaymentViewModel_Factory create(
      javax.inject.Provider<BillingManager> billingManagerProvider) {
    return new PaymentViewModel_Factory(Providers.asDaggerProvider(billingManagerProvider));
  }

  public static PaymentViewModel_Factory create(Provider<BillingManager> billingManagerProvider) {
    return new PaymentViewModel_Factory(billingManagerProvider);
  }

  public static PaymentViewModel newInstance(BillingManager billingManager) {
    return new PaymentViewModel(billingManager);
  }
}
