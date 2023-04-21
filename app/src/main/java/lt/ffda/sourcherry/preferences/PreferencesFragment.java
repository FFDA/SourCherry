/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.preferences;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import lt.ffda.sourcherry.R;

public class PreferencesFragment extends PreferenceFragmentCompat {
    SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Listener to detect when user changes theme to apply it
        ListPreference darkModeListPreference = findPreference("preferences_dark_mode");
        darkModeListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                switch (newValue.toString()) {
                    case "System":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        return true;
                    case "Light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        return true;
                    case "Dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        return true;
                    default:
                        return false;
                }
            }
        });

        // Listeners to detect when user changes paddings to save them in settings
        SeekBarPreference paddingStartPreference = findPreference("preferences_category_padding_start");
        paddingStartPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                SharedPreferences.Editor sharedPrefEditor = PreferencesFragment.this.sharedPreferences.edit();
                sharedPrefEditor.putInt("paddingStart", dpToPx((int) newValue));
                sharedPrefEditor.commit();
                return true;
            }
        });


        SeekBarPreference paddingEndPreference = findPreference("preferences_category_padding_end");
        paddingEndPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                SharedPreferences.Editor sharedPrefEditor = PreferencesFragment.this.sharedPreferences.edit();
                sharedPrefEditor.putInt("paddingEnd", dpToPx((int) newValue));
                sharedPrefEditor.commit();
                return true;
            }
        });
    }

    /**
     * Converts provided PX value to DP and returns it
     * @param paddingInPX padding value in PX to be converted
     * @return padding value in DP
     */
    private int pxToDp(int paddingInPX) {
        return (int) (paddingInPX / Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Converts provided DP value to PX and returns it
     * @param paddingInDP padding value in DP to be converted
     * @return padding value in PX
     */
    private int dpToPx(int paddingInDP) {
        return (int) (paddingInDP * Resources.getSystem().getDisplayMetrics().density);
    }
}
