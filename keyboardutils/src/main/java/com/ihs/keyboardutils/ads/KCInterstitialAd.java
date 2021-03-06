package com.ihs.keyboardutils.ads;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.keyboardutils.iap.RemoveAdsManager;

import net.appcloudbox.ads.base.AcbInterstitialAd;
import net.appcloudbox.ads.interstitialads.AcbInterstitialAdLoader;
import net.appcloudbox.common.utils.AcbError;

import java.util.List;

public class KCInterstitialAd {

    public interface OnAdCloseListener {
        void onAdClose();
    }

    public interface OnAdShowListener {
        void onAdShow(boolean success);
    }

    public static void load(String placement) {
        if (RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            return;
        }
        AdUtils.logAdLoad(placement);
        AcbInterstitialAdLoader.preload(HSApplication.getContext(), 1, placement);
    }

    public static AcbInterstitialAdLoader loadAndShow(final String placement, final String title, final String subTitle, final OnAdShowListener onAdShowListener, final OnAdCloseListener onAdCloseListener) {
        return loadAndShow(placement, title, subTitle, false, onAdShowListener, onAdCloseListener);
    }

    /**
     * 加载插页广告，并在加载完成后显示，使用这种方式时需要在合适的时机去 Cancel 返回的 Loader
     * @param placement 广告位
     * @param onAdCloseListener 广告关闭时的回调
     * @param showQuietly 是否使用安静模式
     * @return 广告的Loader，需要在合适的时机去 Cancel
     */
    public static AcbInterstitialAdLoader loadAndShow(final String placement, final String title, final String subTitle, final boolean showQuietly, final OnAdShowListener onAdShowListener, final OnAdCloseListener onAdCloseListener) {
        if (RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            return null;
        }

        AdUtils.logAdLoad(placement);
        AcbInterstitialAdLoader loader = new AcbInterstitialAdLoader(HSApplication.getContext(), placement);
        loader.load(1, new AcbInterstitialAdLoader.AcbInterstitialAdLoadListener() {
            OnAdShowListener listener = onAdShowListener;

            @Override
            public void onAdReceived(AcbInterstitialAdLoader acbInterstitialAdLoader, List<AcbInterstitialAd> list) {
                boolean success = show(list, placement, title, subTitle, showQuietly, onAdCloseListener);
                if (listener != null) {
                    listener.onAdShow(success);
                    listener = null;
                }
            }

            @Override
            public void onAdFinished(AcbInterstitialAdLoader acbInterstitialAdLoader, AcbError hsError) {
                if (hsError != null) {
                    if (listener != null) {
                        listener.onAdShow(false);
                        listener = null;
                    }
                    if(HSLog.isDebugging()){
                        Toast.makeText(HSApplication.getContext(), "Ad(" + placement + ") Error: " + hsError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        return loader;
    }

    public static boolean show(final String placement, final String title, final String subTitle) {
        return show(placement, title, subTitle, false);
    }

    public static boolean show(final String placement, final String title, final String subTitle, final OnAdCloseListener onAdCloseListener) {
        return show(placement, title, subTitle, false, onAdCloseListener);
    }

    public static boolean show(final String placement, final String title, final String subTitle, boolean showQuietly) {
        return show(placement, title, subTitle, showQuietly, null);
    }

    public static boolean show(final String placement, final String title, final String subTitle, boolean showQuietly, final OnAdCloseListener onAdCloseListener) {
        if (RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            return false;
        }

        List<AcbInterstitialAd> interstitialAds = AcbInterstitialAdLoader.fetch(HSApplication.getContext(), placement, 1);
        if (interstitialAds.size() <= 0) {
            return false;
        }

        return show(interstitialAds, placement, title, subTitle, showQuietly, onAdCloseListener);
    }

    private static boolean show(List<AcbInterstitialAd> interstitialAds, final String placement, final String title, final String subTitle, boolean showQuietly, final OnAdCloseListener onAdCloseListener) {
        if (RemoveAdsManager.getInstance().isRemoveAdsPurchased()) {
            return false;
        }

        if (interstitialAds == null || interstitialAds.size() == 0) {
            return false;
        }
        final AcbInterstitialAd interstitialAd = interstitialAds.get(0);
        interstitialAd.setInterstitialAdListener(new AcbInterstitialAd.IAcbInterstitialAdListener() {
            @Override
            public void onAdDisplayed() {
                AdUtils.logAdShow(placement);
            }

            @Override
            public void onAdClicked() {
                if (HSApplication.isDebugging) {
                    Toast.makeText(HSApplication.getContext(), placement + ":" + interstitialAd.getVendorConfig().name(), Toast.LENGTH_SHORT).show();
                }

                AdUtils.logAdClick(placement);
                releaseInterstitialAd(interstitialAd);
            }

            @Override
            public void onAdClosed() {
                releaseInterstitialAd(interstitialAd);
                if (onAdCloseListener != null) {
                    onAdCloseListener.onAdClose();
                }
            }
        });

        if (!TextUtils.isEmpty(title)) {
            interstitialAd.setCustomTitle(title);
        }

        if (!TextUtils.isEmpty(subTitle)) {
            interstitialAd.setCustomSubtitle(subTitle);
        }

        if (showQuietly) {
            interstitialAd.showQuietly(HSApplication.getContext());
        } else {
            interstitialAd.show();
        }

        return true;
    }

    private static void releaseInterstitialAd(final AcbInterstitialAd interstitialAd) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (interstitialAd != null) {
                    interstitialAd.release();
                }
            }
        });
    }
}
