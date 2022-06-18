/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import lt.ffda.sourcherry.R;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);

        setMessageWithDatabaseName();

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

        //// Disables button Create Database
        // Function isn't implemented yet
        Button createDatabaseButton = (Button) findViewById(R.id.button_create_database);
        createDatabaseButton.setEnabled(false);
        ////
    }

    @Override
    protected void onResume() {
        super.onResume();
        listImportedDatabases(); // Displaying databases on this step because this is the step that app returns to from other Activity
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

                TextView databaseFilenameTextView = importedDatabaseItem.findViewById(R.id.imported_databases_item_text);
                databaseFilenameTextView.setText(databaseFilename); // Adds database filename do be displayed for the current database

                databaseFilenameTextView.setOnClickListener(new View.OnClickListener() {
                    // If user taps on database filename
                    @Override
                    public void onClick(View v) {
                        File selectedDatabaseToOpen = new File(databaseDir, databaseFilename);
                        // Saves selected database's information to the settings
                        saveDatabaseToPrefs("internal", databaseFilename, databaseFilename.split("\\.")[1], selectedDatabaseToOpen.getPath());
                        setMessageWithDatabaseName();
                    }
                });

                //// Delete icon/button
                ImageView removeDatabaseIcon = importedDatabaseItem.findViewById(R.id.imported_databases_item_image);
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
                                        checkIfDeleteDatabaseisBeingUsed(databaseFilename);
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

    private void checkIfDeleteDatabaseisBeingUsed(String databaseFilename) {
        // Checks if user deletes the database that is set to be opened when user presses on Open button
        // And sett everything to null in (database) settings if it's true
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
            if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
                editTextTextPassword.setVisibility(View.VISIBLE);
                textViewPassword.setVisibility(View.VISIBLE);
            } else {
                editTextTextPassword.setVisibility(View.GONE);
                textViewPassword.setVisibility(View.GONE);
            }
        }
    }

    private void openDatabase() {
        String databaseFileExtension = this.sharedPref.getString("databaseFileExtension", null);
        Intent openDatabase = new Intent(this, MainView.class);

        if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
            // Password protected databases
            // Checks if there is a password in the password field before opening database
            EditText passwordField = (EditText) findViewById(R.id.passwordField);
            if (passwordField.getText().length() <= 0) {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            } else {
                this.extractDatabase();
                startActivity(openDatabase);
            }
        } else if (databaseFileExtension.equals("ctd")) {
            // XML database file
            try {
                startActivity(openDatabase);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to open database!", Toast.LENGTH_SHORT).show();
            }
        } else if (databaseFileExtension.equals("ctb")) {
            // SQLite database
            // Needs to be moved to app-specific storage to open it using SQLiteDatabase
            if (this.sharedPref.getString("databaseStorageType", null).equals("shared")) {
                this.copyDatabaseToAppSpecificStorage();
            }
            startActivity(openDatabase);
        }else {
            Toast.makeText(this,"Doesn't look like a CherryTree database", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveCheckboxStatus(View view) {
        CheckBox checkBoxAutoOpen = (CheckBox) findViewById(R.id.checkBox_auto_open);

        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putBoolean("checkboxAutoOpen", checkBoxAutoOpen.isChecked());
        sharedPrefEditor.commit();
    }

    private void extractDatabase() {
        String databaseString = sharedPref.getString("databaseUri", null);
        EditText passwordField = findViewById(R.id.passwordField);
        String password = passwordField.getText().toString();

        File databaseDir = new File(getFilesDir(), "databases");
        if (!databaseDir.exists()) {
            // If directory does not exists - create it
            databaseDir.mkdirs();
        }

        String tmpDatabaseFilename = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                //// Copying file to temporary internal apps storage (cache)
                File tmpCompressedDatabase = File.createTempFile("tmpDatabaseFile", null);
                InputStream is = getContentResolver().openInputStream(Uri.parse(databaseString));
                Files.copy(is, tmpCompressedDatabase.toPath().toRealPath(), StandardCopyOption.REPLACE_EXISTING);
                ////
                is.close();

                //// Extracting file to permanent internal apps storage
                SevenZFile sevenZFile = new SevenZFile(tmpCompressedDatabase, password.toCharArray());
                SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                tmpDatabaseFilename = entry.getName();
                while (entry != null) {
                    FileOutputStream out = new FileOutputStream(new File(databaseDir, tmpDatabaseFilename));
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);
                    out.write(content);
                    out.close();
                    entry = sevenZFile.getNextEntry();
                }
                ////

                //// Creating new settings
                // Saved Uri is not a real Uri, so don't try to use it.
                // The only reason to save it here is, because I'm using it to check if database should be opened automatically
                String[] splitExtension = tmpDatabaseFilename.split("\\."); // Splitting the path to extract the file extension.
                saveDatabaseToPrefs("internal", tmpDatabaseFilename, splitExtension[splitExtension.length - 1], databaseDir.getPath() + "/" + tmpDatabaseFilename);
                ////

                //// Cleaning up
//                tmpCompressedDatabase.delete(); // Using deleteTempFile onDestroy
                sevenZFile.close();
                ////
            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Error FileNotFoundException: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(this, "Error IOException: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Only versions SDK 26 or later supported", Toast.LENGTH_LONG).show();
        }
    }

    private void copyDatabaseToAppSpecificStorage() {
        File databaseDir = new File(getFilesDir(), "databases");

        if (!databaseDir.exists()) {
            // If directory does not exists - create it
            databaseDir.mkdirs();
        }

        String databaseOutputFile = databaseDir.getPath() + "/" + this.sharedPref.getString("databaseFilename", null);
        Uri databaseUri = Uri.parse(this.sharedPref.getString("databaseUri", null));

        try {
            InputStream databaseInputStream = getContentResolver().openInputStream(databaseUri);
            OutputStream databaseOutputStream = new FileOutputStream(databaseOutputFile, false);

            // Copying files
            byte[] buf = new byte[1024];
            int len;
            while ((len = databaseInputStream.read(buf)) > 0) {
                databaseOutputStream.write(buf, 0, len);
            }

            databaseInputStream.close();
            databaseOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not open a file to copy database", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Could not copy database", Toast.LENGTH_LONG).show();
        }

        //// Creating new settings
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString("databaseStorageType", "internal");
        // This is not a real Uri, so don't try to use it, but I use it to check if database should be opened automatically
        sharedPrefEditor.putString("databaseUri", databaseOutputFile);
        sharedPrefEditor.apply();
        ////
    }

    private void saveDatabaseToPrefs(String databaseStorageType, String databaseFilename, String databaseFileExtension, String databaseUri) {
        // Saves passed information about database to preferences
        SharedPreferences.Editor sharedPrefEditor = MainActivity.this.sharedPref.edit();
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
}