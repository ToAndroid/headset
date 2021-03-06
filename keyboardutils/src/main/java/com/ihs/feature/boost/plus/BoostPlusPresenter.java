package com.ihs.feature.boost.plus;

import android.content.Context;
import android.content.Intent;
import android.view.Menu;

import com.artw.lockscreen.common.NavUtils;
import com.ihs.commons.config.HSConfig;
import com.ihs.commons.utils.HSLog;
import com.ihs.device.clean.memory.HSAppMemory;
import com.ihs.feature.boost.BoostUtils;
import com.ihs.feature.boost.RamUsageDisplayUpdater;
import com.ihs.feature.common.ConcurrentUtils;
import com.ihs.feature.common.DataSetComparator;
import com.ihs.feature.common.LauncherFiles;
import com.ihs.feature.common.PreferenceHelper;
import com.ihs.feature.common.Utils;
import com.ihs.keyboardutils.R;
import com.ihs.keyboardutils.permission.PermissionUtils;
import com.kc.utils.KCAnalytics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class BoostPlusPresenter implements BoostPlusContracts.Presenter {

    private static final String TAG = BoostPlusPresenter.class.getSimpleName();

    private static final String PREF_KEY_OPTIONS_MENU_VISITED = "options_menu_visited";
    private static final String PREF_KEY_USER_ADDED_APPS = "user_added_apps";
    private static final String PREF_KEY_USER_REMOVED_APPS = "user_removed_apps";

    private static final int DEFAULT_CRITICAL_MEMORY_THRESHOLD_MBS = 100;
    private static final int DEFAULT_OPTIMAL_MEMORY_THRESHOLD_MBS = 30;

    private BoostPlusContracts.View mView;

    private PreferenceHelper mPrefs;

    private int mCriticalMemoryThresholdMbs;
    private int mOptimalMemoryThresholdMbs;

    static class UserChoices {
        List<String> additions = new ArrayList<>();
        List<String> removals = new ArrayList<>();
    }

    BoostPlusPresenter(BoostPlusContracts.View view) {
        mView = view;

        mPrefs = PreferenceHelper.get(LauncherFiles.BOOST_PREFS);

        mCriticalMemoryThresholdMbs = HSConfig.optInteger(DEFAULT_CRITICAL_MEMORY_THRESHOLD_MBS,
                "Application", "BoostPlus", "Thresholds", "CriticalRamUsageMbs");
        mOptimalMemoryThresholdMbs = HSConfig.optInteger(DEFAULT_OPTIMAL_MEMORY_THRESHOLD_MBS,
                "Application", "BoostPlus", "Thresholds", "OptimalRamUsageMbs");
    }

    @Override
    public boolean createOptionsMenu(Menu menu) {
        if (BoostUtils.shouldEnableBoostPlusFeature()) {
            if (mPrefs.getBoolean(PREF_KEY_OPTIONS_MENU_VISITED, false)) {
                mView.inflateOptionsMenu(R.menu.boost_plus, menu);
            } else {
                mView.inflateOptionsMenu(R.menu.boost_plus_new, menu);
            }
            return true;
        }
        return false;
    }

    @Override
    public void refreshBannerColor(long totalSizeBytes, boolean animated) {
        int sizeInMb = (int) (totalSizeBytes / (1024 * 1024));
        if (sizeInMb >= mCriticalMemoryThresholdMbs) {
            HSLog.d(TAG + ".Banner", "Start animating banner color to red");
            mView.setBannerColor(R.color.boost_plus_red, animated);
            return;
        }
        if (sizeInMb < mOptimalMemoryThresholdMbs) {
            HSLog.d(TAG + ".Banner", "Start animating banner color to blue");
            mView.setBannerColor(R.color.boost_plus_blue, animated);
            return;
        }
        HSLog.d(TAG + ".Banner", "Start animating banner color to yellow");
        mView.setBannerColor(R.color.boost_plus_yellow, animated);
    }

    @Override
    public void startSettings() {
        Context context = mView.getContext();
        Intent settingsIntent = new Intent(context, BoostPlusSettingsActivity.class);
        NavUtils.startActivitySafely(context, settingsIntent);

        boolean firstVisit = !mPrefs.getBoolean(PREF_KEY_OPTIONS_MENU_VISITED, false);
        if (firstVisit) {
            mPrefs.putBoolean(PREF_KEY_OPTIONS_MENU_VISITED, true);
            mView.invalidateOptionsMenu();
        }
    }

    @Override
    public void startBoost(long size) {
        // Try root mode first
        int rootStatus = RootHelper.grantRootPermissionWithTimeout();
        boolean rootPermissionGranted = RootHelper.isPermissionGranted(rootStatus);
        boolean userActionInvolved = RootHelper.isUserActionInvolved(rootStatus);
        if (userActionInvolved) {
            KCAnalytics.logEvent("BoostPlus_Homepage_BtnClick", "Type", "Root Alert Show");
        }
        if (rootPermissionGranted) {
            if (!userActionInvolved) {
                KCAnalytics.logEvent("BoostPlus_Homepage_BtnClick", "Type", "Boost Directly");
            }
            mView.showCleanAnimationDialog(BoostPlusCleanDialog.CLEAN_TYPE_ROOT);
            onBoostStart(size);
            return;
        }

        // Non-root
        if (!userActionInvolved) {
            if (PermissionUtils.isAccessibilityGranted()) {
                KCAnalytics.logEvent("BoostPlus_Homepage_BtnClick", "Type", "Boost Directly");
                mView.showCleanAnimationDialog(BoostPlusCleanDialog.CLEAN_TYPE_NON_ROOT_DIRECTLY);
            } else {
                KCAnalytics.logEvent("BoostPlus_Homepage_BtnClick", "Type", "Accessibility Alert Show");
                mView.showAuthorizeDialog(size);
            }
            onBoostStart(size);
        }

        if (BoostPlusUtils.hasTurnOnAccessibilityDialogShowed()) {
            BoostPlusUtils.setActionButtonClickCount(false);
        }
    }

    private void onBoostStart(long size) {
        RamUsageDisplayUpdater.getInstance().adjustRamUsageFromBoostPlus(size); // Update boost icon display
    }

    @Override
    public void commitUserChoices(Collection<HSAppMemory> selectedApps, Collection<HSAppMemory> selectSuggestions) {
        final UserChoices userChoices = new UserChoices();

        DataSetComparator<HSAppMemory> comparator = new DataSetComparator<>();
        boolean hasEdits = comparator.compare(selectedApps, selectSuggestions,
                new DataSetComparator.ResultHandler<HSAppMemory>() {
                    @Override
                    public void onItemsAdded(Collection<HSAppMemory> oldCollection, Collection<HSAppMemory> newItems) {
                        for (HSAppMemory added : newItems) {
                            userChoices.additions.add(added.getPackageName());
                        }
                    }

                    @Override
                    public void onItemsRemoved(Collection<HSAppMemory> oldCollection, Collection<HSAppMemory> removedItems) {
                        for (HSAppMemory removed : removedItems) {
                            userChoices.removals.add(removed.getPackageName());
                        }
                    }
                });

        if (!hasEdits) {
            return;
        }
        ConcurrentUtils.postOnSingleThreadExecutor(new Runnable() {
            @Override
            public void run() {
                Set<String> additions = new HashSet<>(
                        Utils.getStringList(mPrefs.getString(PREF_KEY_USER_ADDED_APPS, "")));
                Set<String> removals = new HashSet<>(
                        Utils.getStringList(mPrefs.getString(PREF_KEY_USER_REMOVED_APPS, "")));
                additions.removeAll(userChoices.removals);
                additions.addAll(userChoices.additions);
                removals.removeAll(userChoices.additions);
                removals.addAll(userChoices.removals);
                HSLog.d(TAG + ".UserChoices", "Store user choices, CHECKED " + additions + ", UNCHECKED" + removals);
                mPrefs.putString(PREF_KEY_USER_ADDED_APPS, Utils.getStringListCsv(new ArrayList<>(additions)));
                mPrefs.putString(PREF_KEY_USER_REMOVED_APPS, Utils.getStringListCsv(new ArrayList<>(removals)));
            }
        });
    }

    @Override
    public UserChoices loadUserChoices() {
        UserChoices userChoices = new UserChoices();
        userChoices.additions.addAll(Utils.getStringList(mPrefs.getString(PREF_KEY_USER_ADDED_APPS, "")));
        userChoices.removals.addAll(Utils.getStringList(mPrefs.getString(PREF_KEY_USER_REMOVED_APPS, "")));
        HSLog.d(TAG + ".UserChoices", "Loaded user choices, CHECKED " + userChoices.additions
                + ", UNCHECKED" + userChoices.removals);
        return userChoices;
    }
}
