package com.example

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    var mInterstitialAd: InterstitialAd? = null
    var mRewardedAd: RewardedAd? = null

    const val BANNER_ID = "ca-app-pub-2254438781203492/1846378798"
    const val INTERSTITIAL_ID = "ca-app-pub-2254438781203492/7364474955"
    const val REWARDED_ID = "ca-app-pub-2254438781203492/7883349703"
    const val NATIVE_ID = "ca-app-pub-2254438781203492/4841987461"

    fun loadInterstitial(context: Context) {
        if (mInterstitialAd != null) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    fun showInterstitial(activity: Activity) {
        mInterstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    loadInterstitial(activity)
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mInterstitialAd = null
                }
            }
            ad.show(activity)
        } ?: run {
            loadInterstitial(activity)
        }
    }

    fun loadRewarded(context: Context) {
        if (mRewardedAd != null) return
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        mRewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    loadRewarded(activity)
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    mRewardedAd = null
                }
            }
            ad.show(activity) { _ ->
                onReward()
            }
        } ?: run {
            loadRewarded(activity)
        }
    }
}
