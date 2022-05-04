package com.ffda.sourcherry;

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
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setMessageWithDatabaseName();

        Button buttonOpen = (Button) findViewById(R.id.button_open);
        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
                String databaseFileExtension = sharedPref.getString("databaseFileExtension", null);
                String databaseUri = sharedPref.getString("databaseUri", null);
                if (databaseFileExtension.equals("ctd")) {
                    // Works with not protected XML based databases
                    Intent openXML = new Intent(v.getContext(), XMLView.class);
                    startActivity(openXML);
                }
//                } else if (databaseFileExtension.equals("ctb") || databaseFileExtension.equals("ctx")) {
//                    // Works with SQLite based databases
//                }
            }
        });

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
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

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
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
        EditText editTextTextPassword = findViewById(R.id.editTextTextPassword);
        TextView textViewMessage = findViewById(R.id.textViewMessage);
        TextView textViewPassword = findViewById(R.id.textViewPassword);
        Button buttonOpen = findViewById(R.id.button_open);

        // Settings message for the user if there isn't a database selected to open
        // Otherwise displaying the name of the file
        String databaseFilename = sharedPref.getString("databaseFilename", null);

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
            String databaseFileExtension = sharedPref.getString("databaseFileExtension", null);
            if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
                editTextTextPassword.setVisibility(View.VISIBLE);
                textViewPassword.setVisibility(View.VISIBLE);
                String tt = sharedPref.getString("databaseFileExtension", null);
            } else {
                String tt = sharedPref.getString("databaseFileExtension", null);
                editTextTextPassword.setVisibility(View.GONE);
                textViewPassword.setVisibility(View.GONE);
            }
        }
    }

}