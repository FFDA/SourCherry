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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import lt.ffda.sourcherry.dialogs.ExportDatabaseDialogFragment;
import lt.ffda.sourcherry.dialogs.MirrorDatabaseProgressDialogFragment;
import lt.ffda.sourcherry.dialogs.OpenDatabaseProgressDialogFragment;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setNightMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        setMessageWithDatabaseName();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button buttonOpen = findViewById(R.id.button_open);
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.openDatabase();
            }
        });

        // Listens when user presses "Done" (bottom right) button while typing password and opens database
        EditText editTextPassword = findViewById(R.id.passwordField);
        editTextPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handle = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handle = true;
                    MainActivity.this.openDatabase();
                }
                return handle;
            }
        });

        if (this.sharedPreferences.getBoolean("isChangingConfigurations", false)) {
            this.resetIsChangingConfigurationsValue();
        }

        // Creates internal and external folders for databases
        if (!(new File (getFilesDir(), "databases")).exists()) {
            (new File (getFilesDir(), "databases")).mkdirs();
            (new File (getExternalFilesDir(null), "databases")).mkdirs();
        }

        // If launched the app by opening a file from different app
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            DocumentFile databaseDocumentFile = DocumentFile.fromSingleUri(this, intent.getData());
            saveDatabaseToPrefs("shared", databaseDocumentFile.getName(), databaseDocumentFile.getName().split("\\.")[1], intent.getData().toString());

            setMessageWithDatabaseName();

            String databaseFileExtension = this.sharedPreferences.getString("databaseFileExtension", null);
            if (databaseFileExtension.equals("ctb") || databaseFileExtension.equals("ctd")) {
                // If database is not protected it can be opened without any user interaction
                this.openDatabase();
            }
        } else {
            // If app weren't launched by selecting a database file from external app
            CheckBox checkboxAutoOpen = findViewById(R.id.checkBox_auto_open);
            if (!this.sharedPreferences.getBoolean("isChangingConfigurations", false)) {
                if (checkboxAutoOpen.isChecked()) {
                    // All these ifs are needed
                    // startMainViewActivity() has to be launched from FragmentDialog
                    // Otherwise it will be interrupted and database won't be copied
                    if (this.sharedPreferences.getString("databaseUri", null) != null) {
                        if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
                            this.mirrorDatabase();
                        } else {
                            this.openDatabase();
                        }
                    }
                } else {
                    // If “Open this database on startup” isn't checked but "Mirror Database" might still be
                    if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
                        this.mirrorDatabase();
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.setMessageWithDatabaseName();
        this.listImportedDatabases(); // Displaying databases on this step because this is the step that app returns to from other Activity
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.deleteTempFiles();
        if (isChangingConfigurations()) {
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("isChangingConfigurations", true);
            sharedPreferencesEditor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_main_activity, menu);
        menu.findItem(R.id.options_menu_external_storage).setChecked(sharedPreferences.getBoolean("preferences_external_storage", false));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.options_menu_about) {
            Intent openAboutPage = new Intent(this, AboutActivity.class);
            startActivity(openAboutPage);
            return true;
        } else if (item.getItemId() == R.id.options_menu_external_storage) {
            item.setChecked(!item.isChecked());
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("preferences_external_storage", item.isChecked());
            sharedPreferencesEditor.commit();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Registers activity for user to select a document
     * Gets permanent read and write permissions for selected document
     * Saved necessary data in the preferences
     */
    ActivityResultLauncher<String[]> getDatabase = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        if (result != null) {
            DocumentFile databaseDocumentFile = DocumentFile.fromSingleUri(this, result);
            String databaseFileExtension = databaseDocumentFile.getName().split("\\.")[1];
            // Saving filename and path to the file in the preferences
            this.saveDatabaseToPrefs("shared", databaseDocumentFile.getName(), databaseFileExtension, result.toString());
            //
            if (databaseDocumentFile.getName().split("\\.")[1].equals("ctd")) {
                // Only if user selects ctd (not protected xml database) permanent permission should be requested
                // When uri is received from intent-filter app will crash
                getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            this.resetMirrorDatabasePreferences();
            this.setMessageWithDatabaseName();
        }
    });

    /**
     * Launches activity for user to select a database
     * @param view button that was clicked
     */
    public void openGetDatabase(View view) {
        getDatabase.launch(new String[]{"*/*",});
    }

    /**
     * Lists all imported databases in IU
     */
    private void listImportedDatabases() {
        // View into which all databases will be listed to
        LinearLayout importedDatabases = findViewById(R.id.layout_imported_databases);
        importedDatabases.removeAllViews(); // Everytime all the items are removed and re-added just in case user deleted something

        File databaseDir = new File(getFilesDir(), "databases");
        LayoutInflater layoutInflater = null;

        // If there are any databases in app-specific storage
        if (databaseDir.list().length > 0) {
            layoutInflater = this.getLayoutInflater();
            for (String databaseFilename: databaseDir.list()) {
                if (!databaseFilename.endsWith("-journal")) {
                    importedDatabases.addView(this.createImportedDatabaseListItem(layoutInflater, databaseDir, databaseFilename));
                }
            }
        }

        // If there are any databases in external storage
        databaseDir = new File(getExternalFilesDir(null), "databases");
        if (databaseDir.list() != null && databaseDir.list().length > 0) {
            if (layoutInflater == null) {
                layoutInflater = this.getLayoutInflater();
            }
            for (String databaseFilename: databaseDir.list()) {
                if (!databaseFilename.endsWith("-journal")) {
                    importedDatabases.addView(this.createImportedDatabaseListItem(layoutInflater, databaseDir, databaseFilename));
                }
            }
        }
        
        TextView importedDatabasesTitle = findViewById(R.id.imported_databases_title);
        if (importedDatabases.getChildCount() > 0) {
            importedDatabasesTitle.setVisibility(View.VISIBLE);
        } else {
            importedDatabasesTitle.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Creates an imported database list item from provided information to be displayed for the user
     * Adds functions to select a database to be opened, open (using long tap), export and delete
     * @param layoutInflater layout inflater to inflate new menu time layout
     * @param databaseDir File object representing directory in which item is located.
     * @param databaseFilename filename if the database for which imported database item is being created
     * @return imported database list item
     */
    private LinearLayout createImportedDatabaseListItem(LayoutInflater layoutInflater,File databaseDir, String databaseFilename) {
        // Inflates database list item view
        LinearLayout importedDatabaseItem = (LinearLayout) layoutInflater.inflate(R.layout.item_imported_databases, null);

        Button databaseFilenameButton = importedDatabaseItem.findViewById(R.id.imported_databases_item_text);
        databaseFilenameButton.setText(getString(R.string.main_activity_imported_databases_item_internal, databaseFilename)); // Adds database filename do be displayed for the current database
        // If user taps on database filename
        databaseFilenameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File selectedDatabaseToOpen = new File(databaseDir, databaseFilename);
                // Saves selected database's information to the settings
                MainActivity.this.saveDatabaseToPrefs("internal", databaseFilename, databaseFilename.split("\\.")[1], selectedDatabaseToOpen.getPath());
                MainActivity.this.resetMirrorDatabasePreferences();
                MainActivity.this.setMessageWithDatabaseName();
            }
        });
        // If user long presses database filename
        databaseFilenameButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                File selectedDatabaseToOpen = new File(databaseDir, databaseFilename);
                // Saves selected database's information to the settings
                MainActivity.this.saveDatabaseToPrefs("internal", databaseFilename, databaseFilename.split("\\.")[1], selectedDatabaseToOpen.getPath());
                MainActivity.this.resetMirrorDatabasePreferences();
                MainActivity.this.setMessageWithDatabaseName();
                // Opens database
                MainActivity.this.openDatabase();
                return true;
            }
        });

        ImageButton exportDatabaseButton = importedDatabaseItem.findViewById(R.id.imported_databases_item_export_button);
        exportDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.exportDatabaseToFile.launch(databaseFilename);
            }
        });

        //// Delete icon/button
        ImageButton removeDatabaseButton = importedDatabaseItem.findViewById(R.id.imported_databases_item_delete_button);
        removeDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If user taps on delete (trashcan image) icon
                // confirmation dialog window for deletion is displayed
                AlertDialog.Builder confirmDeletion = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(databaseFilename)
                        .setMessage(R.string.main_activity_imported_databases_delete_dialog_message)
                        .setPositiveButton(R.string.button_delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File selectedDatabaseToDelete = new File(databaseDir, databaseFilename);
                                selectedDatabaseToDelete.delete(); // Deletes database file
                                checkIfDeleteDatabaseIsBeingUsed(databaseFilename);
                                listImportedDatabases(); // Launches this function to make a new list of imported databases
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                confirmDeletion.show();
            }
        });
        ////
        return importedDatabaseItem;
    }

    /**
     * Launches file chooser to select location
     * where to export database. If user chooses a file - launches a
     * export dialog fragment
     */
    ActivityResultLauncher<String> exportDatabaseToFile = registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), result -> {
        if (result != null) {
            Bundle bundle = new Bundle();
            bundle.putString("exportFileUri", result.toString());
            ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
            exportDatabaseDialogFragment.setArguments(bundle);
            exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
        }
    });

    /**
     * Checks if user deletes the database that is set to be opened when user presses on Open button
     * And set everything to null in settings if it's true
     * @param databaseFilename database filename that user wants to delete
     */
    private void checkIfDeleteDatabaseIsBeingUsed(String databaseFilename) {
        String databaseStorageType = this.sharedPreferences.getString("databaseStorageType", null);
        String databaseFilenameFromPreference = this.sharedPreferences.getString("databaseFilename", null);
        if (databaseStorageType != null && databaseFilenameFromPreference != null && databaseStorageType.equals("internal") && databaseFilenameFromPreference.equals(databaseFilename)) {
            saveDatabaseToPrefs(null, null, null, null); // Setting database info as null that correct message for user will be displayed
            resetMirrorDatabasePreferences();
            setMessageWithDatabaseName();
        }
    }

    /**
     * Sets UI messages for the user with appropriate database name
     * Shows password field for password protected databases
     * Set checkbox state depending on what's saved in database
     */
    private void setMessageWithDatabaseName() {
        EditText editTextTextPassword = findViewById(R.id.passwordField);
        TextView textViewMessage = findViewById(R.id.textViewMessage);
        TextView textViewPassword = findViewById(R.id.textViewPassword);
        Button buttonOpen = findViewById(R.id.button_open);
        CheckBox checkboxAutoOpen = findViewById(R.id.checkBox_auto_open);

        checkboxAutoOpen.setChecked(this.sharedPreferences.getBoolean("checkboxAutoOpen", false));

        // Settings message for the user if there isn't a database selected to open
        // Otherwise displaying the name of the file
        String databaseFilename = this.sharedPreferences.getString("databaseFilename", null);

        if (databaseFilename == null) {
            // No file is selected
            // Displays a message and hides password label and field
            // Disables Open button
            textViewMessage.setText(R.string.text_message_no_database);
            editTextTextPassword.setVisibility(View.GONE);
            textViewPassword.setVisibility(View.GONE);
            buttonOpen.setEnabled(false);
        } else {
            // File selected. Displays filename
            // If file extension is for password protected file. Shows password field and label
            // Enabled Open button
            textViewMessage.setText(databaseFilename);
            buttonOpen.setEnabled(true);
            String databaseFileExtension = this.sharedPreferences.getString("databaseFileExtension", null);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
                // Password protected databases
                editTextTextPassword.getText().clear();
                editTextTextPassword.setVisibility(View.VISIBLE);
                editTextTextPassword.requestFocus();
                textViewPassword.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Shows keyboard on API 30 (Android 11) reliably
                    WindowCompat.getInsetsController(getWindow(), editTextTextPassword).show(WindowInsetsCompat.Type.ime());
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        // Delays to show soft keyboard by few milliseconds
                        // Otherwise keyboard does not show up
                        // It's a bit hacky (should be fixed)
                        @Override
                        public void run() {
                            imm.showSoftInput(editTextTextPassword, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 20);
                }
            } else {
                editTextTextPassword.setVisibility(View.GONE);
                textViewPassword.setVisibility(View.GONE);
                // Hides keyboard
                imm.hideSoftInputFromWindow(editTextTextPassword.getWindowToken(), 0);
            }
        }
    }

    /**
     * Depending of where database is located and if it is password protected this function
     * Checks password field for password protected databases
     * and launches OpenDatabaseProgressDialogFragment fragment to import database to app-specific storage
     * If database already is in app-specific storage or database format is not password protected XML database (ctd extension)
     * launches MainView with database that is saved in settings
     */
    private void openDatabase() {
        String databaseFileExtension = this.sharedPreferences.getString("databaseFileExtension", null);
        if (this.sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
            // A check for external databases that they still exists and app still able to read it
            // If the check fails message for user is displayed and MainView activity will not open
            Uri databaseUri = Uri.parse(this.sharedPreferences.getString("databaseUri", null));
            DocumentFile databaseDocumentFile = DocumentFile.fromSingleUri(this, databaseUri);
            if (!databaseDocumentFile.exists()) {
                Toast.makeText(this, R.string.toast_error_database_does_not_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            // Disabled because clashed with intent-filters
            // Delete later if no issues arise
//            if (!databaseDocumentFile.canRead()) {
//                Toast.makeText(this, R.string.toast_error_cant_read_database, Toast.LENGTH_SHORT).show();
//                return;
//            }

            if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
                // Password protected databases
                // Checks if there is a password in the password field before opening database
                EditText passwordField = findViewById(R.id.passwordField);
                if (passwordField.getText().length() <= 0) {
                    Toast.makeText(this, R.string.toast_message_please_enter_password, Toast.LENGTH_SHORT).show();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("password", ((TextView) findViewById(R.id.passwordField)).getText().toString());
                    OpenDatabaseProgressDialogFragment openDatabaseProgressDialogFragment = new OpenDatabaseProgressDialogFragment();
                    openDatabaseProgressDialogFragment.setArguments(bundle);
                    openDatabaseProgressDialogFragment.show(getSupportFragmentManager(), "progressDialog");
                }
            } else if (databaseFileExtension.equals("ctd")) {
                // XML database file
                try {
                    this.startMainViewActivity();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.toast_error_failed_to_open_database, Toast.LENGTH_SHORT).show();
                }
            } else if (databaseFileExtension.equals("ctb")) {
                // SQLite database
                // Needs to be moved to app-specific storage to open it using SQLiteDatabase
                OpenDatabaseProgressDialogFragment openDatabaseProgressDialogFragment = new OpenDatabaseProgressDialogFragment();
                openDatabaseProgressDialogFragment.show(getSupportFragmentManager(), "progressDialog");
            } else {
                Toast.makeText(this,R.string.toast_error_does_not_look_like_a_cherrytree_database, Toast.LENGTH_SHORT).show();
            }
        } else {
            // If database is in app-specific storage there is no need for any processing
            this.startMainViewActivity();
        }
    }

    /**
     * Function that saves checkbox state to preferences
     * on every checkbox tap
     * @param view checkbox that was clicked
     */
    public void saveCheckboxStatus(View view) {
        CheckBox checkBoxAutoOpen = findViewById(R.id.checkBox_auto_open);
        SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("checkboxAutoOpen", checkBoxAutoOpen.isChecked());
        sharedPreferencesEditor.commit();
    }

    /**
     * Saves passed information about database to preferences
     * @param databaseStorageType values can be "shared" or "internal"
     * @param databaseFilename database filename with extension
     * @param databaseFileExtension database extension
     * @param databaseUri uri for shared databases and path for internal databases
     */
    private void saveDatabaseToPrefs(String databaseStorageType, String databaseFilename, String databaseFileExtension, String databaseUri) {
        SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
        sharedPreferencesEditor.putString("databaseStorageType", databaseStorageType);
        sharedPreferencesEditor.putString("databaseFilename", databaseFilename);
        sharedPreferencesEditor.putString("databaseFileExtension", databaseFileExtension);
        sharedPreferencesEditor.putString("databaseUri", databaseUri);
        if (this.sharedPreferences.getBoolean("restore_last_node", true) && this.sharedPreferences.getString("last_node_name", null) != null) {
            // If last node from previous database was saved - resets values
            sharedPreferencesEditor.putString("last_node_name", null);
            sharedPreferencesEditor.putString("last_node_unique_id", null);
            sharedPreferencesEditor.putString("last_node_has_subnodes", null);
            sharedPreferencesEditor.putString("last_node_is_parent", null);
            sharedPreferencesEditor.putString("last_node_is_subnode", null);
            sharedPreferencesEditor.putInt("last_node_position", -1);
        }
        sharedPreferencesEditor.apply();
    }

    /**
     * Resets Mirror Database preferences to default values
     * and disables Mirror Database function
     * if this function is turned on
     */
    private void resetMirrorDatabasePreferences() {
        if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
//        sharedPreferencesEditor.remove("mirrorDatabaseFolderUri"); // Not necessary to delete, user most likely will use the same folder in the future
            sharedPreferencesEditor.remove("mirrorDatabaseLastModified");
            sharedPreferencesEditor.remove("mirrorDatabaseFilename");
            sharedPreferencesEditor.putBoolean("mirror_database_switch", false);
            sharedPreferencesEditor.commit();
        }
    }

    /**
     * Deletes all file from cache (temp) directory
     */
    private void deleteTempFiles() {
        File cachedFileDir = getCacheDir();
        if (cachedFileDir.list().length > 0) {
            for (String filename: cachedFileDir.list()) {
                new File(cachedFileDir, filename).delete();
            }
        }
    }

    /**
     * Starts MainView activity with current settings/database
     */
    public void startMainViewActivity() {
        Intent openDatabase = new Intent(this, MainView.class);
        startActivity(openDatabase);
    }

    /**
     * Sets theme depending on user selected setting
     */
    private void setNightMode() {
        SharedPreferences sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(this);
        switch (sharedPreferencesEditor.getString("preferences_dark_mode", "System")) {
            case "System":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    /**
     * Reset preference variable that marks that activity/app is restarting for configuration change
     */
    private void resetIsChangingConfigurationsValue() {
        SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("isChangingConfigurations", false);
        sharedPreferencesEditor.commit();
    }

    /**
     * Checks if database mirror file has been modified from the last startup
     * If mirror database was modified - copies the mirror database into app-specific storage
     */
    private void mirrorDatabase() {
        // Variables that will be put into bundle for MirrorDatabaseProgressDialogFragment
        Uri mirrorDatabaseFileUri = null; // Uri to the Mirror Database File inside Mirror Database Folder
        long mirrorDatabaseDocumentFileLastModified = 0;
        String mirrorDatabaseFileExtension = null;

        // Reading through files inside Mirror Database Folder
        Uri mirrorDatabaseFolderUri = Uri.parse(this.sharedPreferences.getString("mirrorDatabaseFolderUri", null));
        Uri mirrorDatabaseFolderChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDatabaseFolderUri, DocumentsContract.getTreeDocumentId(mirrorDatabaseFolderUri));

        Cursor cursor = this.getContentResolver().query(mirrorDatabaseFolderChildrenUri, new String[]{"document_id", "_display_name", "last_modified"}, null, null, null);
        while (cursor != null && cursor.moveToNext()) {
            if (cursor.getString(1).equals(this.sharedPreferences.getString("mirrorDatabaseFilename", null))) {
                // if file with the Mirror Database File filename was wound inside Mirror Database Folder
                mirrorDatabaseFileUri = DocumentsContract.buildDocumentUriUsingTree(mirrorDatabaseFolderUri, cursor.getString(0));
                mirrorDatabaseFileExtension = cursor.getString(1).split("\\.")[1];
                mirrorDatabaseDocumentFileLastModified = cursor.getLong(2);
                break;
            }
        }
        cursor.close();

        // If found Mirror Database File's last modified time is bigger than saved from previous database update
        if (mirrorDatabaseDocumentFileLastModified > this.sharedPreferences.getLong("mirrorDatabaseLastModified", 0)) {
            if (mirrorDatabaseFileExtension.equals("ctz") || mirrorDatabaseFileExtension.equals("ctx") || mirrorDatabaseFileExtension.equals("ctb")) {
                Bundle bundle = new Bundle();
                bundle.putLong("mirrorDatabaseLastModified", mirrorDatabaseDocumentFileLastModified);
                bundle.putString("mirrorDatabaseUri", mirrorDatabaseFileUri.toString());
                bundle.putString("mirrorDatabaseFileExtension", mirrorDatabaseFileExtension);
                MirrorDatabaseProgressDialogFragment mirrorDatabaseProgressDialogFragment = new MirrorDatabaseProgressDialogFragment();
                mirrorDatabaseProgressDialogFragment.setArguments(bundle);
                mirrorDatabaseProgressDialogFragment.show(getSupportFragmentManager(), "mirrorDatabaseProgressDialog");
            }
        } else {
            if (this.sharedPreferences.getBoolean("checkboxAutoOpen", false)) {
                this.openDatabase();
            }
        }
    }
}