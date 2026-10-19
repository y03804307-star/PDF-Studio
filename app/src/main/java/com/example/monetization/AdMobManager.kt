package com.example.monetization

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdMobManager(private val context: Context) {

    companion object {
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "AdMobManager"
    }

    private var isInitialized = false
    private var isInitializing = false
    private var rewardedAd: RewardedAd? = null

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initializeIfNeeded(onInitialized: (() -> Unit)? = null) {
        if (isInitialized) {
            onInitialized?.invoke()
            return
        }
        if (isInitializing) return
        isInitializing = true

        // Offload AdMob initialization to a background thread to prevent cold start delays and Chromium ANR warnings
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestConfiguration = RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                    .build()
                MobileAds.setRequestConfiguration(requestConfiguration)

                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "AdMob MobileAds initialized on background thread: $status")
                    isInitialized = true
                    isInitializing = false
                    Handler(Looper.getMainLooper()).post {
                        onInitialized?.invoke()
                        loadRewardedAd()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MobileAds", e)
                isInitializing = false
            }
        }
    }

    fun loadRewardedAd() {
        if (!isInitialized) {
            initializeIfNeeded { loadRewardedAd() }
            return
        }
        if (_isLoading.value || rewardedAd != null) return

        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadRewardedAd", e)
            _isLoading.value = false
            _isAdLoaded.value = false
        }
    }

    fun showRewardedAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdDismissed: () -> Unit,
        onAdFailedToShow: (String) -> Unit
    ) {
        if (!isInitialized) {
            initializeIfNeeded {
                showRewardedAd(activity, onUserEarnedReward, onAdDismissed, onAdFailedToShow)
            }
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            onAdFailedToShow("Reward ad is loading, proceeding...")
            loadRewardedAd()
            return
        }

        try {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed full screen")
                    rewardedAd = null
                    _isAdLoaded.value = false
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad was dismissed")
                    onAdDismissed()
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
        } catch (e: Exception) {
            Log.e(TAG, "Error showing rewarded ad", e)
            onAdFailedToShow(e.message ?: "Failed to show ad")
            rewardedAd = null
            _isAdLoaded.value = false
        }
    }
}
