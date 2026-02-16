package com.radiodedios.gt.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;

import com.radiodedios.gt.R;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    public static final String PRODUCT_ID_PERMANENT = "radio_dios_permanente";
    private static final String PREF_NAME = "billing_prefs";
    private static final String KEY_IS_PREMIUM = "is_premium";

    private final Context context;
    private final BillingClient billingClient;
    private boolean isPremium = false;
    private ProductDetails productDetails;

    public interface BillingCallback {
        void onPurchaseSuccess();
        void onPurchaseFailure(int messageId);
        void onPremiumChecked(boolean isPremium);
    }

    private BillingCallback callback;

    public BillingManager(Context context) {
        this.context = context;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        isPremium = prefs.getBoolean(KEY_IS_PREMIUM, false);

        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        connectToPlayBilling();
    }

    public void setCallback(BillingCallback callback) {
        this.callback = callback;
    }

    private void connectToPlayBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Connected to Google Play Billing");
                    queryPurchases();
                    queryProductDetails();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d(TAG, "Billing service disconnected");
            }
        });
    }

    private void queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases);
                }
            }
        );
    }

    private void processPurchases(List<Purchase> purchases) {
        boolean hasPremium = false;
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    for (String product : purchase.getProducts()) {
                        if (PRODUCT_ID_PERMANENT.equals(product)) {
                            hasPremium = true;
                            if (!purchase.isAcknowledged()) {
                                acknowledgePurchase(purchase);
                            }
                            break;
                        }
                    }
                }
            }
        }
        isPremium = hasPremium;
        
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
               .edit()
               .putBoolean(KEY_IS_PREMIUM, isPremium)
               .apply();
               
        Log.d(TAG, "Premium status: " + isPremium);
        
        if (callback != null) {
            callback.onPremiumChecked(isPremium);
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            Log.d(TAG, "Purchase acknowledged");
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PERMANENT)
                .setProductType(ProductType.INAPP)
                .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, result) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && result != null) {
                List<ProductDetails> list = result.getProductDetailsList();
                if (list != null) {
                    for (ProductDetails details : list) {
                        if (details.getProductId().equals(PRODUCT_ID_PERMANENT)) {
                            productDetails = details;
                        }
                    }
                }
            }
        });
    }

    public void purchasePremium(Activity activity) {
        if (productDetails != null) {
            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
            productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            );

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

            billingClient.launchBillingFlow(activity, billingFlowParams);
        } else {
             if (callback != null) callback.onPurchaseFailure(R.string.billing_error_product_not_found);
        }
    }

    public boolean isPremiumPurchased() {
        return isPremium;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
            processPurchases(list);
            if (isPremium && callback != null) {
                callback.onPurchaseSuccess();
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
             if (callback != null) callback.onPurchaseFailure(R.string.billing_error_canceled);
        } else {
            if (callback != null) callback.onPurchaseFailure(R.string.billing_error_failed);
        }
    }
}
