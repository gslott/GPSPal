package se.gunnarslott.gpspal;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import se.gunnarslott.gpspal.R;

public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}