package com.example.monetization

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID_REMOVE_ADS = "remove_ads_lifetime"
        private const val PREFS_NAME = "pdf_studio_monetization_prefs"
        private const val KEY_ADS_REMOVED = "is_ads_removed_pro"
        private const val TAG = "BillingManager"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAdsRemoved = MutableStateFlow(prefs.getBoolean(KEY_ADS_REMOVED, false))
    val isAdsRemoved: StateFlow<Boolean> = _isAdsRemoved.asStateFlow()

    private val _billingConnected = MutableStateFlow(false)
    val billingConnected: StateFlow<Boolean> = _billingConnected.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Play Billing connected successfully")
                    _billingConnected.value = true
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Play Billing setup finished with response code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Play Billing disconnected, retrying...")
                _billingConnected.value = false
            }
        })
    }

    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundRemoveAds = false
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        foundRemoveAds = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
                if (foundRemoveAds) {
                    setAdsRemoved(true)
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, onFallbackToSimulatedPurchase: () -> Unit) {
        if (!_billingConnected.value) {
            // If offline, test environment, or not yet published in Play Console
            onFallbackToSimulatedPurchase()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val details = productDetailsList.first()
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(details)
                                .build()
                        )
                    )
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                Log.w(TAG, "Product not found on Play Console or in testing mode, activating local simulated purchase")
                onFallbackToSimulatedPurchase()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS)) {
                    acknowledgePurchase(purchase)
                    setAdsRemoved(true)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled purchase")
        } else {
            Log.w(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { result ->
                Log.d(TAG, "Acknowledge purchase response: ${result.responseCode}")
            }
        }
    }

    fun setAdsRemoved(removed: Boolean) {
        prefs.edit().putBoolean(KEY_ADS_REMOVED, removed).apply()
        _isAdsRemoved.value = removed
    }

    fun toggleProStatus() {
        setAdsRemoved(!_isAdsRemoved.value)
    }
}
