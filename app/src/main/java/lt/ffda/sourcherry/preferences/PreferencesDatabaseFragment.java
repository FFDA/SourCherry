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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Date;

import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.database.DatabaseVacuum;
import lt.ffda.sourcherry.utils.Filenames;

public class PreferencesDatabaseFragment extends PreferenceFragmentCompat {
    private SharedPreferences sharedPreferences;
    private SwitchPreferenceCompat mirrorDatabaseSwitch;
    private Preference mirrorDatabaseFolder;
    /**
     * Launches a file picker where user has to choose Mirror Database Folder
     * Saves Mirror Database Folder uri to preferences
     */
    ActivityResultLauncher<Uri> getMirrorDatabaseFolder = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
        if (result != null) {
            getActivity().getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            //// Saving selected file to preferences
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putString("mirrorDatabaseFolderUri", result.toString());
            sharedPreferencesEditor.commit();
            ////
            mirrorDatabaseFolder.setSummary(result.toString());
        }
    });
    private Preference mirrorDatabaseFile;
    private Preference mirrorDatabaseFileLastModified;
    /**
     * Launches a file picker where user has to choose Mirror Database File
     * Saves Mirror Database File filename and last modified long to preferences
     * Displays Toast messages if user selects not permitted files
     */
    ActivityResultLauncher<String[]> getMirrorDatabaseFile = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        if (result != null) {
            DocumentFile databaseMirrorDocumentFile = DocumentFile.fromSingleUri(getContext(), result);
            String selectedFileExtension = Filenames.getFileExtension(databaseMirrorDocumentFile.getName());
            if (selectedFileExtension.equals("ctb") || selectedFileExtension.equals("ctz") || selectedFileExtension.equals("ctx")) {
                String currentDatabaseType = sharedPreferences.getString("databaseFileExtension", null).equals("ctd") ? "xml" : "sql";
                String selectedDatabaseType = selectedFileExtension.equals("ctz") ? "xml" : "sql";
                if (currentDatabaseType.equals(selectedDatabaseType)) {
                    //// Saving selected file to preferences
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString("mirrorDatabaseFilename", databaseMirrorDocumentFile.getName());
                    sharedPreferencesEditor.putLong("mirrorDatabaseLastModified", databaseMirrorDocumentFile.lastModified());
                    sharedPreferencesEditor.commit();
                    ////
                    mirrorDatabaseFile.setSummary(sharedPreferences.getString("mirrorDatabaseFilename", getString(R.string.preferences_mirror_database_mirror_database_file_summary)));
                    mirrorDatabaseFileLastModified.setSummary(new Date(sharedPreferences.getLong("mirrorDatabaseLastModified", 0)).toString());
                } else {
                    Toast.makeText(getContext(), R.string.toast_message_incompatible_database_types, Toast.LENGTH_SHORT).show();
                }
            } else if (selectedFileExtension.equals("ctd")) {
                // Not password protected XML databases are not supported. App opens them in place
                Toast.makeText(getContext(), R.string.toast_error_xml_databases_are_not_supported, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.toast_error_does_not_look_like_a_cherrytree_database, Toast.LENGTH_SHORT).show();
            }
        }
    });
    /**
     * Handles back button and back arrow presses for the fragment
     */
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (mirrorDatabaseSwitch.isChecked() && mirrorDatabaseFile.getSummary().equals(getContext().getString(R.string.preferences_mirror_database_mirror_database_file_summary))) {
                createConfirmationDialog();
            } else {
                ((PreferencesActivity) getActivity()).changeTitle(getString(R.string.options_menu_item_settings));
                getParentFragmentManager().popBackStack();
            }
        }
    };

    /**
     * Shows alert dialog to user with two options
     * Select a mirror database or leave. Leaving will disabled Mirror database preference
     */
    private void createConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.preferences_mirror_database_mirror_database_file_summary);
        builder.setMessage(R.string.alert_dialog_warning_no_mirror_database_selected_message);
        builder.setPositiveButton(R.string.button_leave, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PreferencesDatabaseFragment.this.mirrorDatabaseSwitch.setChecked(false);
                ((PreferencesActivity) getActivity()).changeTitle(getString(R.string.options_menu_item_settings));
                getParentFragmentManager().popBackStack();
            }
        });
        builder.setNegativeButton(R.string.button_select, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getMirrorDatabaseFile.launch(new String[]{"*/*",});
            }
        });
        builder.show();
    }

    /**
     * Makes all elements associated with MirrorDatabase visible. Adds listeners where needed.
     */
    private void initMirrorDatabasePreferences() {
        PreferenceCategory mirrorDatabaseCategory = findPreference("preferences_category_mirror_database");
        if (mirrorDatabaseCategory == null) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_show_a_preference, Toast.LENGTH_SHORT).show();
            return;
        }
        mirrorDatabaseCategory.setVisible(true);
        // Enabling "Mirror database file" preference if "Mirror database" switch is enabled
        if (mirrorDatabaseSwitch.isChecked()) {
            mirrorDatabaseFolder.setEnabled(true);
            mirrorDatabaseFile.setEnabled(true);
            mirrorDatabaseFileLastModified.setVisible(true);

            long lastModifiedDate = sharedPreferences.getLong("mirrorDatabaseLastModified", 0);
            if (lastModifiedDate > 0) {
                mirrorDatabaseFileLastModified.setSummary(new Date(lastModifiedDate).toString());
            } else {
                mirrorDatabaseFileLastModified.setSummary(R.string.preferences_mirror_database_mirror_database_file_summary);
            }
        }
        mirrorDatabaseFolder.setSummary(sharedPreferences.getString("mirrorDatabaseFolderUri", getString(R.string.preferences_mirror_database_mirror_database_folder_summary)));
        mirrorDatabaseFile.setSummary(sharedPreferences.getString("mirrorDatabaseFilename", getString(R.string.preferences_mirror_database_mirror_database_file_summary)));

        // Setting listener to enable/disable setting to select a mirror database file
        mirrorDatabaseSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    mirrorDatabaseFileLastModified.setSummary(R.string.preferences_mirror_database_mirror_database_file_summary);
                    mirrorDatabaseFileLastModified.setVisible(true);
                } else {
                    mirrorDatabaseFileLastModified.setVisible(false);
                    mirrorDatabaseFileLastModified.setSummary(R.string.preferences_mirror_database_mirror_database_file_summary);
                    removeSavedMirrorDatabasePreferences();
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
     * Makes preferences associated with Multifile databases visible.
     */
    private void initMultifileDatabasePreferences() {
        SwitchPreference multifileDatabaseAutoSync = findPreference("preference_multifile_auto_sync");
        if (multifileDatabaseAutoSync == null) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_show_a_preference, Toast.LENGTH_SHORT).show();
            return;
        }
        multifileDatabaseAutoSync.setVisible(true);
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

        mirrorDatabaseSwitch = findPreference("mirror_database_switch");
        mirrorDatabaseFolder = findPreference("mirror_database_folder_preference");
        mirrorDatabaseFile = findPreference("mirror_database_file_preference");
        mirrorDatabaseFileLastModified = findPreference("mirror_database_last_modified_preference");

        String databaseStorageType = sharedPreferences.getString("databaseStorageType", "");
        String databaseExtension = sharedPreferences.getString("databaseFileExtension", "ctd");
        if (databaseStorageType.equals("internal")) {
            initMirrorDatabasePreferences();
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
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
    }

    /**
     * Removes saved Mirror Database preferences
     * Used when user toggles the Mirror Database switch
     */
    private void removeSavedMirrorDatabasePreferences() {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.remove("mirrorDatabaseFilename");
        sharedPreferencesEditor.remove("mirrorDatabaseLastModified");
        sharedPreferencesEditor.commit();
        mirrorDatabaseFile.setSummary(R.string.preferences_mirror_database_mirror_database_file_summary);
    }
}
