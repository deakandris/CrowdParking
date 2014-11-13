package hu.bme.tmit.deakandras.crowdparking.activity;

import hu.bme.tmit.deakandras.crowdparking.R;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_MAX_WALK_DISTANCE_PREFERENCE = "max_walk_distance_preference";
	public static final String KEY_MAX_SEARCH_TIME_PREFERENCE = "max_search_time_preference";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		// load user preferences
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		if (sharedPreferences != null) {
			Preference walkDistance = findPreference(KEY_MAX_WALK_DISTANCE_PREFERENCE);
			if (walkDistance != null) {
				walkDistance.setSummary(sharedPreferences.getString(
						KEY_MAX_WALK_DISTANCE_PREFERENCE, "")
						+ ' ' + getResources().getString(
								R.string.max_walk_distance_postfix));
			}
			Preference searchTime = findPreference(KEY_MAX_SEARCH_TIME_PREFERENCE);
			if (searchTime != null) {
				searchTime.setSummary(sharedPreferences.getString(
						KEY_MAX_SEARCH_TIME_PREFERENCE, "")
						+ ' ' + getResources().getString(
								R.string.max_search_time_postfix));
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_MAX_WALK_DISTANCE_PREFERENCE)) {
			Preference walkDistance = findPreference(key);
			// Set summary to be the user-description for the selected value
			walkDistance.setSummary(sharedPreferences.getString(key, "")
					+ ' ' + getResources().getString(
							R.string.max_walk_distance_postfix));
		} else if (key.equals(KEY_MAX_SEARCH_TIME_PREFERENCE)) {
			Preference searchTime = findPreference(key);
			// Set summary to be the user-description for the selected value
			searchTime.setSummary(sharedPreferences.getString(key, "")
					+ ' ' + getResources()
							.getString(R.string.max_search_time_postfix));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}
}