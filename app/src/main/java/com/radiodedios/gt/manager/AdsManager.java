package com.radiodedios.gt.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.radiodedios.gt.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;

public class AdsManager {
    private static final String TAG = "AdsManager";

    // Test IDs
    public static final String INTERSTITIAL_ID = "ca-app-pub-8751029864651316/1256938353";
    public static final String REWARDED_ID = "ca-app-pub-8751029864651316/8240137342";
    public static final String BANNER_ID = "ca-app-pub-8751029864651316/9553219010";
    public static final String REWARDED_INTERSTITIAL_ID = "ca-app-pub-8751029864651316/1337354439";

    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;
    private RewardedInterstitialAd mRewardedInterstitialAd;
    private final MaxManager maxManager;
    private final BillingManager billingManager;
    private long lastInterstitialTime = 0;
    private static final long INTERSTITIAL_COOLDOWN_MS = 2 * 60 * 1000; // 2 minutes

    public AdsManager(Context context, BillingManager billingManager) {
        this.maxManager = new MaxManager(context);
        this.billingManager = billingManager;
        MobileAds.initialize(context, initializationStatus -> {});
        loadInterstitial(context);
        loadRewarded(context);
        loadRewardedInterstitial(context);
    }

    public void loadBanner(AdView adView) {
        if (billingManager.isPremiumPurchased() || maxManager.isMaxActive()) {
            adView.setVisibility(android.view.View.GONE);
            return;
        }
        adView.setVisibility(android.view.View.VISIBLE);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void loadInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest,
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    public void showInterstitial(Activity activity, Runnable onAdShowed, Runnable onAdClosed) {
        if (billingManager.isPremiumPurchased() || maxManager.isMaxActive()) {
            if (onAdClosed != null) onAdClosed.run();
            return;
        }

        if (System.currentTimeMillis() - lastInterstitialTime < INTERSTITIAL_COOLDOWN_MS) {
            if (onAdClosed != null) onAdClosed.run();
            return; 
        }

        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mInterstitialAd = null;
                    loadInterstitial(activity);
                    if (onAdClosed != null) onAdClosed.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mInterstitialAd = null;
                    if (onAdClosed != null) onAdClosed.run();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    lastInterstitialTime = System.currentTimeMillis();
                    if (onAdShowed != null) onAdShowed.run();
                }
            });
            mInterstitialAd.show(activity);
        } else {
            loadInterstitial(activity);
            if (onAdClosed != null) onAdClosed.run();
        }
    }
    
    // Overload for backward compatibility if needed, though we will update all calls
    public void showInterstitial(Activity activity, Runnable onAdClosed) {
        showInterstitial(activity, null, onAdClosed);
    }

    private void loadRewarded(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, REWARDED_ID, adRequest,
            new RewardedAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mRewardedAd = null;
                }

                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                }
            });
    }

    public void showRewarded(Activity activity, OnUserEarnedRewardListener listener) {
        showRewarded(activity, null, null, listener);
    }
    
    public void showRewarded(Activity activity, Runnable onAdShowed, Runnable onAdClosed, OnUserEarnedRewardListener listener) {
        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                 @Override
                public void onAdDismissedFullScreenContent() {
                    mRewardedAd = null;
                    loadRewarded(activity); // Reload
                    if (onAdClosed != null) onAdClosed.run();
                }
                
                @Override
                public void onAdShowedFullScreenContent() {
                     if (onAdShowed != null) onAdShowed.run();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mRewardedAd = null;
                    if (onAdClosed != null) onAdClosed.run();
                }
            });
            mRewardedAd.show(activity, listener);
        } else {
            loadRewarded(activity);
            // Optionally inform user ad is not ready
        }
    }

    private void loadRewardedInterstitial(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedInterstitialAd.load(context, REWARDED_INTERSTITIAL_ID, adRequest,
            new RewardedInterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                    mRewardedInterstitialAd = ad;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mRewardedInterstitialAd = null;
                }
            });
    }

    public void showRewardedInterstitial(Activity activity, OnUserEarnedRewardListener listener) {
        showRewardedInterstitial(activity, null, null, listener);
    }

    public void showRewardedInterstitial(Activity activity, Runnable onAdShowed, Runnable onAdClosed, OnUserEarnedRewardListener listener) {
        if (mRewardedInterstitialAd != null) {
            mRewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mRewardedInterstitialAd = null;
                    loadRewardedInterstitial(activity);
                    if (onAdClosed != null) onAdClosed.run();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                     if (onAdShowed != null) onAdShowed.run();
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    mRewardedInterstitialAd = null;
                    if (onAdClosed != null) onAdClosed.run();
                }
            });
            mRewardedInterstitialAd.show(activity, listener);
        } else {
            loadRewardedInterstitial(activity);
        }
    }

    public void showMixedRewardedAd(Activity activity, boolean preferInterstitial, Runnable onAdShowed, Runnable onAdClosed, OnUserEarnedRewardListener listener) {
        if (preferInterstitial) {
            if (mRewardedInterstitialAd != null) {
                Toast.makeText(activity, R.string.loading_ad, Toast.LENGTH_SHORT).show();
                showRewardedInterstitial(activity, onAdShowed, onAdClosed, listener);
            } else if (mRewardedAd != null) {
                // Fallback to Video
                Toast.makeText(activity, R.string.loading_ad, Toast.LENGTH_SHORT).show();
                showRewarded(activity, onAdShowed, onAdClosed, listener);
            } else {
                // Both failed
                Toast.makeText(activity, R.string.ad_load_failed, Toast.LENGTH_SHORT).show();
                loadRewarded(activity);
                loadRewardedInterstitial(activity);
            }
        } else {
            // Prefer Video
            if (mRewardedAd != null) {
                Toast.makeText(activity, R.string.loading_ad, Toast.LENGTH_SHORT).show();
                showRewarded(activity, onAdShowed, onAdClosed, listener);
            } else if (mRewardedInterstitialAd != null) {
                // Fallback to Interstitial
                Toast.makeText(activity, R.string.loading_ad, Toast.LENGTH_SHORT).show();
                showRewardedInterstitial(activity, onAdShowed, onAdClosed, listener);
            } else {
                Toast.makeText(activity, R.string.ad_load_failed, Toast.LENGTH_SHORT).show();
                loadRewarded(activity);
                loadRewardedInterstitial(activity);
            }
        }
    }
}
