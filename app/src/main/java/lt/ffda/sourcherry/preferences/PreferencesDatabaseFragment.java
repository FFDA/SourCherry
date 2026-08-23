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
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.database.DatabaseVacuum;

public class PreferencesDatabaseFragment extends AbstractPreferenceFragment {

    private SharedPreferences sharedPreferences;

    /**
     * Makes preferences associated with Multifile databases visible.
     */
    private void initMultifileDatabasePreferences() {
        SwitchPreference multifileDatabaseAutoSync = findPreference("preference_multifile_auto_sync");
        if (multifileDatabaseAutoSync == null) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_show_a_preference, Toast.LENGTH_SHORT).show();
            return;
        }
        multifileDatabaseAutoSync.setVisible(true);
        SwitchPreference useEmbeddedFileNameOnDisk = findPreference("preference_multifile_use_embedded_file_name_on_disk");
        if (useEmbeddedFileNameOnDisk == null) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_show_a_preference, Toast.LENGTH_SHORT).show();
            return;
        }
        useEmbeddedFileNameOnDisk.setVisible(true);
    }

    /**
     * Makes preferences for SQL database visible. Adds listeners where needed.
     */
    private void initSqlDatabasePreferences() {
        SeekBarPreference cursorWindowSizePreference = findPreference("preferences_cursor_window_size");
        if (cursorWindowSizePreference != null) {
            cursorWindowSizePreference.setVisible(true);
        }
        Preference preferenceVacuumDatabase = findPreference("preference_vacuum_database");
        if (preferenceVacuumDatabase == null) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_show_a_preference, Toast.LENGTH_SHORT).show();
            return;
        }
        preferenceVacuumDatabase.setVisible(true);
        preferenceVacuumDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                DatabaseVacuum databaseVacuum = (DatabaseVacuum) DatabaseReaderFactory.getReader();
                databaseVacuum.vacuum();
                return true;
            }
        });
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_database, rootKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        String databaseStorageType = sharedPreferences.getString("databaseStorageType", "");
        String databaseExtension = sharedPreferences.getString("databaseFileExtension", "ctd");
        if (databaseStorageType.equals("internal")) {
            findPreference("preferences_mirror_database").setVisible(true);
        }
        if (databaseExtension.equals("ctb") || databaseExtension.equals("ctx")) {
            initSqlDatabasePreferences();
        }
        if (databaseExtension.equals("multi")) {
            initMultifileDatabasePreferences();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((PreferencesActivity) getActivity()).changeTitle(getString(R.string.preferences_database));
    }
    
}
