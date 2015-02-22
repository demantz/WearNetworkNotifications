package com.mantz_it.wearnetworknotifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

/**
 * Created by dennis on 18/02/15.
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

		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		updateSummaries();
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// update the summeries:
		updateSummaries();
	}

	/**
	 * Will go through each preference element and initialize/update the summary according to its value.
	 * @note this will also correct invalid user inputs on EdittextPreferences!
	 */
	public void updateSummaries() {
		// Wifi signal strength unit
		ListPreference listPref = (ListPreference) findPreference(getString(R.string.pref_wifiSignalStrengthUnit));
		listPref.setSummary(getString(R.string.pref_wifiSignalStrengthUnit_summ, listPref.getEntry()));

		// Cellular signal strength unit
		listPref = (ListPreference) findPreference(getString(R.string.pref_cellularSignalStrengthUnit));
		listPref.setSummary(getString(R.string.pref_cellularSignalStrengthUnit_summ, listPref.getEntry()));
	}
}
