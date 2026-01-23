package remix.myplayer.ui.screen

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import remix.myplayer.R
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.ext.clickWithRipple
import timber.log.Timber

@Composable
fun SupportScreen() {
  val theme = LocalTheme.current
  val activity = LocalActivity.current ?: return

  Scaffold(
    topBar = {
      CommonAppBar(
        title = stringResource(R.string.support_develop),
        actions = emptyList()
      )
    },
    containerColor = theme.mainBackground
  ) { contentPadding ->
    var billingClientRef by remember { mutableStateOf<BillingClient?>(null) }

    val purchasesUpdatedListener = remember {
      PurchasesUpdatedListener { billingResult, purchases ->
        Timber.v("onPurchasesUpdated: $billingResult")
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
          val client = billingClientRef
          if (client != null) {
            purchases.forEach { handlePurchase(activity, client, it) }
          }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
          Timber.v("user cancel")
        } else {
          Timber.v("other error")
        }
      }
    }

    val billingClient = remember(activity) {
      BillingClient.newBuilder(activity)
        .enablePendingPurchases()
        .setListener(purchasesUpdatedListener)
        .build()
    }

    LaunchedEffect(billingClient) {
      billingClientRef = billingClient
    }

    val productDetails = remember { mutableStateListOf<ProductDetails>() }

    val productListener = remember {
      ProductDetailsResponseListener { result, details ->
        Timber.v("onProductDetailsResponse, r: $result, d: $details")
        if (details.isEmpty()) return@ProductDetailsResponseListener
        details.sortWith { o1, o2 ->
          o1.oneTimePurchaseOfferDetails!!.priceAmountMicros.compareTo(
            o2.oneTimePurchaseOfferDetails!!.priceAmountMicros
          )
        }
        productDetails.clear()
        productDetails.addAll(details)
      }
    }

    DisposableEffect(billingClient) {
      val stateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: com.android.billingclient.api.BillingResult) {
          Timber.v("onBillingSetupFinished: $billingResult")
          if (billingResult.responseCode != BillingResponseCode.OK) return
          val skuIds = listOf("price_3", "price_8", "price_15", "price_25", "price_40")
          val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
              skuIds.map {
                Product.newBuilder()
                  .setProductId(it)
                  .setProductType(BillingClient.ProductType.INAPP)
                  .build()
              }
            )
            .build()
          billingClient.queryProductDetailsAsync(params, productListener)
        }

        override fun onBillingServiceDisconnected() {
          Timber.v("onBillingServiceDisconnected")
        }
      }

      billingClient.startConnection(stateListener)
      onDispose {
        try {
          billingClient.endConnection()
        } catch (_: Throwable) {
        }
      }
    }

    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.padding(contentPadding),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(productDetails) { pd ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickWithRipple(false) {
              val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                  listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                      .setProductDetails(pd)
                      .build()
                  )
                )
                .build()
              billingClient.launchBillingFlow(activity, params)
            },
          color = LocalTheme.current.mainBackground,
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 8.dp
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            TextPrimary(text = pd.title)
            TextSecondary(text = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: "")
          }

        }
      }
    }
  }
}

private fun handlePurchase(activity: Activity, billingClient: BillingClient, purchase: Purchase) {
  billingClient.consumeAsync(
    ConsumeParams.newBuilder()
      .setPurchaseToken(purchase.purchaseToken)
      .build()
  ) { result, _ ->
    Timber.v("handlePurchase, result: $result")
    if (result.responseCode == BillingResponseCode.OK) {
      MessageNotifier.show(R.string.thank_you)
    } else {
      MessageNotifier.show(R.string.payment_failure)
    }
  }
}