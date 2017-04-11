package com.mantz_it.wearnetworknotifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

/**
 * <h1>Wear Network Notifications - Settings Fragment</h1>
 *
 * Module:      SettingsFragment.java
 * Description: The SettingsFragment enables the user to change preferences defined in
 *              preferences.xml.
 *
 * @author Dennis Mantz
 *
 * Copyright (C) 2015 Dennis Mantz
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
public class SettingsFragment  extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String LOGTAG = "SettingsFragment";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();

		// add change listener for shared preferences:
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		updateSummaries();
	}

	@Override
	public void onPause() {
		// unregister change listener:
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// update the summeries:
		updateSummaries();
	}

	/**
	 * Will go through each preference element and initialize/update the summary according to its value.
	 */
	public void updateSummaries() {
		// Auto Dismiss
		ListPreference listPref = (ListPreference) findPreference(getString(R.string.pref_autoDismissNotification));
		if(getString(R.string.pref_autoDismissNotification_default).equals(listPref.getValue()))
			listPref.setSummary(getString(R.string.pref_autoDismissNotification_summ_off));
		else
            listPref.setSummary(getString(R.string.pref_autoDismissNotification_summ, listPref.getEntry()));

		// Signal indicator position
		listPref = (ListPreference) findPreference(getString(R.string.pref_signalIndicatorPosition));
		listPref.setSummary(getString(R.string.pref_signalIndicatorPosition_summ, listPref.getEntry()));

		// Wifi signal strength unit
		listPref = (ListPreference) findPreference(getString(R.string.pref_wifiSignalStrengthUnit));
		listPref.setSummary(getString(R.string.pref_wifiSignalStrengthUnit_summ, listPref.getEntry()));

		// Cellular signal strength unit
		listPref = (ListPreference) findPreference(getString(R.string.pref_cellularSignalStrengthUnit));
		listPref.setSummary(getString(R.string.pref_cellularSignalStrengthUnit_summ, listPref.getEntry()));
	}
}
