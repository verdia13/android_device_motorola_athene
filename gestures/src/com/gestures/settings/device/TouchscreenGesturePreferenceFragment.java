/*
 * Copyright (C) 2015 The CyanogenMod Project
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

package com.gestures.settings.device;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;

import java.io.File;

import com.gestures.settings.device.FileUtils;

public class TouchscreenGesturePreferenceFragment extends PreferenceFragment {
    private static final String CATEGORY_AMBIENT_DISPLAY = "ambient_display_key";
    private static final String CATEGORY_FINGER_PRINT = "fp_key";
    public static final String DISPLAY_BURNIN_PREF = "display_burnin";
    private SwitchPreference mBurnInPref;
    private SwitchPreference mFlipPref;
    private NotificationManager mNotificationManager;
    private static boolean mFpsAvailable =
            SystemProperties.getBoolean("ro.hw.fps", false);
    private boolean mFlipClick = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.gesture_panel);
        PreferenceCategory ambientDisplayCat = (PreferenceCategory)
                findPreference(CATEGORY_AMBIENT_DISPLAY);
        PreferenceCategory fingerPrintCat = (PreferenceCategory)
                findPreference(CATEGORY_FINGER_PRINT);
        if (ambientDisplayCat != null) {
            ambientDisplayCat.setEnabled(ActionsSettings.isDozeEnabled(getActivity().getContentResolver()));
        }
        if (fingerPrintCat != null) {
            fingerPrintCat.setEnabled(mFpsAvailable);
        }
        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mFlipPref = (SwitchPreference) findPreference("gesture_flip_to_mute");
        mFlipPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
                    mFlipPref.setChecked(false);
                    new AlertDialog.Builder(getContext())
                        .setTitle(getString(R.string.flip_to_mute_title))
                        .setMessage(getString(R.string.dnd_access))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mFlipClick = true;
                                startActivity(new Intent(
                                   android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                            }
                        }).show();
                }
                return true;
            }
        });

        //Users may deny DND access after giving it
        if (!mNotificationManager.isNotificationPolicyAccessGranted()) {
            mFlipPref.setChecked(false);
        }
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Initialize node preferences
        for (String pref : Constants.sBooleanNodePreferenceMap.keySet()) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            b.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String node = Constants.sBooleanNodePreferenceMap.get(preference.getKey());
                    if (!TextUtils.isEmpty(node)) {
                        Boolean value = (Boolean) newValue;
                        FileUtils.writeLine(node, value ? "1" : "0");
                        return true;
                    }
                    return false;
                }
            });
            String node = Constants.sBooleanNodePreferenceMap.get(pref);
            if (!node.isEmpty()) {
                if (new File(node).exists()) {
                    String curNodeValue = FileUtils.readOneLine(node);
                    b.setChecked(curNodeValue.equals("1"));
                } else {
                    b.setEnabled(false);
                }
            }
        }
        // Initialize display preference
        mBurnInPref = (SwitchPreference) findPreference(DISPLAY_BURNIN_PREF);
        mBurnInPref.setChecked(DisplayColors.isBurnInProtectionEnabled());
        mBurnInPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String node = DisplayColors.DISPLAY_BURNIN_NODE;
                if (!TextUtils.isEmpty(node)) {
                    Boolean enabled = (Boolean) newValue;
                    DisplayColors.enableBurnInProtection(enabled);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNotificationManager.isNotificationPolicyAccessGranted() && mFlipClick) {
            mFlipPref.setChecked(true);
        }
    }
}
