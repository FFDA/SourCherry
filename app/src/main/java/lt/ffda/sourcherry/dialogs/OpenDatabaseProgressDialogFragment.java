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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import lt.ffda.sourcherry.AppContainer;
import lt.ffda.sourcherry.MainActivity;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ScApplication;
import lt.ffda.sourcherry.utils.Files;

public class OpenDatabaseProgressDialogFragment extends DialogFragment {
    private ScheduledThreadPoolExecutor executor;
    private long fileSize; // File size of the file (not the archive itself) that is being extracted
    private Handler handler;
    private TextView message;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private long totalLen; // Used to calculate percentage for progressBar

    private void copyDatabaseToAppSpecificStorage() {
        // Set directory where database will be saved depending on user's choice in options menu
        File databaseDir;
        if (sharedPreferences.getBoolean("preferences_external_storage", false)) {
            databaseDir = new File(getContext().getExternalFilesDir(null), "databases");
        } else {
            databaseDir = new File(getContext().getFilesDir(), "databases");
        }

        String databaseOutputFile = databaseDir.getPath() + "/" + sharedPreferences.getString("databaseFilename", null);
        Uri databaseUri = Uri.parse(sharedPreferences.getString("databaseUri", null));
        totalLen = 0;

        try (
                InputStream databaseInputStream = getContext().getContentResolver().openInputStream(databaseUri);
                OutputStream databaseOutputStream = new FileOutputStream(databaseOutputFile, false);
                ){
            fileSize = databaseInputStream.available();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                }
            });

            // Copying files
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = databaseInputStream.read(buf)) > 0) {
                databaseOutputStream.write(buf, 0, len);
                updateProgressBar(len);
            }
        } catch (FileNotFoundException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_could_not_open_a_file_to_copy_the_database, Toast.LENGTH_LONG).show();
                }
            });
            getDialog().dismiss();
        } catch (IOException e) {
            // This exception is for close() method
            // If app continues to work after this exception it can be ignored
            // It could be that input stream was already closed by fragment before this function was called
            getDialog().dismiss();
        }

        //// Creating new settings
        SharedPreferences.Editor sharedPrefEditor = sharedPreferences.edit();
        sharedPrefEditor.putString("databaseStorageType", "internal");
        // This is not a real Uri, so don't try to use it, but I use it to check if database should be opened automatically
        sharedPrefEditor.putString("databaseUri", databaseOutputFile);
        sharedPrefEditor.apply();
        ////
    }

    private void extractDatabase() {
        String databaseString = sharedPreferences.getString("databaseUri", null);

        String password = getArguments().getString("password");

        // Set directory where database will be saved depending on user's choice in options menu
        File databaseDir;
        if (sharedPreferences.getBoolean("preferences_external_storage", false)) {
            databaseDir = new File(getContext().getExternalFilesDir(null), "databases");
        } else {
            databaseDir = new File(getContext().getFilesDir(), "databases");
        }

        String tmpDatabaseFilename;
        totalLen = 0;

        try {
            //// Copying file to temporary internal apps storage (cache)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    message.setText(R.string.open_database_fragment_copying_database_message);
                }
            });

            File tmpCompressedDatabase = File.createTempFile("tmpDatabaseFile", null);
            OutputStream os = new FileOutputStream(tmpCompressedDatabase, false);
            InputStream is = getContext().getContentResolver().openInputStream(Uri.parse(databaseString));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                }
            });
            // Copying files
            fileSize = is.available();
            byte[] buf = new byte[4 * 1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                updateProgressBar(len);
            }
            ////
            is.close();
            os.close();

            //// Extracting file to permanent internal apps storage
            handler.post(new Runnable() {
                @Override
                public void run() {
                    message.setText(R.string.open_database_fragment_extracting_database_message);
                    progressBar.setProgress(0);
                }
            });

            // Opening archive
            RandomAccessFile randomAccessFile = new RandomAccessFile(tmpCompressedDatabase, "r");
            RandomAccessFileInStream inStream = new RandomAccessFileInStream(randomAccessFile);
            IInArchive inArchive = SevenZip.openInArchive(ArchiveFormat.SEVEN_ZIP, inStream);
            // Creating/opening output file
            tmpDatabaseFilename = inArchive.getStringProperty(0, PropID.PATH);
            // At some point filenames inside CherryTree password protected archives were changed to include a random(?) integer
            // in the middle of the filename. To make it look normal again I had to remove it
            tmpDatabaseFilename = Files.getFileName(tmpDatabaseFilename) + "." + Files.getFileExtension(tmpDatabaseFilename); // Joining first and last part of the filename array
            totalLen = 0; // Resetting totalLen value
            fileSize = Long.parseLong(inArchive.getStringProperty(0, PropID.SIZE));
            // Writing data
            SequentialOutStream sequentialOutStream = new SequentialOutStream();
            sequentialOutStream.openOutputStream(new File(databaseDir, tmpDatabaseFilename));
            inArchive.extractSlow(0, sequentialOutStream, password); // Extracting file
            // Cleaning up
            sequentialOutStream.closeOutputStream();
            inArchive.close();
            inStream.close();

            //// Creating new settings
            // Saved Uri is not a real Uri, so don't try to use it.
            // The only reason to save it here is, that I'm using it to check if database should be opened automatically
            saveDatabaseToPrefs("internal", tmpDatabaseFilename, Files.getFileExtension(tmpDatabaseFilename), databaseDir.getPath() + "/" + tmpDatabaseFilename);
            ////
        } catch (FileNotFoundException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_database_does_not_exists, Toast.LENGTH_SHORT).show();
                }
            });
            dismiss();
        } catch (IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_extract_database, Toast.LENGTH_SHORT).show();
                }
            });
            dismiss();
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        ((MainActivity) getActivity()).startMainViewActivity();
        dismissNow();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_open_database_progress, null);
        builder.setView(view);

        setCancelable(false); // Not allowing user to cancel the the dialog fragment

        // Setting up variables
        progressBar = view.findViewById(R.id.progress_fragment_progressBar);
        message = view.findViewById(R.id.progress_fragment_message);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        executor = appContainer.executor;
        handler = appContainer.handler;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        dismissNow();
    }

    @Override
    public void onStart() {
        super.onStart();
        String databaseFileExtension = sharedPreferences.getString("databaseFileExtension", null);

        if (databaseFileExtension.equals("ctb")) {
            message.setText(R.string.open_database_fragment_copying_database_message);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    copyDatabaseToAppSpecificStorage();
                    getDialog().cancel();
                }
            });
        }
        if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    extractDatabase();
                    getDialog().cancel();
                }
            });
        }
    }

    /**
     * aves passed information about database to preferences
     * @param databaseStorageType values can be "shared" or "internal"
     * @param databaseFilename database filename with extension
     * @param databaseFileExtension database extension
     * @param databaseUri uri for shared databases and path for internal databases
     */
    private void saveDatabaseToPrefs(String databaseStorageType, String databaseFilename, String databaseFileExtension, String databaseUri) {
        // Saves passed information about database to preferences
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString("databaseStorageType", databaseStorageType);
        sharedPreferencesEditor.putString("databaseFilename", databaseFilename);
        sharedPreferencesEditor.putString("databaseFileExtension", databaseFileExtension);
        sharedPreferencesEditor.putString("databaseUri", databaseUri);
        sharedPreferencesEditor.apply();
    }

    /**
     * Calculates new value for the progress bar and updates progress bar to show it
     * @param len amount of data that was consumed
     */
    private void updateProgressBar(int len) {
        totalLen += len;
        int percent = (int) (totalLen * 100 / fileSize);
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(percent);
            }
        });
    }

    /**
     * Class to write archive to a file
     */
    private class SequentialOutStream implements ISequentialOutStream {
        private FileOutputStream fileOutputStream;

        /**
         * Closes output stream
         */
        public void closeOutputStream() {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_close_extraction_output_stream, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Opens output file stream to write archived file into
         * @param file file to open as output stream
         */
        public void openOutputStream(File file) {
            try {
                fileOutputStream = new FileOutputStream(file, false);
            } catch (FileNotFoundException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_open_extraction_output_stream, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Writes data stream to a file
         * @param data data to be written
         * @return amount of data consumed (written)
         * @throws SevenZipException exception while extracting
         */
        @Override
        public int write(byte[] data) throws SevenZipException {
            try {
                fileOutputStream.write(data);
                updateProgressBar(data.length);
            } catch (IOException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_extract_database, Toast.LENGTH_SHORT).show();
            }
            return data.length;
        }
    }
}
