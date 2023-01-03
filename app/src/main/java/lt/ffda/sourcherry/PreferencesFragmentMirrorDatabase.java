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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Date;

public class PreferencesFragmentMirrorDatabase extends PreferenceFragmentCompat {
    SharedPreferences sharedPreferences;
    Preference mirrorDatabaseFolder;
    Preference mirrorDatabaseFile;
    Preference mirrorDatabaseFileLastModified;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_mirror_database, rootKey);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        SwitchPreferenceCompat mirrorDatabaseSwitch = findPreference("mirror_database_switch");
        this.mirrorDatabaseFolder = findPreference("mirror_database_folder_preference");
        this.mirrorDatabaseFile = findPreference("mirror_database_file_preference");
        this.mirrorDatabaseFileLastModified = findPreference("mirror_database_last_modified_preference");

        // Enabling "Mirror database file" preference if "Mirror database" switch is enabled
        if (mirrorDatabaseSwitch.isChecked()) {
            this.mirrorDatabaseFolder.setEnabled(true);
            this.mirrorDatabaseFile.setEnabled(true);
            this.mirrorDatabaseFileLastModified.setVisible(true);

            long lastModifiedDate = this.sharedPreferences.getLong("mirrorDatabaseLastModified", 0);
            if (lastModifiedDate > 0) {
                this.mirrorDatabaseFileLastModified.setSummary(new Date(lastModifiedDate).toString());
            } else {
                this.mirrorDatabaseFileLastModified.setSummary(R.string.preferences_screen_mirror_database_mirror_database_file_summary);
            }
        }
        this.mirrorDatabaseFolder.setSummary(this.sharedPreferences.getString("mirrorDatabaseFolderUri", getString(R.string.preferences_screen_mirror_database_mirror_database_folder_summary)));
        this.mirrorDatabaseFile.setSummary(this.sharedPreferences.getString("mirrorDatabaseFilename", getString(R.string.preferences_screen_mirror_database_mirror_database_file_summary)));

        // Setting listener to enable/disable setting to select a mirror database file
        mirrorDatabaseSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFolder.setEnabled(true);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFile.setEnabled(true);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFileLastModified.setSummary(R.string.preferences_screen_mirror_database_mirror_database_file_summary);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFileLastModified.setVisible(true);
                } else {
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFolder.setEnabled(true);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFile.setEnabled(false);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFileLastModified.setVisible(false);
                    PreferencesFragmentMirrorDatabase.this.mirrorDatabaseFileLastModified.setSummary(R.string.preferences_screen_mirror_database_mirror_database_file_summary);
                    PreferencesFragmentMirrorDatabase.this.removeSavedMirrorDatabasePreferences();
                }
                return true;
            }
        });

        this.mirrorDatabaseFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                getMirrorDatabaseFolder.launch(null);
                return true;
            }
        });

        this.mirrorDatabaseFile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                getMirrorDatabaseFile.launch(new String[]{"*/*",});
                return true;
            }
        });
    }

    /**
     * Removes saved Mirror Database preferences
     * Used when user toggles the Mirror Database switch
     */
    private void removeSavedMirrorDatabasePreferences() {
        SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
        sharedPreferencesEditor.remove("mirrorDatabaseFilename");
        sharedPreferencesEditor.remove("mirrorDatabaseLastModified");
        sharedPreferencesEditor.commit();
        this.mirrorDatabaseFile.setSummary(R.string.preferences_screen_mirror_database_mirror_database_file_summary);
    }

    /**
     * Launches a file picker where user has to choose Mirror Database File
     * Saves Mirror Database File filename and last modified long to preferences
     * Displays Toast messages if user selects not permitted files
     */
    ActivityResultLauncher<String[]> getMirrorDatabaseFile = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        if (result != null) {
            DocumentFile databaseMirrorDocumentFile = DocumentFile.fromSingleUri(getContext(), result);
            String selectedFileExtension = databaseMirrorDocumentFile.getName().split("\\.")[1];
            if (selectedFileExtension.equals("ctb") || selectedFileExtension.equals("ctz") || selectedFileExtension.equals("ctx")) {
                //// Saving selected file to preferences
                SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
                sharedPreferencesEditor.putString("mirrorDatabaseFilename", databaseMirrorDocumentFile.getName());
                sharedPreferencesEditor.putLong("mirrorDatabaseLastModified", databaseMirrorDocumentFile.lastModified());
                sharedPreferencesEditor.commit();
                ////
                this.mirrorDatabaseFile.setSummary(this.sharedPreferences.getString("mirrorDatabaseFilename", getString(R.string.preferences_screen_mirror_database_mirror_database_file_summary)));
                this.mirrorDatabaseFileLastModified.setSummary(new Date(this.sharedPreferences.getLong("mirrorDatabaseLastModified", 0)).toString());
            } else if (selectedFileExtension.equals("ctd")) {
                // Not password protected XML databases are not supported. App opens them in place
                Toast.makeText(getContext(), R.string.toast_error_xml_databases_are_not_supported, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.toast_error_does_not_look_like_a_cherrytree_database, Toast.LENGTH_SHORT).show();
            }
        }
    });

    /**
     * Launches a file picker where user has to choose Mirror Database Folder
     * Saves Mirror Database Folder uri to preferences
     */
    ActivityResultLauncher<Uri> getMirrorDatabaseFolder = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
        if (result != null) {
            getActivity().getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //// Saving selected file to preferences
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
            sharedPreferencesEditor.putString("mirrorDatabaseFolderUri", result.toString());
            sharedPreferencesEditor.commit();
            ////
            this.mirrorDatabaseFolder.setSummary(result.toString());
        }
    });
}