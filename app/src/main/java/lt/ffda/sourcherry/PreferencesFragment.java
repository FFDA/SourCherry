/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Listener to detect when user changes theme to apply it
        ListPreference darkModeListPreference = findPreference("preferences_category_dark_mode");
        darkModeListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                switch (newValue.toString()) {
                    case "System":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        PreferencesFragment.this.stopAutomaticallyOpeningDatabases();
                        return true;
                    case "Light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        PreferencesFragment.this.stopAutomaticallyOpeningDatabases();
                        return true;
                    case "Dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        PreferencesFragment.this.stopAutomaticallyOpeningDatabases();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    private void stopAutomaticallyOpeningDatabases() {
        // Stops opening databases automatically on start up
        // Needed because otherwise everytime user would change theme settings and mainview theme would be added to backstack
        // Moreover, setting would close and mainview would be loaded
        SharedPreferences.Editor sharedPrefEditor = getContext().getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE).edit();
        sharedPrefEditor.putBoolean("checkboxAutoOpen", false);
        sharedPrefEditor.commit();
    }
}
