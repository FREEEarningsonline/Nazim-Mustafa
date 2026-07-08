package com.example

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdViewComposable() {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    
    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, AdManager.NATIVE_ID)
            .forNativeAd { ad ->
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle load error
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            
        adLoader.loadAd(AdRequest.Builder().build())
        
        onDispose {
            nativeAd?.destroy()
        }
    }
    
    nativeAd?.let { ad ->
        AndroidView(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            factory = { ctx ->
                val adView = LayoutInflater.from(ctx).inflate(R.layout.native_ad_layout, null) as NativeAdView
                
                // Populate views
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)
                
                (adView.headlineView as TextView).text = ad.headline
                
                if (ad.body == null) {
                    adView.bodyView?.visibility = android.view.View.INVISIBLE
                } else {
                    adView.bodyView?.visibility = android.view.View.VISIBLE
                    (adView.bodyView as TextView).text = ad.body
                }
                
                if (ad.callToAction == null) {
                    adView.callToActionView?.visibility = android.view.View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = android.view.View.VISIBLE
                    (adView.callToActionView as Button).text = ad.callToAction
                }
                
                if (ad.icon == null) {
                    adView.iconView?.visibility = android.view.View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(ad.icon?.drawable)
                    adView.iconView?.visibility = android.view.View.VISIBLE
                }
                
                adView.setNativeAd(ad)
                adView
            }
        )
    }
}
