package com.parsfilo.astrology.core.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.parsfilo.astrology.R
import com.parsfilo.astrology.core.data.remote.VerifySubscriptionRequest
import com.parsfilo.astrology.core.domain.model.SubscriptionStatus
import com.parsfilo.astrology.core.util.AppException
import com.parsfilo.astrology.core.util.AppResult
import com.parsfilo.astrology.core.util.StringsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val PRODUCT_PREMIUM_MONTHLY = "premium_monthly"
private const val PRODUCT_PREMIUM_YEARLY = "premium_yearly"

data class PremiumPlanUi(
    val productId: String,
    val title: String,
    val price: String,
    val offerDescription: String? = null,
)

@Singleton
class BillingManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val sessionRepository: SessionRepository,
        private val stringsProvider: StringsProvider,
    ) : com.android.billingclient.api.PurchasesUpdatedListener,
        java.io.Closeable {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(job + Dispatchers.IO)
        private val _plans = MutableStateFlow<List<PremiumPlanUi>>(emptyList())
        val plans: StateFlow<List<PremiumPlanUi>> = _plans.asStateFlow()

        private val _purchaseState = MutableStateFlow<AppResult<SubscriptionStatus>?>(null)
        val purchaseState: StateFlow<AppResult<SubscriptionStatus>?> = _purchaseState.asStateFlow()

        private val billingClient: BillingClient =
            BillingClient
                .newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build()

        suspend fun loadPlans() {
            ensureReady()
            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(
                        listOf(PRODUCT_PREMIUM_MONTHLY, PRODUCT_PREMIUM_YEARLY).map {
                            QueryProductDetailsParams.Product
                                .newBuilder()
                                .setProductId(it)
                                .setProductType(ProductType.SUBS)
                                .build()
                        },
                    ).build()
            val result =
                suspendCancellableCoroutine<Pair<BillingResult, QueryProductDetailsResult>> { continuation ->
                    billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                        continuation.resume(billingResult to productDetailsResult)
                    }
                }
            if (result.first.responseCode == BillingResponseCode.OK) {
                _plans.value =
                    result.second.productDetailsList.map { detail ->
                        PremiumPlanUi(
                            productId = detail.productId,
                            title = detail.name,
                            price =
                                detail.subscriptionOfferDetails
                                    ?.firstOrNull()
                                    ?.pricingPhases
                                    ?.pricingPhaseList
                                    ?.firstOrNull()
                                    ?.formattedPrice
                                    .orEmpty(),
                            offerDescription = detail.description,
                        )
                    }
            }
        }

        fun launchPurchase(
            activity: Activity,
            productId: String,
        ) {
            scope.launch {
                ensureReady()
                val detail =
                    fetchProductDetails(productId) ?: run {
                        _purchaseState.value =
                            AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_plan_not_found)))
                        return@launch
                    }
                val offerToken =
                    detail.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.offerToken
                        .orEmpty()
                val params =
                    BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(detail)
                        .setOfferToken(offerToken)
                        .build()
                billingClient.launchBillingFlow(
                    activity,
                    BillingFlowParams
                        .newBuilder()
                        .setProductDetailsParamsList(listOf(params))
                        .build(),
                )
            }
        }

        suspend fun restorePurchases(): AppResult<SubscriptionStatus> {
            ensureReady()
            val purchases =
                suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { continuation ->
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build(),
                    ) { result, list -> continuation.resume(result to list) }
                }
            if (purchases.first.responseCode != BillingResponseCode.OK) {
                return AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_restore_failed)))
            }
            val purchase =
                purchases.second.firstOrNull()
                    ?: return AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_restore_missing_purchase)))
            return verifyPurchase(purchase, restore = true)
        }

        override fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: MutableList<Purchase>?,
        ) {
            when (billingResult.responseCode) {
                BillingResponseCode.OK ->
                    purchases?.firstOrNull()?.let { purchase ->
                        scope.launch {
                            _purchaseState.value = verifyPurchase(purchase, restore = false)
                        }
                    }
                BillingResponseCode.USER_CANCELED -> {
                    _purchaseState.value =
                        AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_purchase_cancelled)))
                }
                else -> {
                    _purchaseState.value =
                        AppResult.Error(
                            AppException.BillingException(
                                billingResult.debugMessage.ifBlank { stringsProvider.get(R.string.billing_purchase_failed) },
                            ),
                        )
                }
            }
        }

        private suspend fun verifyPurchase(
            purchase: Purchase,
            restore: Boolean,
        ): AppResult<SubscriptionStatus> {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                return AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_payment_pending)))
            }
            if (!purchase.isAcknowledged) {
                val ackResult =
                    suspendCancellableCoroutine { continuation ->
                        billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams
                                .newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build(),
                        ) { continuation.resume(it) }
                    }
                if (ackResult.responseCode != BillingResponseCode.OK) {
                    return AppResult.Error(AppException.BillingException(stringsProvider.get(R.string.billing_acknowledge_failed)))
                }
            }
            val productId = purchase.products.firstOrNull().orEmpty()
            val response =
                if (restore) {
                    sessionRepository.apiRestore(
                        VerifySubscriptionRequest(
                            purchaseToken = purchase.purchaseToken,
                            productId = productId,
                        ),
                    )
                } else {
                    sessionRepository.apiVerify(
                        VerifySubscriptionRequest(
                            purchaseToken = purchase.purchaseToken,
                            productId = productId,
                        ),
                    )
                }
            return response
        }

        private suspend fun fetchProductDetails(productId: String): ProductDetails? {
            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product
                                .newBuilder()
                                .setProductId(productId)
                                .setProductType(ProductType.SUBS)
                                .build(),
                        ),
                    ).build()
            val result =
                suspendCancellableCoroutine<Pair<BillingResult, QueryProductDetailsResult>> { continuation ->
                    billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                        continuation.resume(billingResult to productDetailsResult)
                    }
                }
            return if (result.first.responseCode == BillingResponseCode.OK) {
                result.second.productDetailsList.firstOrNull()
            } else {
                null
            }
        }

        private suspend fun ensureReady() {
            if (billingClient.isReady) return
            suspendCancellableCoroutine<Unit> { continuation ->
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onBillingServiceDisconnected() {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                    },
                )
            }
        }

        override fun close() {
            job.cancel()
            billingClient.endConnection()
        }
    }
