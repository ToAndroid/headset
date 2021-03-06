package com.ihs.feature.common;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Printer;

import com.ihs.app.framework.HSApplication;
import com.ihs.commons.utils.HSLog;
import com.ihs.commons.utils.HSPreferenceHelper;
import com.ihs.keyboardutils.BuildConfig;
import com.kc.utils.KCAnalytics;

/**
 * Evaluates device rendering performance and provides suggestions on how we do animations and draw expensive effects.
 */
public class DeviceEvaluator {

    private static final String TAG = DeviceEvaluator.class.getSimpleName();

    /**
     * Good device. All animations on.
     */
    public static final int DEVICE_EVALUATION_GOOD = 3;

    /**
     * Average device. Enable most animations and effects that are not very expensive.
     */
    public static final int DEVICE_EVALUATION_DEFAULT = 2;

    /**
     * Below-average device. Only enable animations that are not expensive at all.
     */
    public static final int DEVICE_EVALUATION_BELOW_AVERAGE = 1;

    /**
     * Poor device. Disable all animations that can be disabled.
     */
    public static final int DEVICE_EVALUATION_POOR = 0;

    public static final int DEVICE_EVALUATION_UNKNOWN = -1;

    private static final int DEBUG_EVALUATION = BuildConfig.DEBUG ? -1 : -1;

    private static final String PREF_KEY_EVALUATION = "device_evaluation";

    private static final long MB = 1024 * 1024;

    private static int sEvaluation = DEVICE_EVALUATION_DEFAULT;

    private static FrameSaturationMonitor sMonitor;

    public static int getEvaluation() {
        return sEvaluation;
    }

    public static void evaluate(Context context) {
        if (DEBUG_EVALUATION != -1) {
            HSLog.i(TAG, "Evaluation set to " + toString(DEBUG_EVALUATION) + " for debugging");
            sEvaluation = DEBUG_EVALUATION;
            return;
        }

        int lastKnownEvaluation = HSPreferenceHelper.getDefault().getInt(PREF_KEY_EVALUATION, DEVICE_EVALUATION_UNKNOWN);
        sEvaluation = (lastKnownEvaluation == DEVICE_EVALUATION_UNKNOWN) ?
                evaluateByRam(context) : lastKnownEvaluation;
    }

    private static int evaluateByRam(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        try {
            am.getMemoryInfo(memoryInfo);
        } catch (SecurityException e) {
            return DEVICE_EVALUATION_DEFAULT;
        }
        long totalMem = DeviceManager.getInstance().getTotalRam();

        int evaluation;
        if (totalMem < 1024 * MB) {
            evaluation = DEVICE_EVALUATION_POOR;
        } else if (totalMem < 1536 * MB) {
            evaluation = DEVICE_EVALUATION_BELOW_AVERAGE;
        } else if (totalMem < 2048 * MB) {
            evaluation = DEVICE_EVALUATION_DEFAULT;
        } else {
            evaluation = DEVICE_EVALUATION_GOOD;
        }
        HSLog.d(TAG, "Device total RAM: " + (totalMem / MB) + " MB, evaluation " + toString(evaluation));
        return evaluation;
    }

    public static void startEvaluation() {
        if (DEBUG_EVALUATION != -1) {
            return;
        }
        return;

//        sMonitor = new FrameSaturationMonitor();
//        Looper.getMainLooper().setMessageLogging(sMonitor);
    }

    public static void stopEvaluation() {
        if (sMonitor != null) {
            int oldEvaluation = sEvaluation;
            int evaluation = sMonitor.report();
            if (evaluation != DEVICE_EVALUATION_UNKNOWN && oldEvaluation != evaluation) {
                HSLog.d(TAG, "Evaluation changed from " + toString(oldEvaluation) + " to " + toString(evaluation));
                sEvaluation = evaluation;
            } else {
                HSLog.d(TAG, "Evaluation stays at " + toString(oldEvaluation));
            }
            HSPreferenceHelper.getDefault().putInt(PREF_KEY_EVALUATION, evaluation); // Inter-process SP file
            sMonitor = null;
        }
    }

