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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class SaveOpenDialogFragment extends DialogFragment {
    private DatabaseReader reader;
    private String nodeUniqueID;
    private String filename;
    private String time;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        // For consistency and laziness buttons were mixed.
        // NeutralButton will cancel dialog and NegativeButton will be for saving the file
        super.onCreateDialog(savedInstanceState);

        this.reader = ((MainView) getActivity()).reader(); // reader from main MainView

        this.filename = getArguments().getString("filename", null); // Filename passed to fragment
        this.time = getArguments().getString("time");
        this.nodeUniqueID = ((MainView) getActivity()).getCurrentNodeUniqueID(); // Current node's unique_id retrieved from MainView

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_open_fragment, null);

        builder.setView(view)
            .setTitle(R.string.save_open_dialog_fragment_title)
            .setPositiveButton(R.string.button_open, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
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
        LinearLayout dialogLayout = view.findViewById(R.id.dialog_save_open_fragment_layout);
        TextView textViewFilename = new TextView(getContext());
        textViewFilename.setText(filename);
        dialogLayout.addView(textViewFilename);

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
        // This is needed to make keep activity alive while file is being written
        Button saveButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFile.launch(SaveOpenDialogFragment.this.filename);
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
                Files.write(tmpAttachedFile.toPath(), reader.getFileByteArray(this.nodeUniqueID, this.filename, this.time));
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
            e.printStackTrace();
        }
    }


    ActivityResultLauncher<String> saveFile = registerForActivityResult(new ActivityResultContracts.CreateDocument(), result -> {
        // Saves touched file to the user selected file
        try {
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(result, "w"); // Output file
            // Writes byte array converted from encoded string
            outputStream.write(reader.getFileByteArray(this.nodeUniqueID, this.filename, this.time));
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        dismiss(); // Closes dialog fragment after writing to file (hopefully)
    });
}

