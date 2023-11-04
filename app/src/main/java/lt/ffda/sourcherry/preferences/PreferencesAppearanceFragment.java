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

import static android.content.Context.UI_MODE_SERVICE;

import android.app.UiModeManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.utils.DpPxConverter;

public class PreferencesAppearanceFragment extends PreferenceFragmentCompat {
    /**
     * Handles back button and back arrow presses for the fragment
     */
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            ((PreferencesActivity) getActivity()).changeTitle(getString(R.string.options_menu_item_settings));
            getParentFragmentManager().popBackStack();
        }
    };
    SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        setPreferencesFromResource(R.xml.preferences_appearance, rootKey);

        // Listener to detect when user changes theme to apply it
        ListPreference darkModeListPreference = findPreference("preferences_dark_mode");
        darkModeListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                switch (newValue.toString()) {
                    case "System":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            UiModeManager uiModeManager = (UiModeManager) getActivity().getSystemService(UI_MODE_SERVICE);
                            uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        }
                        return true;
                    case "Light":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            UiModeManager uiModeManager = (UiModeManager) getActivity().getSystemService(UI_MODE_SERVICE);
                            uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }
                        return true;
                    case "Dark":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            UiModeManager uiModeManager = (UiModeManager) getActivity().getSystemService(UI_MODE_SERVICE);
                            uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES);
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
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
                SharedPreferences.Editor sharedPrefEditor = PreferencesAppearanceFragment.this.sharedPreferences.edit();
                sharedPrefEditor.putInt("paddingStart", DpPxConverter.dpToPx((int) newValue));
                sharedPrefEditor.commit();
                return true;
            }
        });

        SeekBarPreference paddingEndPreference = findPreference("preferences_category_padding_end");
        paddingEndPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                SharedPreferences.Editor sharedPrefEditor = PreferencesAppearanceFragment.this.sharedPreferences.edit();
                sharedPrefEditor.putInt("paddingEnd", DpPxConverter.dpToPx((int) newValue));
                sharedPrefEditor.commit();
                return true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((PreferencesActivity) getActivity()).changeTitle(getString(R.string.preferences_appearance));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    getActivity().onBackPressed();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
    }
}
