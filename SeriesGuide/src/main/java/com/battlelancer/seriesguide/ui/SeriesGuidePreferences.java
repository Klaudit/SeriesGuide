/*
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.KeyEvent;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.settings.UpdateSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.List;

/**
 * Allows tweaking of various SeriesGuide settings.
 */
public class SeriesGuidePreferences extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static final String TAG = "Settings";

    // Actions for legacy settings
    private static final String ACTION_PREFS_BASIC = "com.battlelancer.seriesguide.PREFS_BASIC";

    private static final String ACTION_PREFS_NOTIFICATIONS
            = "com.battlelancer.seriesguide.PREFS_NOTIFICATIONS";

    private static final String ACTION_PREFS_SHARING = "com.battlelancer.seriesguide.PREFS_SHARING";

    private static final String ACTION_PREFS_ADVANCED
            = "com.battlelancer.seriesguide.PREFS_ADVANCED";

    private static final String ACTION_PREFS_ABOUT = "com.battlelancer.seriesguide.PREFS_ABOUT";

    // Preference keys
    private static final String KEY_CLEAR_CACHE = "clearCache";

    private static final String KEY_GETGLUE_DISCONNECT = "clearGetGlueCredentials";

    public static final String KEY_OFFSET = "com.battlelancer.seriesguide.timeoffset";

    public static final String KEY_DATABASEIMPORTED = "com.battlelancer.seriesguide.dbimported";

    public static final String KEY_SECURE = "com.battlelancer.seriesguide.secure";

    public static final String SUPPORT_MAIL = "support@seriesgui.de";

    private static final String KEY_ABOUT = "aboutPref";

    public static final String KEY_TAPE_INTERVAL = "com.battlelancer.seriesguide.tapeinterval";

    public static int THEME = R.style.Theme_SeriesGuide;

    private static void fireTrackerEvent(Context context, String label) {
        Utils.trackClick(context, TAG, label);
    }

    private static OnPreferenceChangeListener sNoOpChangeListener
            = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Utils.advertiseSubscription(preference.getContext());
            // prevent value from getting saved
            return false;
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_PREFS_BASIC)) {
            addPreferencesFromResource(R.xml.settings_basic);
            setupBasicSettings(
                    this,
                    getIntent(),
                    findPreference(DisplaySettings.KEY_NO_RELEASED_EPISODES),
                    findPreference(DisplaySettings.KEY_HIDE_SPECIALS),
                    findPreference(DisplaySettings.KEY_LANGUAGE),
                    findPreference(DisplaySettings.KEY_THEME),
                    findPreference(DisplaySettings.KEY_NUMBERFORMAT),
                    findPreference(UpdateSettings.KEY_AUTOUPDATE)
            );
        } else if (action != null && action.equals(ACTION_PREFS_NOTIFICATIONS)) {
            addPreferencesFromResource(R.xml.settings_notifications);
            setupNotifiationSettings(
                    this,
                    findPreference(NotificationSettings.KEY_ENABLED),
                    findPreference(NotificationSettings.KEY_FAVONLY),
                    findPreference(NotificationSettings.KEY_VIBRATE),
                    findPreference(NotificationSettings.KEY_RINGTONE),
                    findPreference(NotificationSettings.KEY_THRESHOLD)
            );
        } else if (action != null && action.equals(ACTION_PREFS_SHARING)) {
            addPreferencesFromResource(R.xml.settings_services);
            setupSharingSettings(
                    this,
                    findPreference(KEY_GETGLUE_DISCONNECT)
            );
        } else if (action != null && action.equals(ACTION_PREFS_ADVANCED)) {
            addPreferencesFromResource(R.xml.settings_advanced);
            setupAdvancedSettings(
                    this,
                    findPreference(AdvancedSettings.KEY_UPCOMING_LIMIT),
                    findPreference(KEY_OFFSET),
                    findPreference(AppSettings.KEY_GOOGLEANALYTICS),
                    findPreference(KEY_CLEAR_CACHE)
            );
        } else if (action != null && action.equals(ACTION_PREFS_ABOUT)) {
            addPreferencesFromResource(R.xml.settings_about);
            setupAboutSettings(
                    this,
                    findPreference(KEY_ABOUT)
            );
        } else if (!AndroidUtils.isHoneycombOrHigher()) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_legacy);
        }

        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(R.drawable.ic_actionbar);
    }

    protected static void setupSharingSettings(final Context context, Preference getGluePref) {
        // Disconnect GetGlue
        getGluePref.setEnabled(GetGlueSettings.isAuthenticated(context));
        getGluePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent(context, "Disonnect GetGlue");

                GetGlueSettings.clearTokens(context);
                preference.setEnabled(false);
                return true;
            }
        });
    }

    protected static void setupBasicSettings(final Activity activity, final Intent startIntent,
            Preference noAiredPref, Preference noSpecialsPref, Preference languagePref,
            Preference themePref, Preference numberFormatPref, Preference updatePref) {
        // No aired episodes
        noAiredPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    Utils.trackCustomEvent(activity, TAG, "OnlyFutureEpisodes", "Enable");
                } else {
                    Utils.trackCustomEvent(activity, TAG, "OnlyFutureEpisodes", "Disable");
                }
                return false;
            }
        });

        // No special episodes
        noSpecialsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (((CheckBoxPreference) preference).isChecked()) {
                    Utils.trackCustomEvent(activity, TAG, "OnlySeasonEpisodes", "Enable");
                } else {
                    Utils.trackCustomEvent(activity, TAG, "OnlySeasonEpisodes", "Disable");
                }
                return false;
            }
        });

        // Theme switcher
        if (Utils.hasAccessToX(activity)) {
            themePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (DisplaySettings.KEY_THEME.equals(preference.getKey())) {
                        Utils.updateTheme((String) newValue);

                        // restart to apply new theme (actually build an entirely new task stack)
                        TaskStackBuilder.create(activity)
                                .addNextIntent(new Intent(activity, ShowsActivity.class))
                                .addNextIntent(startIntent)
                                .startActivities();
                    }
                    return true;
                }
            });
            setListPreferenceSummary((ListPreference) themePref);
        } else {
            themePref.setOnPreferenceChangeListener(sNoOpChangeListener);
            themePref.setSummary(R.string.onlyx);
        }

        // set current value of auto-update pref
        ((CheckBoxPreference) updatePref).setChecked(SgSyncAdapter.isSyncAutomatically(activity));

        // show currently set values for list prefs
        setListPreferenceSummary((ListPreference) languagePref);
        setListPreferenceSummary((ListPreference) numberFormatPref);
    }

    protected static void setupNotifiationSettings(final Context context,
            Preference notificationsPref, final Preference notificationsFavOnlyPref,
            final Preference vibratePref, final Preference ringtonePref,
            final Preference notificationsThresholdPref) {
        // allow supporters to enable notifications
        if (Utils.hasAccessToX(context)) {
            notificationsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((CheckBoxPreference) preference).isChecked();
                    if (isChecked) {
                        Utils.trackCustomEvent(context, TAG, "Notifications", "Enable");
                    } else {
                        Utils.trackCustomEvent(context, TAG, "Notifications", "Disable");
                    }

                    notificationsThresholdPref.setEnabled(isChecked);
                    notificationsFavOnlyPref.setEnabled(isChecked);
                    vibratePref.setEnabled(isChecked);
                    ringtonePref.setEnabled(isChecked);

                    Utils.runNotificationService(context);
                    return true;
                }
            });
            notificationsFavOnlyPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetAndRunNotificationsService(context);
                    return true;
                }
            });
            // disable advanced notification settings if notifications are disabled
            boolean isNotificationsEnabled = NotificationSettings.isNotificationsEnabled(context);
            notificationsThresholdPref.setEnabled(isNotificationsEnabled);
            notificationsFavOnlyPref.setEnabled(isNotificationsEnabled);
            vibratePref.setEnabled(isNotificationsEnabled);
            ringtonePref.setEnabled(isNotificationsEnabled);
        } else {
            notificationsPref.setOnPreferenceChangeListener(sNoOpChangeListener);
            ((CheckBoxPreference) notificationsPref).setChecked(false);
            notificationsPref.setSummary(R.string.onlyx);
            notificationsThresholdPref.setEnabled(false);
            notificationsFavOnlyPref.setEnabled(false);
            vibratePref.setEnabled(false);
            ringtonePref.setEnabled(false);
        }

        setListPreferenceSummary((ListPreference) notificationsThresholdPref);
    }

    protected static void setupAdvancedSettings(final Context context,
            Preference upcomingPref, Preference offsetPref, Preference analyticsPref,
            Preference clearCachePref) {

        // Clear image cache
        clearCachePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                fireTrackerEvent(context, "Clear Image Cache");

                // try to open app info where user can clear app cache folders
                Intent intent = new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    // open all apps view
                    intent = new Intent(
                            android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    context.startActivity(intent);
                }

                return true;
            }
        });

        // GA opt-out
        analyticsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference.getKey().equals(AppSettings.KEY_GOOGLEANALYTICS)) {
                    boolean isEnabled = (Boolean) newValue;
                    GoogleAnalytics.getInstance(context).setAppOptOut(isEnabled);
                    return true;
                }
                return false;
            }
        });

        // show currently set values for list prefs
        setListPreferenceSummary((ListPreference) upcomingPref);
        ListPreference offsetListPref = (ListPreference) offsetPref;
        offsetListPref.setSummary(context.getString(R.string.pref_offsetsummary,
                offsetListPref.getEntry()));
    }

    protected static void setupAboutSettings(Context context, Preference aboutPref) {
        final String versionFinal = Utils.getVersion(context);

        // About
        aboutPref.setSummary("v" + versionFinal + " (Database v"
                + SeriesGuideDatabase.DATABASE_VERSION + ")");
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings, target);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Performs certain actions on settings changes. <br> <b>WARNING This is for older devices.
     * Newer devices should implement actions in {@link SettingsFragment}s implementation if they
     * require findPreference() to return non-null values.</b>
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (AdvancedSettings.KEY_UPCOMING_LIMIT.equals(key)
                || DisplaySettings.KEY_LANGUAGE.equals(key)
                || DisplaySettings.KEY_NUMBERFORMAT.equals(key)
                || DisplaySettings.KEY_THEME.equals(key)
                || NotificationSettings.KEY_THRESHOLD.equals(key)
                ) {
            Preference pref = findPreference(key);
            if (pref != null) {
                setListPreferenceSummary((ListPreference) pref);
            }
        }

        /*
         * This can run here, as it does not depend on findPreference() which
         * would return null when using a SettingsFragment.
         */
        if (DisplaySettings.KEY_LANGUAGE.equals(key)) {
            // reset last edit date of all episodes so they will get updated
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(Episodes.LAST_EDITED, 0);
                    getContentResolver().update(Episodes.CONTENT_URI, values, null, null);
                }
            }).start();
        }

        if (key.equals(KEY_OFFSET)) {
            Preference pref = findPreference(key);
            if (pref != null) {
                ListPreference listPref = (ListPreference) pref;
                // Set summary to be the user-description for the selected value
                listPref.setSummary(getString(R.string.pref_offsetsummary, listPref.getEntry()));

                resetAndRunNotificationsService(SeriesGuidePreferences.this);
            }
        }

        if (NotificationSettings.KEY_THRESHOLD.equals(key)) {
            Preference pref = findPreference(key);
            if (pref != null) {
                resetAndRunNotificationsService(SeriesGuidePreferences.this);
            }
        }

        // Toggle auto-update on SyncAdapter
        if (UpdateSettings.KEY_AUTOUPDATE.equals(key)) {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
            if (pref != null) {
                SgSyncAdapter.setSyncAutomatically(SeriesGuidePreferences.this, pref.isChecked());
            }
        }
    }

    /**
     * Resets and runs the notification service to take care of potential time shifts when e.g.
     * changing the time offset.
     */
    private static void resetAndRunNotificationsService(Context context) {
        NotificationService.resetLastEpisodeAirtime(PreferenceManager
                .getDefaultSharedPreferences(context));
        Utils.runNotificationService(context);
    }

    public static void setListPreferenceSummary(ListPreference listPref) {
        // Set summary to be the user-description for the selected value
        listPref.setSummary(listPref.getEntry().toString().replaceAll("%", "%%"));
    }

    @TargetApi(11)
    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            switch (settings) {
                case "basic":
                    addPreferencesFromResource(R.xml.settings_basic);
                    setupBasicSettings(
                            getActivity(),
                            getActivity().getIntent(),
                            findPreference(DisplaySettings.KEY_NO_RELEASED_EPISODES),
                            findPreference(DisplaySettings.KEY_HIDE_SPECIALS),
                            findPreference(DisplaySettings.KEY_LANGUAGE),
                            findPreference(DisplaySettings.KEY_THEME),
                            findPreference(DisplaySettings.KEY_NUMBERFORMAT),
                            findPreference(UpdateSettings.KEY_AUTOUPDATE)
                    );
                    break;
                case "notifications":
                    addPreferencesFromResource(R.xml.settings_notifications);
                    setupNotifiationSettings(
                            getActivity(),
                            findPreference(NotificationSettings.KEY_ENABLED),
                            findPreference(NotificationSettings.KEY_FAVONLY),
                            findPreference(NotificationSettings.KEY_VIBRATE),
                            findPreference(NotificationSettings.KEY_RINGTONE),
                            findPreference(NotificationSettings.KEY_THRESHOLD)
                    );
                    break;
                case "sharing":
                    addPreferencesFromResource(R.xml.settings_services);
                    setupSharingSettings(
                            getActivity(),
                            findPreference(KEY_GETGLUE_DISCONNECT)
                    );
                    break;
                case "advanced":
                    addPreferencesFromResource(R.xml.settings_advanced);
                    setupAdvancedSettings(
                            getActivity(),
                            findPreference(AdvancedSettings.KEY_UPCOMING_LIMIT),
                            findPreference(KEY_OFFSET),
                            findPreference(AppSettings.KEY_GOOGLEANALYTICS),
                            findPreference(KEY_CLEAR_CACHE)
                    );
                    break;
                case "about":
                    addPreferencesFromResource(R.xml.settings_about);
                    setupAboutSettings(
                            getActivity(),
                            findPreference(KEY_ABOUT)
                    );
                    break;
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(getActivity());
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (AdvancedSettings.KEY_UPCOMING_LIMIT.equals(key)
                    || DisplaySettings.KEY_LANGUAGE.equals(key)
                    || DisplaySettings.KEY_NUMBERFORMAT.equals(key)
                    || DisplaySettings.KEY_THEME.equals(key)
                    || NotificationSettings.KEY_THRESHOLD.equals(key)
                    ) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    setListPreferenceSummary((ListPreference) pref);
                }
            }

            if (key.equals(KEY_OFFSET)) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    ListPreference listPref = (ListPreference) pref;
                    // Set summary to be the user-description for the selected
                    // value
                    listPref.setSummary(
                            getString(R.string.pref_offsetsummary, listPref.getEntry()));

                    resetAndRunNotificationsService(getActivity());
                }
            }

            if (NotificationSettings.KEY_THRESHOLD.equals(key)) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    resetAndRunNotificationsService(getActivity());
                }
            }

            // Toggle auto-update on SyncAdapter
            if (UpdateSettings.KEY_AUTOUPDATE.equals(key)) {
                CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
                if (pref != null) {
                    SgSyncAdapter.setSyncAutomatically(getActivity(), pref.isChecked());
                }
            }
        }
    }
}
