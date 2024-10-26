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

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IOutCreateArchive7z;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.ISequentialInStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.OutItemFactory;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import lt.ffda.sourcherry.AppContainer;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.ScApplication;

public class ExportDatabaseDialogFragment extends DialogFragment {
    private Button buttonCancel;
    private Button buttonExport;
    private ScheduledThreadPoolExecutor executor;
    private long fileSize;
    private Handler handler;
    private TextView message;
    private TextInputEditText password1;
    private TextInputEditText password2;
    private CheckBox passwordCheckBox;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    /**
     * Compresses currently opened database file to an encrypted archive
     * @return File object with compressed database file
     */
    private File compressDatabase() {
        File tmpCompressedDatabase = null;
        try {
            String databaseFilename = sharedPreferences.getString("databaseFilename", null);
            tmpCompressedDatabase = File.createTempFile(databaseFilename, null);
            RandomAccessFile randomAccessFile = new RandomAccessFile(tmpCompressedDatabase, "rw");
            OutCreateCallback outCreateCallback = new OutCreateCallback(databaseFilename);
            IOutCreateArchive7z outArchive = SevenZip.openOutArchive7z();
            outArchive.setLevel(1);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    message.setText(R.string.dialog_fragment_export_database_message_compressing_database);
                }
            });
            outArchive.createArchive(new RandomAccessFileOutStream(randomAccessFile), 1, outCreateCallback);
            // Cleaning up
            outArchive.close();
            randomAccessFile.close();
            outCreateCallback.closeRandomAccessFileInStream();
        } catch (IOException e) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
            dismiss();
        }
        return tmpCompressedDatabase;
    }

    /**
     * Enable or disable UI elements depending of passed boolean variable.
     * Makes dialog not cancelable if UI elements are disabled.
     * UI elements that are disabled by this function:
     * "Protect with password" checkbox, both password fields, Cancel and Export buttons.
     * @param enable true - to enable, false - to disable
     */
    private void enableUI(boolean enable) {
        getDialog().setCancelable(enable);
        passwordCheckBox.setEnabled(enable);
        password1.setEnabled(enable);
        password2.setEnabled(enable);
        buttonCancel.setEnabled(enable);
        buttonExport.setEnabled(enable);
    }

    /**
     * Exports database to external storage. Depending on user choices compresses it to password
     * protected archive. Updates MirrorDatabase preferences if needed. If there is a file with the
     * the same filename in the exporting folder and MirrorDatabase is enabled - it renames the old
     * file (adds timestamp before the file extension), if MirrorDatabase is disabled - renames the
     * new file (adds timestamp before the file extension).
     */
    private void exportDatabase() {
        Uri outputFileUri = Uri.parse(getArguments().getString("exportFileUri"));
        DocumentFile outputDocumentFile = DocumentFile.fromSingleUri(getContext(), outputFileUri);
        String fullFilename = sharedPreferences.getString("databaseFilename", null);
        ContentResolver contentResolver = getContext().getContentResolver();
        InputStream inputStream;
        OutputStream outputStream;
        // Setting up input steam
        if (passwordCheckBox.isChecked()) {
            // If database is password protected - compress the file
            File compressedDatabase = compressDatabase();
            try {
                inputStream = new FileInputStream(compressedDatabase);
            } catch (FileNotFoundException e) {
                dismiss();
                Toast.makeText(getContext(), R.string.toast_error_failed_to_export_database, Toast.LENGTH_SHORT).show();
                return;
            }
            // Compressed (password protected) databases have a
            // different filename extension based on their type
            String databaseExtensionCompressed = null;
            if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                // XML
                databaseExtensionCompressed = "ctz";
            } else {
                // SQL
                databaseExtensionCompressed = "ctx";
            }
            fullFilename = String.format("%1$s.%2$s", fullFilename.substring(0, fullFilename.lastIndexOf(".")), databaseExtensionCompressed);
        } else {
            try {
                inputStream = new FileInputStream(sharedPreferences.getString("databaseUri", null));
            } catch (FileNotFoundException e) {
                dismiss();
                Toast.makeText(getContext(), R.string.toast_error_failed_to_export_database, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // Setting up output stream. Uri is passed to the DialogFragment as an argument from
        // MainView depending on if user chose a location or MirrorDatabase was turned on
        try {
            outputStream = getContext().getContentResolver().openOutputStream(outputFileUri, "wt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        // Exporting database
        exportFile(inputStream, outputStream);
        // Rename file
        if (sharedPreferences.getBoolean("mirror_database_switch", false)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String mirrorDatabaseFilename = sharedPreferences.getString("mirrorDatabaseFilename", null);
            if (!mirrorDatabaseFilename.equals(fullFilename)) {
                // If filename of the new file does not match the one saved in the MirrorDatabase
                // settings - file will be renamed to match the new database filename and
                // settings will be updated to match it
                try {
                    // First try to rename a database file
                    Uri uri = DocumentsContract.renameDocument(contentResolver, outputFileUri, fullFilename);
                    if (uri == null) {
                        // Failed to rename the file. Most likely because there is file with the same
                        // filename in the directory. Reading through files inside Mirror Database
                        // folder and searching for the file
                        Uri mirrorDatabaseFolderUri = Uri.parse(sharedPreferences.getString("mirrorDatabaseFolderUri", null));
                        Uri mirrorDatabaseFolderChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDatabaseFolderUri, DocumentsContract.getTreeDocumentId(mirrorDatabaseFolderUri));
                        Cursor cursor = contentResolver.query(mirrorDatabaseFolderChildrenUri, new String[]{"document_id", "_display_name", "last_modified"}, null, null, null);
                        while (cursor != null && cursor.moveToNext()) {
                            if (cursor.getString(1).equals(fullFilename)) {
                                // if file with the Mirror Database File filename was wound inside Mirror Database Folder
                                // Renaming it with appended timestamp before file extension
                                Uri offendingFileUri = DocumentsContract.buildDocumentUriUsingTree(mirrorDatabaseFolderUri, cursor.getString(0));
                                String filename = fullFilename.substring(0, fullFilename.lastIndexOf("."));
                                String extension = fullFilename.substring(fullFilename.lastIndexOf(".") + 1);
                                String newFullFilename = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s_%2$d.%3$s", filename, System.currentTimeMillis(), extension);
                                DocumentsContract.renameDocument(contentResolver, offendingFileUri, newFullFilename);
                                break;
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                        // Trying to rename database with the filename that other
                        DocumentsContract.renameDocument(contentResolver, outputFileUri, fullFilename);
                    }
                } catch (Exception ex) {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                    dismiss();
                    return;
                }
                editor.putString("mirrorDatabaseFilename", fullFilename);
            }
            editor.putLong("mirrorDatabaseLastModified", System.currentTimeMillis());
            editor.apply();
        } else {
            if (!outputDocumentFile.getName().equals(fullFilename)) {
                try {
                    // Trying to rename to the file user specified
                    Uri uri = DocumentsContract.renameDocument(contentResolver, outputFileUri, fullFilename);
                    if (uri == null) {
                        try {
                            // If rename operation failed because there was already a file with that name
                            // Trying to rename file with a timestamp
                            String filename = fullFilename.substring(0, fullFilename.lastIndexOf("."));
                            String extension = fullFilename.substring(fullFilename.lastIndexOf(".") + 1);
                            fullFilename = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s_%2$d.%3$s", filename, System.currentTimeMillis(), extension);
                            DocumentsContract.renameDocument(contentResolver, outputFileUri, fullFilename);
                        } catch (FileNotFoundException ex) {
                            Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    try {
                        // If rename operation failed because there was already a file with that name
                        // Trying to rename file with a timestamp
                        String filename = fullFilename.substring(0, fullFilename.lastIndexOf("."));
                        String extension = fullFilename.substring(fullFilename.lastIndexOf(".") + 1);
                        fullFilename = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s_%2$d.%3$s", filename, System.currentTimeMillis(), extension);
                        DocumentsContract.renameDocument(contentResolver, outputFileUri, fullFilename);
                    } catch (FileNotFoundException ex) {
                        Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        dismiss();
    }

    /**
     * Copies file from app-specific storage to external storage
     * @param inputStream stream of the file to be exported
     * @param outputStream stream of the file to which database has to be exported to
     */
    private void exportFile(InputStream inputStream, OutputStream outputStream){
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setIndeterminate(false);
                    message.setText(R.string.dialog_fragment_export_database_message_exporting_database);
                }
            });

            // Copying files
            fileSize = inputStream.available();
            long totalLen = 0;
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(percent);
                    }
                });
            }
            // Cleaning up and closing
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_export_database, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.dialog_fragment_export_database, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AppContainer appContainer = ((ScApplication) getActivity().getApplication()).appContainer;
        executor = appContainer.executor;
        handler = appContainer.handler;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        progressBar = view.findViewById(R.id.dialog_fragment_export_database_progress_bar);
        passwordCheckBox = view.findViewById(R.id.dialog_fragment_export_database_checkbox_password_protect);
        password1 = view.findViewById(R.id.dialog_fragment_export_database_password1);
        password2 = view.findViewById(R.id.dialog_fragment_export_database_password2);
        message = view.findViewById(R.id.dialog_fragment_export_database_message);

        passwordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextInputLayout editTextPassword1 = view.findViewById(R.id.dialog_fragment_export_database_password1_layout);
                TextInputLayout editTextPassword2 = view.findViewById(R.id.dialog_fragment_export_database_password2_layout);
                if (isChecked) {
                    editTextPassword1.setVisibility(View.VISIBLE);
                    editTextPassword2.setVisibility(View.VISIBLE);
                } else {
                    editTextPassword1.setVisibility(View.GONE);
                    editTextPassword2.setVisibility(View.GONE);
                }
            }
        });

        String mirrorDatabaseFilename = sharedPreferences.getString("mirrorDatabaseFilename", null);
        if (mirrorDatabaseFilename != null) {
            String mirrorDatabaseFilenameExtension = mirrorDatabaseFilename.substring(mirrorDatabaseFilename.lastIndexOf('.') + 1);
            if (mirrorDatabaseFilenameExtension.equals("ctx") || mirrorDatabaseFilenameExtension.equals("ctz")) {
                passwordCheckBox.setChecked(true);
            }
        }

        buttonCancel = view.findViewById(R.id.dialog_fragment_export_database_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!sharedPreferences.getBoolean("mirror_database_switch", false)) {
                    try {
                        // Deletes temporary file created by file picker if user decides to cancel export operation
                        DocumentsContract.deleteDocument(getContext().getContentResolver(), Uri.parse(getArguments().getString("exportFileUri")));
                    } catch (FileNotFoundException e) {
                        // If it fails to delete the file user will not be notified
                    }
                }
                dismiss();
            }
        });

        buttonExport = view.findViewById(R.id.dialog_fragment_export_database_button_export);
        buttonExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // At this point user can't dismiss dialog fragment
                // It will be done after export action is done
                enableUI(false);
                getDialog().setCancelable(false);
                if (passwordCheckBox.isChecked()) {
                    if (!password1.getText().toString().equals(password2.getText().toString())) {
                        Toast.makeText(getContext(), R.string.toast_error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
                        enableUI(true);
                        return;
                    }
                }
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        exportDatabase();
                    }
                });
            }
        });
    }

    /**
     * Class to provide necessary information about new archive items and to receive information about the progress of the operation.
     */
    private final class OutCreateCallback implements IOutCreateCallback<IOutItem7z>, ICryptoGetTextPassword {
        private final File currentDatabase;
        private final String filenameInsideArchive;
        private RandomAccessFileInStream randomAccessFileInStream;

        /**
         * Constructor that creates current opened database's File object
         */
        public OutCreateCallback(String filenameInsideArchive) {
            currentDatabase = new File(sharedPreferences.getString("databaseUri", null));
            this.filenameInsideArchive = filenameInsideArchive;
        }

        public void closeRandomAccessFileInStream() {
            try {
                randomAccessFileInStream.close();
            } catch (IOException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_close_compression_input_stream, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public String cryptoGetTextPassword() throws SevenZipException {
            return password1.getText().toString();
        }

        @Override
        public IOutItem7z getItemInformation(int index, OutItemFactory<IOutItem7z> outItemFactory) throws SevenZipException {
            IOutItem7z item = outItemFactory.createOutItem();
            item.setDataSize(currentDatabase.length());
            item.setPropertyIsDir(false);
            item.setPropertyIsAnti(false);
            item.setDataSize(currentDatabase.length());
            item.setPropertyPath(filenameInsideArchive);
            return item;
        }

        @Override
        public ISequentialInStream getStream(int index) throws SevenZipException {
            try {
                randomAccessFileInStream = new RandomAccessFileInStream(new RandomAccessFile(currentDatabase, "r"));
            } catch (FileNotFoundException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
            }
            return randomAccessFileInStream;
        }

        /**
         * Calculates how many percentages where process and set's progress bar to that value
         * @param complete amount of bytes that were processed
         * @throws SevenZipException Error during compression
         */
        @Override
        public void setCompleted(long complete) throws SevenZipException {
            int percent = (int) (complete * 100 / fileSize);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(percent);
                }
            });
        }

        @Override
        public void setOperationResult(boolean operationResultOk) throws SevenZipException {

        }

        @Override
        public void setTotal(long total) throws SevenZipException {
            fileSize = total;
        }
    }
}