    private static String toString(int evaluation) {
        switch (evaluation) {
            case DEVICE_EVALUATION_POOR:
                return "POOR";
            case DEVICE_EVALUATION_BELOW_AVERAGE:
                return "BELOW_AVERAGE";
            case DEVICE_EVALUATION_DEFAULT:
                return "DEFAULT";
            case DEVICE_EVALUATION_GOOD:
                return "GOOD";
        }
        return "";
    }

    private static class FrameSaturationMonitor implements Printer {
        private long mFrameStartTimeNanos;

        private int mFrameCount;
        private long mTotalCostNanos;

        private HSPreferenceHelper mPrefs = HSPreferenceHelper.create(HSApplication.getContext(),
                "com.honeycomb.launcher_evaluation");

        private static final String PREF_KEY_ACCUMULATIVE_FRAME_COUNT = "device_eval_accu_frame_count";
        private static final String PREF_KEY_ACCUMULATIVE_COST_NANOS = "device_eval_accu_cost_nanos";
        private static final String PREF_KEY_RESULT_LOGGED = "device_eval_result_logged";

        // We consider the result to be converged after 1500 frames (about 5 times of boost animation)
        private static final int RESULT_CONVERGE_FRAME_COUNT_THRESHOLD = 1500;

        FrameSaturationMonitor() {
            // Main process only
            mFrameCount = mPrefs.getInt(PREF_KEY_ACCUMULATIVE_FRAME_COUNT, 0);
            mTotalCostNanos = mPrefs.getLong(PREF_KEY_ACCUMULATIVE_COST_NANOS, 0L);
        }

        @Override
        public void println(String x) {
            // Partial match "android.view.Choreographer$FrameHandler" to reduce overhead of our measurement.
            // See Looper#loop().
            if (!x.contains("her$Frame")) {
                return;
            }
            if (x.charAt(0) == '>') {
                mFrameStartTimeNanos = elapsedRealtimeNanosCompat();
            } else if (x.charAt(0) == '<') {
                long cost = elapsedRealtimeNanosCompat() - mFrameStartTimeNanos;
                mFrameCount++;
                mTotalCostNanos += cost;
            }
        }

        int report() {
            if (mFrameCount == 0) {
                return DEVICE_EVALUATION_UNKNOWN;
            }
            mPrefs.putInt(PREF_KEY_ACCUMULATIVE_FRAME_COUNT, mFrameCount);
            mPrefs.putLong(PREF_KEY_ACCUMULATIVE_COST_NANOS, mTotalCostNanos);

            long averageFrameTime = mTotalCostNanos / mFrameCount;
            final int saturation = (int) (averageFrameTime * 100 / 16666667);

            int evaluation;
            if (saturation >= 100) {
                // Sony Ericsson (#66): 686 MB, 162%
                evaluation = DEVICE_EVALUATION_POOR;
            } else if (saturation >= 50) {
                evaluation = DEVICE_EVALUATION_BELOW_AVERAGE;
            } else if (saturation >= 30) {
                // Sony D6653 (#69): 2788 MB, 31% | Motorola MotoE2 (#119): 898 MB, 35% | Samsung SM-G9350 (#58): 3422 MB, 37%
                evaluation = DEVICE_EVALUATION_DEFAULT;
            } else {
                // Nexus 6P (#90): 28% | Motorola MotoG (@lei.guo): 884 MB, 28% | Sony E6653 (#68): 2797 MB, 28%
                evaluation = DEVICE_EVALUATION_GOOD;
            }
            HSLog.v(TAG, mFrameCount + " frames in stats, average cost " + (averageFrameTime / 1000) + " us, " +
                    "average saturation " + saturation + "%");

            if (mFrameCount > RESULT_CONVERGE_FRAME_COUNT_THRESHOLD && !mPrefs.getBoolean(PREF_KEY_RESULT_LOGGED, false)) {
                mPrefs.putBoolean(PREF_KEY_RESULT_LOGGED, true);
                KCAnalytics.logEvent("DeviceEvaluation_Result", "result", Integer.toString(saturation) + "%");
            }
            return evaluation;
        }

        @SuppressLint("NewApi")
        private long elapsedRealtimeNanosCompat() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ?
                    SystemClock.elapsedRealtimeNanos() : SystemClock.elapsedRealtime() * 1000000;
        }
    }
}
