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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;

public class SaveOpenDialogFragment extends DialogFragment {
    private XMLReader xmlReader;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        // For consistency and laziness buttons were mixed.
        // NeutralButton will cancel dialog and NegativeButton will be for saving the file
        super.onCreateDialog(savedInstanceState);

        xmlReader  = ((MainView) getActivity()).getXmlReader(); // xmlReader from main MainView

        String filename = getArguments().getString("filename", null); // Filename passed to fragment
        String nodeUniqueID = ((MainView) getActivity()).getCurrentNodeUniqueID(); // Current node retrieved from MainView
        ArrayList<String> filesInNode = this.xmlReader.getAttachedFileList(filename, nodeUniqueID); // All filenames matching filename that was touched by the user

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_open_fragment, null);

        builder.setView(view)
            .setTitle(R.string.save_open_dialog_fragment_title)
            .setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
//                openFile(nodeUniqueID, filename, 0);
                if (filesInNode.size() > 1) {
                    RadioGroup radioGroup = view.findViewById(1000);
                    int selectedRadioButtonID = radioGroup.getCheckedRadioButtonId(); // Checked radio button id (starts with 1001)
                    if (selectedRadioButtonID != -1) { // Checks if user chose a file
                        openFile(nodeUniqueID, filename, radioGroup.getCheckedRadioButtonId() - 1001);
                    } else {
                        // Dialog will close, but at least will show a message
                        Toast.makeText(getContext(), R.string.toast_save_open_dialog_fragment_no_filename_error, Toast.LENGTH_SHORT).show();
                    }

                } else {
                    openFile(nodeUniqueID, filename, 0);
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

    private void openFile(String nodeUniqueID, String filename, int index) {
        try {
            String[] splitFilename = filename.split("\\.");
            // If attached filename has more than one . (dot) in it temporary filename will not have full original filename in it
            // most important that it will have correct extension
            File tmpAttachedFile = File.createTempFile(splitFilename[0], "." + splitFilename[splitFilename.length - 1]); // Temporary file that will shared

            // Writes Base64 encoded string to the temporary file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.write(tmpAttachedFile.toPath(), Base64.decode(xmlReader.getFileBase64String(nodeUniqueID, filename, index), Base64.DEFAULT));
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
}

