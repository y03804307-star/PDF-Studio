package com.example.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdMobManager(private val context: Context) {

    companion object {
        // User requested test rewarded ad unit ID
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "AdMobManager"
    }

    private var rewardedAd: RewardedAd? = null

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                loadRewardedAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    fun loadRewardedAd() {
        if (_isLoading.value || rewardedAd != null) return

        _isLoading.value = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad successfully loaded")
                    rewardedAd = ad
                    _isAdLoaded.value = true
                    _isLoading.value = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    _isLoading.value = false
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit,
        onAdFailedToShow: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onAdFailedToShow("Reward ad is still loading, proceeding...")
            loadRewardedAd()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen")
                rewardedAd = null
                _isAdLoaded.value = false
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad was dismissed")
                onAdDismissed()
                // Automatically preload next ad
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                onAdFailedToShow(adError.message)
                loadRewardedAd()
            }
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onUserEarnedReward()
        }
    }
}
