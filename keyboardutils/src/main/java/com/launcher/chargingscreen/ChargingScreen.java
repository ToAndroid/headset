package com.launcher.chargingscreen;

import android.animation.ObjectAnimator;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ihs.app.framework.HSApplication;
import com.ihs.charging.HSChargingManager;
import com.ihs.charging.HSChargingManager.HSChargingState;
import com.ihs.charging.HSChargingManager.IChargingListener;
import com.ihs.chargingscreen.HSChargingScreenManager;
import com.ihs.chargingscreen.utils.ChargingManagerUtil;
import com.ihs.chargingscreen.utils.ClickUtils;
import com.ihs.chargingscreen.utils.DisplayUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.notificationcenter.HSGlobalNotificationCenter;
import com.ihs.commons.notificationcenter.INotificationObserver;
import com.ihs.commons.utils.HSBundle;
import com.ihs.commons.utils.HSLog;
import com.ihs.feature.common.BitmapUtils;
import com.ihs.feature.common.ConcurrentUtils;
import com.ihs.feature.common.ScreenStatusReceiver;
import com.ihs.feature.common.ViewUtils;
import com.ihs.feature.notification.NotificationCondition;
import com.ihs.keyboardutils.BuildConfig;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.utils.CommonUtils;
import com.launcher.FloatWindowCompat;
import com.launcher.LockScreen;
import com.launcher.LockScreensLifeCycleRegistry;
import com.launcher.chargingscreen.tipview.ToolTip;
import com.launcher.chargingscreen.tipview.ToolTipRelativeLayout;
import com.launcher.chargingscreen.tipview.ToolTipView;
import com.launcher.chargingscreen.view.ChargingBubbleView;
import com.launcher.chargingscreen.view.ChargingQuantityView;
import com.launcher.chargingscreen.view.PopupView;
import com.launcher.chargingscreen.view.RipplePopupView;
import com.launcher.chargingscreen.view.SlidingFinishRelativeLayout;

import net.appcloudbox.ads.expressads.AcbExpressAdView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.ihs.charging.HSChargingManager.HSChargingState.STATE_DISCHARGING;

public class ChargingScreen extends LockScreen implements INotificationObserver {

    private static final String TAG = "ChargingScreen";

    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean LOG_VERBOSE = true && BuildConfig.DEBUG;

    private static final long DURATION_CHARGING_STATE_TIP_ICON_ANIMATOR = 2080;
    private static final long DURATION_TIP_SHOWING = 6000;

    private static final float CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA = 0.8f;
    private static final float CHARGING_STATE_TIP_ICON_NEGATIVE_ALPHA = 0.2f;
    private static final float CHARGING_STATE_TIP_ICON_ANIMATOR_MAX_ALPHA = 1.0f;
    private static final float CHARGING_STATE_TIP_ICON_ANIMATOR_MIN_ALPHA = 0.1f;

    public static final String EXTRA_BOOLEAN_IS_CHARGING = "EXTRA_BOOLEAN_IS_CHARGING";
    public static final String EXTRA_BOOLEAN_IS_CHARGING_FULL = "EXTRA_BOOLEAN_IS_CHARGING_FULL";
    public static final String EXTRA_INT_BATTERY_LEVEL_PERCENT = "EXTRA_INT_BATTERY_LEVEL_PERCENT";
    public static final String EXTRA_INT_CHARGING_LEFT_MINUTES = "EXTRA_INT_CHARGING_LEFT_MINUTES";
    public static final String EXTRA_BOOLEAN_IS_CHARGING_STATE_CHANGED = "EXTRA_BOOLEAN_IS_CHARGING_STATE_CHANGED";

    public static final String EXTRA_SKIN_TYPE = "EXTRA_SKIN_TYPE";
    public static final int SKIN_TYPE_BLUE_BACKGROUND = 0;
    public static final int SKIN_TYPE_BLACK_BACKGROUND = 1;
    public static final int SKIN_TYPE_WALLPAPER_BACKGROUND = 2;

