package com.ihs.keyboardutilslib;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.acb.weather.plugin.AcbWeatherManager;
import com.artw.lockscreen.ScreenLockerManager;
import com.artw.lockscreen.lockerappguide.LockerAppGuideManager;
import com.crashlytics.android.answers.Answers;
import com.ihs.app.alerts.HSAlertMgr;
import com.ihs.app.framework.HSApplication;
import com.ihs.app.framework.HSNotificationConstant;
import com.ihs.chargingscreen.HSChargingScreenManager;
import com.ihs.commons.diversesession.HSDiverseSession;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.feature.notification.NotificationManager;
import com.ihs.keyboardutils.appsuggestion.AppSuggestionManager;
import com.ihs.keyboardutils.notification.KCNotificationManager;
import com.ihs.keyboardutils.notification.NotificationBean;
import com.launcher.FloatWindowCompat;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.squareup.leakcanary.LeakCanary;

import net.appcloudbox.ads.expressads.AcbExpressAdManager;
import net.appcloudbox.ads.nativeads.AcbNativeAdManager;

import io.fabric.sdk.android.Fabric;


/**
 * Created by ihandysoft on 16/10/24.
 */

public class MyApplication extends HSApplication {
    public static final String REMOTE_PROCESS_CLEAN = "clean";

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Answers());

        String packageName = getPackageName();
        String processName = getProcessName();
        if (TextUtils.equals(processName, packageName)) {
            onMainProcessApplicationCreate();
        } else {
            String processSuffix = processName.replace(packageName + ":", "");
            onRemoteProcessApplicationCreate(processSuffix);
        }

    }

    private void onRemoteProcessApplicationCreate(String processSuffix) {
    }

    private void onMainProcessApplicationCreate() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        AcbWeatherManager.getInstance().init(this, R.mipmap.ic_launcher);
        AcbExpressAdManager.getInstance().init(this);
        HSChargingScreenManager.init(true, "Master_A(NativeAds)Charging", getString(R.string.ad_placement_filter_download));

        HSGlobalNotificationCenter.addObserver(HSNotificationConstant.HS_SESSION_START, sessionEventObserver);
        HSGlobalNotificationCenter.addObserver(HSNotificationConstant.HS_SESSION_END, sessionEventObserver);

        ScreenLockerManager.init();
        NotificationManager.getInstance().updateBattery();
        initImageLoader();

        KCNotificationManager.getInstance().init(NotificationReceiver.class, new KCNotificationManager.NotificationAvailabilityCallBack() {
            @Override
            public boolean isItemDownloaded(NotificationBean notificationBean) {
                return false;
            }
        }, null, false);


        AppSuggestionManager.getInstance().init(false, getString(R.string.ad_placement_call_assist));
        LockerAppGuideManager.getInstance().init(true);

        AcbNativeAdManager.sharedInstance().activePlacementInProcess(getString(R.string.ad_placement_result_page));
        AcbNativeAdManager.sharedInstance().activePlacementInProcess(AppSuggestionManager.getInstance().getAdPlacementName());
        AcbNativeAdManager.sharedInstance().activePlacementInProcess(getString(R.string.ad_placement_filter_download));
        FloatWindowCompat.initLockScreen(this);
    }


    private INotificationObserver sessionEventObserver = new INotificationObserver() {

        @Override
        public void onReceive(String notificationName, HSBundle bundle) {
            if (HSNotificationConstant.HS_SESSION_START.equals(notificationName)) {
//                int currentapiVersion = android.os.Build.VERSION.SDK_INT;
//                if (currentapiVersion <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                    HSLog.d("should delay rate alert for sdk version between 4.0 and 4.2");
//                }
                HSAlertMgr.delayRateAlert();
                onSessionStart();
            }

            if (HSNotificationConstant.HS_SESSION_END.equals(notificationName)) {
                HSDiverseSession.end();
            }
        }
    };


    private void onSessionStart() {
        HSLog.e("onSessionStart");
    }

    private void initImageLoader() {
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(this);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.tasksProcessingOrder(QueueProcessingType.LIFO);

        ImageLoader.getInstance().init(config.build());
    }

    @Override
    protected void attachBaseContext(Context base) {
        MultiDex.install(base);
        super.attachBaseContext(base);
    }
}
