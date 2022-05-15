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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
        // Adding persistent read and write permissions. Not sure if actually working yet.
        getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION & Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        //
        
        String decodedUri = Uri.decode(result.getEncodedPath()); // Decoding the path to file to make it more readable
        String[] splitFilename = decodedUri.split("/"); // Splitting the path to extract the filename
        String[] splitExtension = decodedUri.split("\\."); // Splitting the path to extract the file extension.

        // Saving filename and path to the file in the preferences
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();

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
        if (this.sharedPref.getString("databaseFileExtension", null).equals("ctx") || this.sharedPref.getString("databaseFileExtension", null).equals("ctz")) {
            // Checks if there is a password in the password field before opening database
            EditText passwordField = (EditText) findViewById(R.id.passwordField);
            if (passwordField.getText().length() <= 0) {
                Toast.makeText(this,"Please enter password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Not implemented yet!", Toast.LENGTH_SHORT).show();
            }
        } else {
            try {
                Intent openDatabase = new Intent(this, MainView.class);
                startActivity(openDatabase);
            } catch (Exception e) {
                Toast.makeText(this,"Failed to open database!", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void saveCheckboxStatus(View view) {
        CheckBox checkBoxAutoOpen = (CheckBox) findViewById(R.id.checkBox_auto_open);

        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putBoolean("checkboxAutoOpen", checkBoxAutoOpen.isChecked());
        sharedPrefEditor.commit();
    }

}