    private RipplePopupView menuPopupView;
    private ImageView menuImageView;
    private PopupView mCloseLockerPopupView;

    private TextView timeTextView;
    private TextView dateTextView;

    private TextView fullChargeLeftDescribeTextView;

    private ImageView speedChargeStateImageView;
    private ImageView continuousChargeStateImageView;
    private ImageView trickleChargeStateImageView;

    private TextView tipTextView;

    private ToolTipRelativeLayout toolTipContainer;
    private ToolTipView speedChargeToolTipView;
    private ToolTipView continuousChargeToolTipView;
    private ToolTipView trickleChargeToolTipView;

    private SlidingFinishRelativeLayout slidingFinishRelativeLayout;
    private LinearLayout advertisementContainer;
    private ChargingQuantityView chargingQuantityView;
    private ChargingBubbleView chargingBubbleView;
    private ImageView imageBackgroundView;

    private ObjectAnimator chargingStateAlphaAnimator;

    private boolean isChargingOnInit;

    private Handler handler = new Handler();
    private AcbExpressAdView expressAdView;
    private boolean mAdShown;
    private long onStartTime;

    private boolean isPowerConnected;
    private boolean mDismissed;

    private boolean mIsSetup = false;

    private TelephonyManager telephonyManager;

    private Runnable tipRemoveRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDismissed) {
                return;
            }

            if (speedChargeToolTipView != null) {
                speedChargeToolTipView.remove();
                speedChargeToolTipView = null;
            }

            if (continuousChargeToolTipView != null) {
                continuousChargeToolTipView.remove();
                continuousChargeToolTipView = null;
            }

            if (trickleChargeToolTipView != null) {
                trickleChargeToolTipView.remove();
                trickleChargeToolTipView = null;
            }
        }
    };

    private BroadcastReceiver timeTickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction())) {
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                updateTimeAndDateView();
            }
        }
    };

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {

        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {

                case TelephonyManager.CALL_STATE_IDLE:
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    dismiss(getContext(), false);
                    return;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;

                default:
                    break;
            }
        }
    };

    private IChargingListener chargingListener = new IChargingListener() {
        @Override
        public void onBatteryLevelChanged(int preBatteryLevel, int curBatteryLevel) {
            HSLog.d(TAG, "onBatteryLevelChanged() preBatteryLevel=" + preBatteryLevel
                    + " curBatteryLevel=" + curBatteryLevel);

            chargingQuantityView.setTextValue(curBatteryLevel);

            updateChargingStateTipIconAnimator();
        }

        @Override
        public void onChargingStateChanged(HSChargingState preChargingState, HSChargingState curChargingState) {
            HSLog.d(TAG, "onChargingStateChanged()");

            if (HSChargingManager.getInstance().isCharging()) {
                if (preChargingState == STATE_DISCHARGING) {
                    processPowerStateChanged(true);

                    updateChargingStateTipIconAnimator();
                }
            } else {
                if (preChargingState != STATE_DISCHARGING) {
                    processPowerStateChanged(false);
                }
            }
        }

        @Override
        public void onChargingRemainingTimeChanged(int chargingRemainingMinutes) {
            HSLog.d(TAG, "onChargingRemainingTimeChanged() chargingRemainingMinutes"
                    + chargingRemainingMinutes);

            Context context = getContext();
            if (HSChargingManager.getInstance().getChargingState() == HSChargingState.STATE_CHARGING_FULL) {
                fullChargeLeftDescribeTextView.setText(context.getString(R.string.charging_screen_charged_full));
            } else if (chargingRemainingMinutes > 0) {
                fullChargeLeftDescribeTextView.setText(context.getString(R.string.charging_screen_charged_left_describe,
                        getChargingLeftTimeString(HSChargingManager.getInstance().getChargingLeftMinutes())));
            }
        }

        @Override
        public void onBatteryTemperatureChanged(float v, float v1) {
        }
    };

    private void processPowerStateChanged(boolean isPowerConnected) {
        if (this.isPowerConnected == isPowerConnected) {
            return;
        }
        this.isPowerConnected = isPowerConnected;

        if (isPowerConnected) {
            chargingBubbleView.setPopupBubbleFlag(true);
            updateChargingStateTipIconAnimator();

        } else {
            chargingBubbleView.setPopupBubbleFlag(false);

            final int chargeRemainPercent = HSChargingManager.getInstance().getBatteryRemainingPercent();
            speedChargeStateImageView.setAlpha(1.0f);
            if (chargeRemainPercent > 80) {
                continuousChargeStateImageView.setAlpha(1.0f);
            }
            if (chargeRemainPercent >= 100) {
                trickleChargeStateImageView.setAlpha(1.0f);
            }
        }
    }

    @Override
    public void setup(ViewGroup root, Bundle extra) {
        super.setup(root, extra);

        if (!FloatWindowCompat.needsSystemErrorFloatWindow()) {
            root.findViewById(R.id.charging_screen_container).setPadding(0, 0, 0, CommonUtils.getNavigationBarHeight(HSApplication.getContext()));
        }

        mIsSetup = true;
        // ======== onCreate ========
        HSLog.d(TAG, "onCreate()");

        Context context = root.getContext();

        initView(extra);

        menuImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenuPopupWindow(context, menuImageView);
            }
        });

        slidingFinishRelativeLayout.setSlidingFinishListener(
                new SlidingFinishRelativeLayout.OnSlidingFinishListener() {
                    @Override
                    public void onSlidingFinish(@SlidingFinishRelativeLayout.SlidingState int slidingState) {
                        dismiss(getContext(), true);
                    }
                });

        updateTimeAndDateView();

        context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        requestAds();

        if (extra == null) {
            isChargingOnInit = false;
            chargingQuantityView.setTextValue(100);
            fullChargeLeftDescribeTextView.setText(context.getString(R.string.charging_screen_charged_left_describe, ""));
        } else {
            isChargingOnInit = extra.getBoolean(EXTRA_BOOLEAN_IS_CHARGING, false);
            chargingQuantityView.setTextValue(extra.getInt(EXTRA_INT_BATTERY_LEVEL_PERCENT, 100));
            fullChargeLeftDescribeTextView.setText(extra.getBoolean(EXTRA_BOOLEAN_IS_CHARGING_FULL)
                    ? context.getString(R.string.charging_screen_charged_full) :
                    context.getString(R.string.charging_screen_charged_left_describe,
                            getChargingLeftTimeString(extra.getInt(EXTRA_INT_CHARGING_LEFT_MINUTES))));
            if (extra.getBoolean(EXTRA_BOOLEAN_IS_CHARGING_STATE_CHANGED, false)) {
                showExpressAd();
            }
        }

        updateChargingStateTipIconAnimator();
        processPowerStateChanged(true);

        // ======== onStart ========
        HSLog.d(TAG, "onStart()");
        onStartTime = System.currentTimeMillis();

        HSChargingManager.getInstance().addChargingListener(chargingListener);
        context.registerReceiver(timeTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isScreenOn()) {
            updateTipTextRandomValue();
            updateTimeAndDateView();

            if (isChargingOnInit || HSChargingManager.getInstance().isCharging()) {
                isChargingOnInit = false;
                updateChargingStateTipIconAnimator();
            }
        }

        // ======== onResume ========

        if (chargingBubbleView != null) {
            chargingBubbleView.resumeAnim();
        }

        if (speedChargeToolTipView != null) {
            speedChargeToolTipView.remove();
            speedChargeToolTipView = null;
        }

        if (continuousChargeToolTipView != null) {
            continuousChargeToolTipView.remove();
            continuousChargeToolTipView = null;
        }

        if (trickleChargeToolTipView != null) {
            trickleChargeToolTipView.remove();
            trickleChargeToolTipView = null;
        }

        // ======== onAttachToWindow ========
        HSGlobalNotificationCenter.addObserver(ScreenStatusReceiver.NOTIFICATION_SCREEN_ON, this);
        HSGlobalNotificationCenter.addObserver(ScreenStatusReceiver.NOTIFICATION_SCREEN_OFF, this);

        if (ChargingManagerUtil.hasPermission("android.permission.READ_PHONE_STATE")) {

            telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }

        }

        // Life cycle
        LockScreensLifeCycleRegistry.setLockerActive(false);
        LockScreensLifeCycleRegistry.setChargingScreenActive(true);
        HSGlobalNotificationCenter.sendNotification(NotificationCondition.EVENT_LOCK);
    }

    private void requestAds() {
        expressAdView = new AcbExpressAdView(getContext(), HSChargingScreenManager.getInstance().getChargingActivityAdsPlacementName());
        expressAdView.setAutoSwitchAd(false);
        expressAdView.switchAd();
        expressAdView.setExpressAdViewListener(new AcbExpressAdView.AcbExpressAdViewListener() {
            @Override
            public void onAdShown(AcbExpressAdView acbExpressAdView) {
                mAdShown = true;
            }

            @Override
            public void onAdClicked(AcbExpressAdView acbExpressAdView) {
                dismiss(getContext(), true);
            }
        });
    }

    private void showExpressAd() {
        if (expressAdView.getParent() == null) {
            advertisementContainer.addView(expressAdView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
    }

    /**
     * TODO: handle back key
     */
    public void onBackPressed() {
        dismiss(getContext(), true);
    }

    private void cancelChargingStateAlphaAnimation() {
        if (chargingStateAlphaAnimator != null) {
            chargingStateAlphaAnimator.cancel();
        }
    }

    private void initView(Bundle extra) {
        Context context = getContext();

        int chargingQuantityUpColor;
        int chargingQuantityBottomColor;
        int chargingBubbleColor;

        imageBackgroundView = (ImageView) mRootView.findViewById(R.id.charging_screen_bg);
        switch (extra.getInt(EXTRA_SKIN_TYPE, -1)) {

            default:
            case SKIN_TYPE_BLUE_BACKGROUND: {

                chargingQuantityUpColor = ContextCompat.getColor(getContext(), R.color.charging_screen_blue_quantity_up);
                chargingQuantityBottomColor = ContextCompat.getColor(context, R.color.charging_screen_blue_quantity_bottom);
                chargingBubbleColor = ContextCompat.getColor(context, R.color.charging_screen_blue_bubble);

                imageBackgroundView.setBackgroundColor(ContextCompat.getColor(context, R.color.charging_screen_blue_background));

                break;
            }

            case SKIN_TYPE_BLACK_BACKGROUND: {

                chargingQuantityUpColor = ContextCompat.getColor(context, R.color.charging_screen_black_quantity_up);
                chargingQuantityBottomColor = ContextCompat.getColor(context, R.color.charging_screen_black_quantity_bottom);
                chargingBubbleColor = ContextCompat.getColor(context, R.color.charging_screen_black_bubble);

                imageBackgroundView.setBackgroundColor(ContextCompat.getColor(context, R.color.charging_screen_black_background));

                View blackBackgroundView = mRootView.findViewById(R.id.charging_screen_bg_mask);
                blackBackgroundView.setVisibility(View.VISIBLE);
                blackBackgroundView.setAlpha(0.9f);

                ConcurrentUtils.postOnThreadPoolExecutor(() -> {
                    Bitmap bitmap = getFitScreenWallpaperBitmap();

                    if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                        return;
                    }

                    final Bitmap blurBitmap = BitmapUtils.fastBlur(bitmap, 10, 5);
                    handler.post(() -> imageBackgroundView.setImageBitmap(blurBitmap));
                });

                break;
            }

            case SKIN_TYPE_WALLPAPER_BACKGROUND: {

                chargingQuantityUpColor = ContextCompat.getColor(context, R.color.charging_screen_wallpaper_quantity_up);
                chargingQuantityBottomColor = ContextCompat.getColor(context, R.color.charging_screen_wallpaper_quantity_bottom);
                chargingBubbleColor = ContextCompat.getColor(context, R.color.charging_screen_wallpaper_bubble);

                imageBackgroundView.setBackgroundColor(ContextCompat.getColor(context, R.color.charging_screen_black_background));

                View blackBackgroundView = mRootView.findViewById(R.id.charging_screen_bg_mask);
                blackBackgroundView.setVisibility(View.VISIBLE);
                blackBackgroundView.setAlpha(0.7f);

                ConcurrentUtils.postOnThreadPoolExecutor(() -> {
                    Bitmap bitmap = getFitScreenWallpaperBitmap();
                    if (bitmap == null) {
                        return;
                    }

                    final Bitmap blurBitmap = BitmapUtils.fastBlur(bitmap, 10, 10);
                    handler.post(() -> imageBackgroundView.setImageBitmap(blurBitmap));
                });
                break;
            }
        }

        menuImageView = (ImageView) mRootView.findViewById(R.id.charging_screen_menu);
        slidingFinishRelativeLayout = (SlidingFinishRelativeLayout) mRootView.findViewById(R.id.slidingFinishLayout);

        timeTextView = (TextView) mRootView.findViewById(R.id.charging_screen_time);
        dateTextView = (TextView) mRootView.findViewById(R.id.charging_screen_date);

        chargingQuantityView = (ChargingQuantityView) mRootView.findViewById(R.id.charging_screen_battery_level_percent);
        chargingQuantityView.setMaskOpColor(chargingQuantityUpColor, chargingQuantityBottomColor);
        if (context.getResources().getDisplayMetrics().densityDpi <= DisplayMetrics.DENSITY_HIGH) {
            chargingQuantityView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 86);
        }

        fullChargeLeftDescribeTextView = (TextView) mRootView.findViewById(R.id.charging_screen_full_charge_left_describe);

        speedChargeStateImageView = (ImageView) mRootView.findViewById(R.id.charging_screen_speed_charge_state_icon);
        continuousChargeStateImageView = (ImageView) mRootView.findViewById(R.id.charging_screen_continuous_charge_state_icon);
        trickleChargeStateImageView = (ImageView) mRootView.findViewById(R.id.charging_screen_trickle_charge_state_icon);

        tipTextView = (TextView) mRootView.findViewById(R.id.charging_screen_tip);
        toolTipContainer = (ToolTipRelativeLayout) mRootView.findViewById(R.id.charging_screen_show_tip_container);

        advertisementContainer = (LinearLayout) mRootView.findViewById(R.id.charging_screen_advertisement_container);

        chargingBubbleView = (ChargingBubbleView) mRootView.findViewById(R.id.charging_screen_bubble_view);
        chargingBubbleView.setPopupBubbleColor(chargingBubbleColor);
        speedChargeStateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolTip toolTip = new ToolTip();
                toolTip.withText(context.getString(R.string.charging_screen_charging_speed_charging_state_tip_view))
                        .withColor(Color.BLACK)
                        .withTextColor(Color.WHITE)
                        .withAnimationType(ToolTip.ANIMATOR_TYPE_NONE);

                speedChargeToolTipView = toolTipContainer.showToolTipForView(toolTip, speedChargeStateImageView);
                speedChargeToolTipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (speedChargeToolTipView != null) {
                            speedChargeToolTipView.remove();
                            speedChargeToolTipView = null;
                        }
                    }
                });

                handler.removeCallbacks(tipRemoveRunnable);
                handler.postDelayed(tipRemoveRunnable, DURATION_TIP_SHOWING);
            }
        });

        continuousChargeStateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolTip toolTip = new ToolTip();
                toolTip.withText(context.getString(R.string.charging_screen_charging_continue_charging_state_tip_view))
                        .withColor(Color.BLACK)
                        .withTextColor(Color.WHITE)
                        .withAnimationType(ToolTip.ANIMATOR_TYPE_NONE);

                continuousChargeToolTipView = toolTipContainer.showToolTipForView(toolTip, continuousChargeStateImageView);
                continuousChargeToolTipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (continuousChargeToolTipView != null) {
                            continuousChargeToolTipView.remove();
                            continuousChargeToolTipView = null;
                        }
                    }
                });

                handler.removeCallbacks(tipRemoveRunnable);
                handler.postDelayed(tipRemoveRunnable, DURATION_TIP_SHOWING);
            }
        });

        trickleChargeStateImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolTip toolTip = new ToolTip();
                toolTip.withText(context.getString(R.string.charging_screen_charging_trickle_charging_state_tip_view))
                        .withColor(Color.BLACK)
                        .withTextColor(Color.WHITE)
                        .withAnimationType(ToolTip.ANIMATOR_TYPE_NONE);

                trickleChargeToolTipView = toolTipContainer.showToolTipForView(toolTip, trickleChargeStateImageView);
                trickleChargeToolTipView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (trickleChargeToolTipView != null) {
                            trickleChargeToolTipView.remove();
                            trickleChargeToolTipView = null;
                        }
                    }
                });

                handler.removeCallbacks(tipRemoveRunnable);
                handler.postDelayed(tipRemoveRunnable, DURATION_TIP_SHOWING);
            }
        });

        toolTipContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (speedChargeToolTipView != null) {
                        speedChargeToolTipView.remove();
                        speedChargeToolTipView = null;
                    }

                    if (continuousChargeToolTipView != null) {
                        continuousChargeToolTipView.remove();
                        continuousChargeToolTipView = null;
                    }

                    if (trickleChargeToolTipView != null) {
                        trickleChargeToolTipView.remove();
                        trickleChargeToolTipView = null;
                    }
                }

                return false;
            }
        });
    }

    private void showMenuPopupWindow(Context context, View anchorView) {
        if (menuPopupView == null) {
            menuPopupView = new RipplePopupView(context, mRootView);
            View view = LayoutInflater.from(context).inflate(R.layout.charging_screen_popup_window_new, mRootView, false);
            TextView txtCloseChargingBoost = (TextView) view.findViewById(R.id.tv_close);
            txtCloseChargingBoost.setOnClickListener(v -> {
                if (ClickUtils.isFastDoubleClick()) {
                    return;
                }
                menuPopupView.dismiss();
                showChargingScreenCloseDialog();
            });
            menuPopupView.setOutSideBackgroundColor(Color.TRANSPARENT);
            menuPopupView.setContentView(view);
            menuPopupView.setOutSideClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuPopupView.dismiss();
                }
            });
        }
        menuPopupView.showAsDropDown(anchorView,
                -(getContext().getResources().getDimensionPixelOffset(R.dimen.lock_screen_pop_menu_offset_x) - anchorView.getWidth()),
                -(getContext().getResources().getDimensionPixelOffset(R.dimen.charging_screen_menu_to_top_height) + anchorView.getHeight()) / 2);

    }

    private String getChargingLeftTimeString(int chargingLeftMinutes) {

        String leftTime = "";
        if (chargingLeftMinutes / 60 > 0) {
            leftTime += String.valueOf(chargingLeftMinutes / 60) + "h ";
        }
        if (chargingLeftMinutes % 60 > 0) {
            leftTime += String.valueOf(chargingLeftMinutes % 60) + "m";
        }

        return leftTime;
    }

    private void updateTimeAndDateView() {
        String txtHour;
        String strTimeFormat = Settings.System.getString(getContext().getContentResolver(), Settings.System.TIME_12_24);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

        if ("24".equals(strTimeFormat)) {
            simpleDateFormat.applyPattern("HH");
            txtHour = simpleDateFormat.format(new Date());
        } else {
            simpleDateFormat.applyPattern("hh");
            txtHour = simpleDateFormat.format(new Date());
        }

        simpleDateFormat.applyPattern("mm");
        String txtMinute = simpleDateFormat.format(new Date());

        String txtDay = String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        String txtWeek = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.ENGLISH);
        String txtMonth = Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH);

        timeTextView.setText(getContext().getString(R.string.charging_screen_time, txtHour, txtMinute));
        dateTextView.setText(getContext().getString(R.string.charging_screen_date, txtWeek, txtMonth, txtDay));
    }

    private void updateChargingStateTipIconAnimator() {
        final int chargingRemainingPercent = HSChargingManager.getInstance().getBatteryRemainingPercent();

        if (chargingRemainingPercent < 80) {
            speedChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            continuousChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_NEGATIVE_ALPHA);
            trickleChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_NEGATIVE_ALPHA);
            startChargingStateTipIconAnimator(speedChargeStateImageView);
        } else if (chargingRemainingPercent < 100) {
            speedChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            continuousChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            trickleChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_NEGATIVE_ALPHA);
            startChargingStateTipIconAnimator(continuousChargeStateImageView);
        } else {
            speedChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            continuousChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            trickleChargeStateImageView.setAlpha(CHARGING_STATE_TIP_ICON_POSITIVE_ALPHA);
            startChargingStateTipIconAnimator(trickleChargeStateImageView);
        }
    }

    private void startChargingStateTipIconAnimator(View tipIconView) {
        cancelChargingStateAlphaAnimation();

        chargingStateAlphaAnimator = ObjectAnimator.ofFloat(tipIconView, "alpha",
                CHARGING_STATE_TIP_ICON_ANIMATOR_MIN_ALPHA,
                CHARGING_STATE_TIP_ICON_ANIMATOR_MAX_ALPHA,
                CHARGING_STATE_TIP_ICON_ANIMATOR_MIN_ALPHA);

        chargingStateAlphaAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        chargingStateAlphaAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        chargingStateAlphaAnimator.setDuration(DURATION_CHARGING_STATE_TIP_ICON_ANIMATOR);
        chargingStateAlphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        if (LOG_VERBOSE) {
            chargingStateAlphaAnimator.addUpdateListener(animation -> {
                HSLog.v(TAG, "ANIMATING charging state tip icon");
            });
        }
        chargingStateAlphaAnimator.start();
    }

    private void updateTipTextRandomValue() {
        Context context = getContext();
        final String[] tips = new String[]{
                context.getString(R.string.charging_screen_charging_tip1),
                context.getString(R.string.charging_screen_charging_tip2),
                context.getString(R.string.charging_screen_charging_tip3),
                context.getString(R.string.charging_screen_charging_tip4),
                context.getString(R.string.charging_screen_charging_tip5),
                context.getString(R.string.charging_screen_charging_tip6, context.getString(R.string.app_name))
        };

        tipTextView.setText(tips[new Random().nextInt(tips.length)]);
    }

    private void showChargingScreenCloseDialog() {
        if (mCloseLockerPopupView == null) {
            mCloseLockerPopupView = new PopupView(getContext(), mRootView);
            View content = LayoutInflater.from(getContext()).inflate(R.layout.locker_popup_dialog, null);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams((int) (CommonUtils
                    .getPhoneWidth(getContext()) * 0.872f), WRAP_CONTENT);
            content.setLayoutParams(layoutParams);
            TextView title = ViewUtils.findViewById(content, R.id.title);
            TextView hintContent = ViewUtils.findViewById(content, R.id.hint_content);
            AppCompatButton buttonYes = ViewUtils.findViewById(content, R.id.button_yes);
            AppCompatButton buttonNo = ViewUtils.findViewById(content, R.id.button_no);
            title.setText(R.string.charging_screen_close_dialog_title);
            hintContent.setText(R.string.charging_screen_close_dialog_content);
            buttonNo.setText(R.string.charging_screen_close_dialog_positive_action);
            buttonNo.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mCloseLockerPopupView.dismiss();
                }
            });
            buttonYes.setText(R.string.charging_screen_close_dialog_negative_action);
            buttonYes.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ChargingManagerUtil.disableCharging();
                    mCloseLockerPopupView.dismiss();
                    dismiss(getContext(), false);
                }
            });
            mCloseLockerPopupView.setOutSideBackgroundColor(0xB3000000);
            mCloseLockerPopupView.setContentView(content);
            mCloseLockerPopupView.setOutSideClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mCloseLockerPopupView.dismiss();
                }
            });
        }
        mCloseLockerPopupView.showInCenter();
    }

    private Bitmap getFitScreenWallpaperBitmap() {
        Drawable wallpaperDrawable = null;
        try {
            wallpaperDrawable = WallpaperManager.getInstance(getContext()).getDrawable();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (wallpaperDrawable == null) {
            return null;
        }

        Bitmap wallpaperBitmap = ((BitmapDrawable) wallpaperDrawable).getBitmap();
        if (wallpaperBitmap == null) {
            return null;
        }

        final int resultWidth = Math.min(DisplayUtils.getScreenWidthPixels(), wallpaperBitmap.getWidth());
        final int resultHeight = Math.min(DisplayUtils.getScreenHeightPixels(), wallpaperBitmap.getHeight());

        if (resultWidth <= 0 || resultHeight <= 0) {
            return null;
        }

        return Bitmap.createBitmap(wallpaperBitmap, 0, 0, resultWidth, resultHeight);
    }

    @Override
    public void onReceive(String s, HSBundle hsBundle) {
        switch (s) {
            case ScreenStatusReceiver.NOTIFICATION_SCREEN_ON:
                if (expressAdView == null) {
                    requestAds();
                    showExpressAd();
                } else if (expressAdView.getParent() == null) {
                    showExpressAd();
                } else {
                    if (HSConfig.optBoolean(false, "Application", "Locker", "LockerAutoRefreshAdsEnable")) {
                        expressAdView.switchAd();
                    }
                }

                if (this.isPowerConnected) {
                    chargingBubbleView.setPopupBubbleFlag(true);
                }
                break;
            case ScreenStatusReceiver.NOTIFICATION_SCREEN_OFF:
                if (System.currentTimeMillis() - onStartTime > DateUtils.SECOND_IN_MILLIS) {
                    mAdShown = false;
                }

                if (this.isPowerConnected) {
                    chargingBubbleView.setPopupBubbleFlag(false);
                }
                break;
        }
    }

    @Override
    public void dismiss(Context context, boolean dismissKeyguard) {
        mDismissed = true;

        if (!mIsSetup) {
            return;
        }
        mIsSetup = false;
        // ======== onDetachFromWindow ========
        HSGlobalNotificationCenter.removeObserver(this);

        if (chargingBubbleView != null) {
            chargingBubbleView.pauseAnim();
        }

        // ======== onStop ========
        HSLog.d(TAG, "onStop()");
        if (System.currentTimeMillis() - onStartTime > DateUtils.SECOND_IN_MILLIS) {
            mAdShown = false;
        }

        HSChargingManager.getInstance().removeChargingListener(chargingListener);
        CommonUtils.unregisterReceiver(getContext(), timeTickReceiver);
        HSGlobalNotificationCenter.removeObserver(this);

        cancelChargingStateAlphaAnimation();

        // ======== onDestroy ========
        HSLog.d(TAG, "onDestroy()");

        advertisementContainer.removeAllViews();
        if (expressAdView != null) {
            expressAdView.destroy();
        }

        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            telephonyManager = null;
        }

        // Life cycle
        LockScreensLifeCycleRegistry.setChargingScreenActive(false);
        HSGlobalNotificationCenter.sendNotification(NotificationCondition.EVENT_UNLOCK);

        super.dismiss(context, dismissKeyguard);
    }
}
