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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
        Button createDatabase = (Button) findViewById(R.id.button_create_database);
        createDatabase.setEnabled(false);
        ////
    }

    ActivityResultLauncher<String[]> getDatabase = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        String decodedUri = Uri.decode(result.getEncodedPath()); // Decoding the path to file to make it more readable
        String[] splitFilename = decodedUri.split("/"); // Splitting the path to extract the filename
        String[] splitExtension = decodedUri.split("\\."); // Splitting the path to extract the file extension.

        // Saving filename and path to the file in the preferences
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();

        sharedPrefEditor.putString("databaseStorageType", "shared");
        sharedPrefEditor.putString("databaseFilename", splitFilename[splitFilename.length - 1]);
        sharedPrefEditor.putString("databaseFileExtension", splitExtension[splitExtension.length - 1]);
        sharedPrefEditor.putString("databaseUri", result.toString());
        sharedPrefEditor.apply();
        //

        setMessageWithDatabaseName();
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
//                Intent openDatabase = new Intent(this, MainView.class);
                startActivity(openDatabase);
            }
        } else if (databaseFileExtension.equals("ctd")) {
            // XML database file
            try {
//                Intent openDatabase = new Intent(this, MainView.class);
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
//            Intent openDatabase = new Intent(this, MainView.class);
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
                SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
                String[] splitExtension = tmpDatabaseFilename.split("\\."); // Splitting the path to extract the file extension.

                sharedPrefEditor.putString("databaseStorageType", "internal");
                sharedPrefEditor.putString("databaseFilename", tmpDatabaseFilename);
                sharedPrefEditor.putString("databaseFileExtension", splitExtension[splitExtension.length - 1]);
                // This is not a real Uri, so don't try to use it, but I use it to check if database should be opened automatically
                sharedPrefEditor.putString("databaseUri", databaseDir.getPath() + "/" + tmpDatabaseFilename);
                sharedPrefEditor.apply();
                ////

                //// Cleaning up
                tmpCompressedDatabase.delete();
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
}