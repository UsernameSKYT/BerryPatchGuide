package com.berry.patchguide.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.berry.patchguide.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdView(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_view, null) as NativeAdView
            bindNativeAd(adView, nativeAd)
            adView
        },
        update = { adView ->
            bindNativeAd(adView, nativeAd)
        }
    )
}

private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_icon)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)

    (adView.headlineView as TextView).text = nativeAd.headline
    (adView.bodyView as TextView).text = nativeAd.body ?: ""
    (adView.callToActionView as Button).text = nativeAd.callToAction ?: ""

    val iconView = adView.iconView as ImageView
    if (nativeAd.icon != null) {
        iconView.setImageDrawable(nativeAd.icon!!.drawable)
        iconView.visibility = android.view.View.VISIBLE
    } else {
        iconView.visibility = android.view.View.GONE
    }

    val ratingBar = adView.starRatingView as RatingBar
    if (nativeAd.starRating != null) {
        ratingBar.rating = nativeAd.starRating!!.toFloat()
        ratingBar.visibility = android.view.View.VISIBLE
    } else {
        ratingBar.visibility = android.view.View.GONE
    }

    adView.setNativeAd(nativeAd)
}
