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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenDatabaseProgressDialogFragment extends DialogFragment {
    private SharedPreferences sharedPref;
    private ProgressBar progressBar;
    private TextView message;
    private ExecutorService executor;
    private Handler handler;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_open_database_progress, null);
        builder.setView(view);

        setCancelable(false); // Not allowing user to cancel the the dialog fragment

        // Setting up variables
        this.progressBar = view.findViewById(R.id.progress_fragment_progressBar);
        this.message = view.findViewById(R.id.progress_fragment_message);
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.sharedPref = getContext().getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        String databaseFileExtension = this.sharedPref.getString("databaseFileExtension", null);

        if (databaseFileExtension.equals("ctb")) {
            this.message.setText(R.string.open_database_fragment_copying_database_message);
            executor.execute(() -> {
                this.copyDatabaseToAppSpecificStorage();
                getDialog().cancel();
            });
        }
        if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
            executor.execute(() -> {
                this.extractDatabase();
                getDialog().cancel();
            });
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        dismissNow();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        ((MainActivity) getActivity()).startMainViewActivity();
        dismissNow();
    }

    @Override
    public void onDestroyView() {
        executor.shutdownNow();
        super.onDestroyView();
    }

    private void copyDatabaseToAppSpecificStorage() {
        File databaseDir = new File(getContext().getFilesDir(), "databases");
        if (!databaseDir.exists()) {
            // If directory does not exists - create it
            databaseDir.mkdirs();
        }

        String databaseOutputFile = databaseDir.getPath() + "/" + this.sharedPref.getString("databaseFilename", null);
        Uri databaseUri = Uri.parse(this.sharedPref.getString("databaseUri", null));
        long totalLen = 0;

        try {
            InputStream databaseInputStream = getContext().getContentResolver().openInputStream(databaseUri);
            OutputStream databaseOutputStream = new FileOutputStream(databaseOutputFile, false);
            long fileSize = databaseInputStream.available();

            // Copying files
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = databaseInputStream.read(buf)) > 0) {
                databaseOutputStream.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                handler.post(() -> this.progressBar.setProgress(percent));
            }

            databaseInputStream.close();
            databaseOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            handler.post(() -> Toast.makeText(getContext(), R.string.toast_error_could_not_open_a_file_to_copy_the_database, Toast.LENGTH_LONG).show());
            getDialog().dismiss();
        } catch (IOException e) {
            e.printStackTrace();
            handler.post(() -> Toast.makeText(getContext(), R.string.toast_error_could_not_copy_the_database, Toast.LENGTH_LONG).show());
            getDialog().dismiss();
        }

        //// Creating new settings
        SharedPreferences.Editor sharedPrefEditor = this.sharedPref.edit();
        sharedPrefEditor.putString("databaseStorageType", "internal");
        // This is not a real Uri, so don't try to use it, but I use it to check if database should be opened automatically
        sharedPrefEditor.putString("databaseUri", databaseOutputFile);
        sharedPrefEditor.apply();
        ////
    }

    private void extractDatabase() {
        String databaseString = sharedPref.getString("databaseUri", null);

        String password = getArguments().getString("password");

        File databaseDir = new File(getContext().getFilesDir(), "databases");
        if (!databaseDir.exists()) {
            // If directory does not exists - create it
            databaseDir.mkdirs();
        }

        String tmpDatabaseFilename = "";
        long totalLen = 0; // Used to calculate percentage

        try {
            //// Copying file to temporary internal apps storage (cache)
            handler.post(() -> this.message.setText(R.string.open_database_fragment_copying_database_message));

            File tmpCompressedDatabase = File.createTempFile("tmpDatabaseFile", null);
            OutputStream os = new FileOutputStream(tmpCompressedDatabase, false);
            InputStream is = getContext().getContentResolver().openInputStream(Uri.parse(databaseString));
            // Copying files
            long fileSize = is.available();
            byte[] buf = new byte[4 * 1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                handler.post(() -> this.progressBar.setProgress(percent));
            }
            ////
            is.close();
            os.close();

            //// Extracting file to permanent internal apps storage
            handler.post(() -> {
                this.message.setText(R.string.open_database_fragment_extracting_database_message);
                this.progressBar.setProgress(0);
            });

            SevenZFile sevenZFile = new SevenZFile(tmpCompressedDatabase, password.toCharArray());
            // Entry is a file inside the archive
            // In CherryTree database file there always should be one
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();

            while (entry != null) {
                tmpDatabaseFilename = entry.getName(); // Getting the (current) name of the file inside the archive
                totalLen = 0; // Resetting totalLen value
                FileOutputStream out = new FileOutputStream(new File(databaseDir, tmpDatabaseFilename));
                InputStream in = sevenZFile.getInputStream(entry);
                fileSize = entry.getSize();
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    totalLen += len;
                    int percent = (int) (totalLen * 100 / fileSize);
                    handler.post(() -> this.progressBar.setProgress(percent));
                }
                out.close();
                in.close();
                entry = sevenZFile.getNextEntry();
            }
            ////
            //// Creating new settings
            // Saved Uri is not a real Uri, so don't try to use it.
            // The only reason to save it here is, that I'm using it to check if database should be opened automatically
            String[] splitExtension = tmpDatabaseFilename.split("\\."); // Splitting the path to extract the file extension.
            this.saveDatabaseToPrefs("internal", tmpDatabaseFilename, splitExtension[splitExtension.length - 1], databaseDir.getPath() + "/" + tmpDatabaseFilename);
            ////

            //// Cleaning up
//                tmpCompressedDatabase.delete(); // Using deleteTempFile onDestroy in MainActivity
            sevenZFile.close();
            ////
        } catch (FileNotFoundException e) {
            handler.post(() -> Toast.makeText(getContext(), "Error FileNotFoundException (1a): " + e.getMessage(), Toast.LENGTH_LONG).show());
            e.printStackTrace();
            dismiss();
        } catch (IOException e) {
            handler.post(() -> Toast.makeText(getContext(), "Error IOException (1b): " + e.getMessage(), Toast.LENGTH_LONG).show());
            e.printStackTrace();
            dismiss();
        }
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
}
