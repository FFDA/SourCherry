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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);

        setMessageWithDatabaseName();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button buttonOpen = (Button) findViewById(R.id.button_open);
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.openDatabase();
            }
        });

        CheckBox checkboxAutoOpen = (CheckBox) findViewById(R.id.checkBox_auto_open);
        if (checkboxAutoOpen.isChecked()) {
            if (this.sharedPref.getString("databaseUri", null) != null) {
                this.openDatabase();
            }
        }

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.setMessageWithDatabaseName();
        this.listImportedDatabases(); // Displaying databases on this step because this is the step that app returns to from other Activity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_activity_option_menu_about:
                Intent openAboutPage = new Intent(this, AboutActivity.class);
                startActivity(openAboutPage);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    ActivityResultLauncher<String[]> getDatabase = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        DocumentFile databaseDocumentFile = DocumentFile.fromSingleUri(this, result);

        if (result != null) {
            // Saving filename and path to the file in the preferences
            saveDatabaseToPrefs("shared", databaseDocumentFile.getName(), databaseDocumentFile.getName().split("\\.")[1], result.toString());
            //
            setMessageWithDatabaseName();
        }
    });

    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
    });

    public void openGetDatabase(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        // There should be an else statement here do disable Select Button and most likely other buttons

        getDatabase.launch(new String[]{"*/*",});
    }

    private void listImportedDatabases() {
        // Lists all databases that are currently in app-specific storage

        // View into which all databases will be listed to
        LinearLayout importedDatabases = findViewById(R.id.layout_imported_databases);
        importedDatabases.removeAllViews(); // Everytime all the items are removed and re-added just in case user deleted something

        TextView importedDatabasesTitle = findViewById(R.id.imported_databases_title);
        importedDatabasesTitle.setVisibility(View.INVISIBLE); // Hides "Imported Databases" title if there a no

        File databaseDir = new File(getFilesDir(), "databases");

        if (!databaseDir.exists()) {
            // If directory does not exists (when app is launched first time)
            // There this no need to continue
            // It will cause crash otherwise
            return;
        }

        if (databaseDir.list().length > 0) {
            // If there are any databases in app-specific storage
            importedDatabasesTitle.setVisibility(View.VISIBLE);

            LayoutInflater layoutInflater = this.getLayoutInflater();

            for (String databaseFilename: databaseDir.list()) {
                // Inflates database list item view
                LinearLayout importedDatabaseItem = (LinearLayout) layoutInflater.inflate(R.layout.imported_databases_item, null);

                Button databaseFilenameButton = importedDatabaseItem.findViewById(R.id.imported_databases_item_text);
                databaseFilenameButton.setText(databaseFilename); // Adds database filename do be displayed for the current database
                // If user taps on database filename
                databaseFilenameButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File selectedDatabaseToOpen = new File(databaseDir, databaseFilename);
                        // Saves selected database's information to the settings
                        saveDatabaseToPrefs("internal", databaseFilename, databaseFilename.split("\\.")[1], selectedDatabaseToOpen.getPath());
                        setMessageWithDatabaseName();
                    }
                });
                // If user long presses database filename
                databaseFilenameButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        File selectedDatabaseToOpen = new File(databaseDir, databaseFilename);
                        // Saves selected database's information to the settings
                        MainActivity.this.saveDatabaseToPrefs("internal", databaseFilename, databaseFilename.split("\\.")[1], selectedDatabaseToOpen.getPath());
                        MainActivity.this.setMessageWithDatabaseName();
                        // Opens database
                        MainActivity.this.openDatabase();
                        return true;
                    }
                });

                //// Delete icon/button
                ImageButton removeDatabaseIcon = importedDatabaseItem.findViewById(R.id.imported_databases_item_image);
                removeDatabaseIcon.setOnClickListener(new View.OnClickListener() {
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
                importedDatabases.addView(importedDatabaseItem);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deleteTempFiles();
    }

    private void checkIfDeleteDatabaseIsBeingUsed(String databaseFilename) {
        // Checks if user deletes the database that is set to be opened when user presses on Open button
        // And set everything to null in (database) settings if it's true
        if (sharedPref.getString("databaseStorageType", null).equals("internal") && sharedPref.getString("databaseFilename", null).equals(databaseFilename)) {
            saveDatabaseToPrefs(null, null, null, null); // Setting database info as null that correct message for user will be displayed
            setMessageWithDatabaseName();
        }
    }

    private void setMessageWithDatabaseName() {
        EditText editTextTextPassword = findViewById(R.id.passwordField);
        TextView textViewMessage = findViewById(R.id.textViewMessage);
        TextView textViewPassword = findViewById(R.id.textViewPassword);
        Button buttonOpen = findViewById(R.id.button_open);
        CheckBox checkboxAutoOpen = (CheckBox) findViewById(R.id.checkBox_auto_open);

        checkboxAutoOpen.setChecked(this.sharedPref.getBoolean("checkboxAutoOpen", false));

        // Settings message for the user if there isn't a database selected to open
        // Otherwise displaying the name of the file
        String databaseFilename = this.sharedPref.getString("databaseFilename", null);

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
            String databaseFileExtension = this.sharedPref.getString("databaseFileExtension", null);
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
                    new Handler().postDelayed(new Runnable() {
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

    private void openDatabase() {
        String databaseFileExtension = this.sharedPref.getString("databaseFileExtension", null);
        if (this.sharedPref.getString("databaseStorageType", null).equals("shared")) {
            // A check for external databases that they still exists and app still able to read it
            // If the check fails message for user is displayed and MainView activity will not open
            Uri databaseUri = Uri.parse(this.sharedPref.getString("databaseUri", null));
            DocumentFile databaseDocumentFile = DocumentFile.fromSingleUri(this, databaseUri);
            if (!databaseDocumentFile.exists()) {
                Toast.makeText(this, R.string.toast_error_database_does_not_exists, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!databaseDocumentFile.canRead()) {
                Toast.makeText(this, R.string.toast_error_cant_read_database, Toast.LENGTH_SHORT).show();
                return;
            }

            if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
                // Password protected databases
                // Checks if there is a password in the password field before opening database
                EditText passwordField = (EditText) findViewById(R.id.passwordField);
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

    public void saveCheckboxStatus(View view) {
        CheckBox checkBoxAutoOpen = (CheckBox) findViewById(R.id.checkBox_auto_open);

        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putBoolean("checkboxAutoOpen", checkBoxAutoOpen.isChecked());
        sharedPrefEditor.commit();
    }

    private void saveDatabaseToPrefs(String databaseStorageType, String databaseFilename, String databaseFileExtension, String databaseUri) {
        // Saves passed information about database to preferences
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString("databaseStorageType", databaseStorageType);
        sharedPrefEditor.putString("databaseFilename", databaseFilename);
        sharedPrefEditor.putString("databaseFileExtension", databaseFileExtension);
        sharedPrefEditor.putString("databaseUri", databaseUri);
        sharedPrefEditor.apply();
    }

    private void deleteTempFiles() {
        // Deletes all file from cache (temp) directory
        File cachedFileDir = getCacheDir();

        if (cachedFileDir.list().length > 0) {
            for (String filename: cachedFileDir.list()) {
                new File(cachedFileDir, filename).delete();
            }
        }
    }

    public void startMainViewActivity() {
        // Starts MainView activity with current settings/database
        Intent openDatabase = new Intent(this, MainView.class);
        startActivity(openDatabase);
    }
}