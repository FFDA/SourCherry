/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;

public class SaveOpenDialogFragment extends DialogFragment {
    private DatabaseReader reader;
    private String nodeUniqueID;
    private String filename;
    private String time;
    private String fileMimeType;
    private CheckBox rememberChoice;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        // For consistency and laziness buttons were mixed.
        // NeutralButton will cancel dialog and NegativeButton will be for saving the file
        super.onCreateDialog(savedInstanceState);

        this.reader = ((MainView) getActivity()).getReader(); // reader from main MainView

        this.nodeUniqueID = getArguments().getString("nodeUniqueID", null);
        this.filename = getArguments().getString("filename", null); // Filename passed to fragment
        this.time = getArguments().getString("time");
        this.fileMimeType = getArguments().getString("fileMimeType");

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_open_fragment, null);

        builder.setView(view)
            .setTitle(R.string.save_open_dialog_fragment_title)
            .setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (SaveOpenDialogFragment.this.rememberChoice.isChecked()) {
                        // Saving preference if checkbox is checked
                        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                        sharedPreferencesEditor.putString("preferences_save_open_file", "Open");
                        sharedPreferencesEditor.commit();
                    }
                    openFile();
                }
            })
            .setNeutralButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            })
            .setNegativeButton(R.string.button_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {

                }
            });

        /// Part of layout that changes depending on how many files with same filename there are in node
        this.rememberChoice = view.findViewById(R.id.dialog_save_open_fragment_remember_choice_checkBox);
        TextView textViewFilename = view.findViewById(R.id.dialog_save_open_fragment_filename_textview);
        textViewFilename.setText(filename);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        AlertDialog alertDialog = (AlertDialog) getDialog();
        //// Save button
        // This is needed to keep activity alive while file is being written
        Button saveButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SaveOpenDialogFragment.this.rememberChoice.isChecked()) {
                    // Saving preference if checkbox is checked
                    SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                    sharedPreferencesEditor.putString("preferences_save_open_file", "Save");
                    sharedPreferencesEditor.commit();
                }
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType(SaveOpenDialogFragment.this.fileMimeType);
                intent.putExtra(Intent.EXTRA_TITLE, SaveOpenDialogFragment.this.filename);
                saveFile.launch(intent);
            }
        });
        ////
    }

    private void openFile() {

        try {
            String[] splitFilename = filename.split("\\.");
            // If attached filename has more than one . (dot) in it temporary filename will not have full original filename in it
            // most important that it will have correct extension
            File tmpAttachedFile = File.createTempFile(splitFilename[0], "." + splitFilename[splitFilename.length - 1]); // Temporary file that will shared

            // Writes Base64 encoded string to the temporary file
            FileOutputStream out = new FileOutputStream(tmpAttachedFile);
            out.write(reader.getFileByteArray(this.nodeUniqueID, this.filename, this.time));
            out.close();

            // Getting Uri to share
            Uri tmpFileUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", tmpAttachedFile);

            // Intent to open file
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(tmpFileUri, this.fileMimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    ActivityResultLauncher<Intent> saveFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        // Saves attached file to the user selected file
        if (result.getResultCode() == Activity.RESULT_OK) {

            try {
                OutputStream outputStream = getContext().getContentResolver().openOutputStream(result.getData().getData(), "w"); // Output file
                outputStream.write(reader.getFileByteArray(this.nodeUniqueID, this.filename, this.time));
                outputStream.close();
            } catch (Exception e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_save_file, Toast.LENGTH_SHORT).show();
            }
        }
        dismiss(); // Closes dialog fragment after writing to file (hopefully)
    });
}

