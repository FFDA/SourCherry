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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;

public class SaveOpenDialogFragment extends DialogFragment {
    private XMLReader xmlReader;
    private ArrayList<String> filesInNode; // All filenames matching filename that was touched by the user
    private String nodeUniqueID;
    private String filename;
    private int index = -1; // Index of selected radio button. -1 - no radio button selected

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        // For consistency and laziness buttons were mixed.
        // NeutralButton will cancel dialog and NegativeButton will be for saving the file
        super.onCreateDialog(savedInstanceState);

        xmlReader  = ((MainView) getActivity()).getXmlReader(); // xmlReader from main MainView

        this.filename = getArguments().getString("filename", null); // Filename passed to fragment
        this.nodeUniqueID = ((MainView) getActivity()).getCurrentNodeUniqueID(); // Current node's unique_id retrieved from MainView
        this.filesInNode = this.xmlReader.getAttachedFileList(filename, nodeUniqueID);

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_open_fragment, null);

        builder.setView(view)
            .setTitle(R.string.save_open_dialog_fragment_title)
            .setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (filesInNode.size() > 1) { // Multiple files. User has to choose
                        if (SaveOpenDialogFragment.this.index == -1) { // User did not select any file
                            Toast.makeText(getContext(), R.string.toast_seve_open_dialog_fragment_no_file_selected, Toast.LENGTH_SHORT).show();
                        } else {
                            openFile();
                        }
                    } else { // Just one file
                        SaveOpenDialogFragment.this.index = 0;
                        openFile();
                    }
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
        //

        /// Part of layout that changes depending on how many files with same filename there are in node
        LinearLayout dialogLayout = view.findViewById(R.id.dialog_save_open_fragment_layout);

        if (filesInNode.size() == 1) {
            TextView textViewFilename = new TextView(getContext());
            textViewFilename.setText(filesInNode.get(0));
            dialogLayout.addView(textViewFilename);
        } else if (filesInNode.size() > 1) {
            // If there are more than one filename radio buttons are displayed for user to select a file
            int fileRadioGroupID = 1000; // Id for the RadioGroup
            int counter = 1; // Counter to add radio buttons to the group with increasing id number

            // Message for the user about the issue
            TextView multipleFilesMessageTextView = new TextView(getContext());
            multipleFilesMessageTextView.setText(R.string.save_open_dialog_fragment_multiple_file_message);
            dialogLayout.addView(multipleFilesMessageTextView);
            //

            // Adding radio group and buttons
            RadioGroup fileRadioGroup = new RadioGroup(getContext());
            fileRadioGroup.setId(fileRadioGroupID); // Id for the group. Always 1000
            fileRadioGroup.setPadding(0,20,0,0); // Some formatting for the group
            for (String f: filesInNode) { // For every filename (that is actually the same)
                RadioButton currentFilename = new RadioButton(getContext()); // Creating radio button
                currentFilename.setText(f); // Setting the filename
                currentFilename.setId(fileRadioGroupID + counter); // Adding the id 1000 + counter (starts from 1)
                fileRadioGroup.addView(currentFilename);
                counter++;
            }
            fileRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    SaveOpenDialogFragment.this.index = checkedId - 1001;
                }
            });
            dialogLayout.addView(fileRadioGroup);
        } else {
            // Error message if there is 0 filenames
            Toast.makeText(getContext(), R.string.toast_save_open_dialog_fragment_no_filename_error, Toast.LENGTH_SHORT).show();
            dismiss();
        }
        ///
        ////

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
        // This needed to make keep activity alive while file is being written
        Button saveButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filesInNode.size() > 1) { // Multiple files. User has to choose
                    if (SaveOpenDialogFragment.this.index == -1) { // User did not select any file
                        Toast.makeText(getContext(), R.string.toast_seve_open_dialog_fragment_no_file_selected, Toast.LENGTH_SHORT).show();
                    } else {
                        saveFile.launch(SaveOpenDialogFragment.this.filename);
                    }
                } else { // Just one file
                    SaveOpenDialogFragment.this.index = 0;
                    saveFile.launch(SaveOpenDialogFragment.this.filename);
                }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.write(tmpAttachedFile.toPath(), Base64.decode(xmlReader.getFileBase64String(this.nodeUniqueID, this.filename, this.index), Base64.DEFAULT));
            } else {
                // Android 8 is SDK 26
                Toast.makeText(getContext(), R.string.toast_error_minimum_android_version_8, Toast.LENGTH_SHORT).show();
            }

            // Getting Uri to share
            Uri tmpFileUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", tmpAttachedFile);

            // Getting mime type of the file. Without it files won't be opened
            FileNameMap fileNameMap  = URLConnection.getFileNameMap();
            String tmpFileMime = fileNameMap.getContentTypeFor(filename);

            // Intent to open file
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(tmpFileUri, tmpFileMime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    ActivityResultLauncher<String> saveFile = registerForActivityResult(new ActivityResultContracts.CreateDocument(), result -> {
        // Saves touched file to the user selected file
        try {
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(result, "w"); // Output file
            // Writes byte array converted from encoded string
            outputStream.write(Base64.decode(xmlReader.getFileBase64String(this.nodeUniqueID, this.filename, this.index), Base64.DEFAULT));
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dismiss(); // Closes dialog fragment after writing to file (hopefully)
    });
}

