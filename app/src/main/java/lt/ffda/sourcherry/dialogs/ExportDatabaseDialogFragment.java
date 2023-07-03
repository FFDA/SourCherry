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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;

public class ExportDatabaseDialogFragment extends DialogFragment {
    private ExecutorService executor;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private ProgressBar progressBar;
    private TextView message;
    private CheckBox passwordCheckBox;
    private TextInputEditText password1;
    private TextInputEditText password2;
    private Button buttonCancel;
    private Button buttonExport;
    private long fileSize;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.dialog_fragment_export_database, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            this.executor = ((MainView) getActivity()).getExecutor();
            this.handler = ((MainView) getActivity()).getHandler();
        } catch (RuntimeException e) {
            this.executor = Executors.newSingleThreadExecutor();
            this.handler = new Handler(Looper.getMainLooper());
        }
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.progressBar = view.findViewById(R.id.dialog_fragment_export_database_progress_bar);
        this.passwordCheckBox = view.findViewById(R.id.dialog_fragment_export_database_checkbox_password_protect);
        this.password1 = view.findViewById(R.id.dialog_fragment_export_database_password1);
        this.password2 = view.findViewById(R.id.dialog_fragment_export_database_password2);
        this.message = view.findViewById(R.id.dialog_fragment_export_database_message);

        Uri outputFileUri = Uri.parse(getArguments().getString("exportFileUri"));
        DocumentFile outputDocumentFile = DocumentFile.fromSingleUri(getContext(), outputFileUri);
        ContentResolver contentResolver = getContext().getContentResolver();

        this.passwordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

        String mirrorDatabaseFilename = this.sharedPreferences.getString("mirrorDatabaseFilename", null);
        if (mirrorDatabaseFilename != null) {
            String mirrorDatabaseFilenameExtension = mirrorDatabaseFilename.substring(mirrorDatabaseFilename.lastIndexOf('.') + 1);
            if (mirrorDatabaseFilenameExtension.equals("ctx") || mirrorDatabaseFilenameExtension.equals("ctz")) {
                this.passwordCheckBox.setChecked(true);
            }
        }

        this.buttonCancel = view.findViewById(R.id.dialog_fragment_export_database_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ExportDatabaseDialogFragment.this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
                    try {
                        // Deletes temporary file created by file picker if user decides to cancel export operation
                        DocumentsContract.deleteDocument(getContext().getContentResolver(), Uri.parse(getArguments().getString("exportFileUri")));
                    } catch (FileNotFoundException e) {
                        // If it fails to delete the file user will not be notified
                    }
                }
                ExportDatabaseDialogFragment.this.dismiss();
            }
        });

        this.buttonExport = view.findViewById(R.id.dialog_fragment_export_database_button_export);
        buttonExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // At this point user can't dismiss dialog fragment
                // It will be done after export action is done
                ExportDatabaseDialogFragment.this.enableUI(false);
                getDialog().setCancelable(false);
                if (passwordCheckBox.isChecked()) {
                    if (ExportDatabaseDialogFragment.this.password1.getText().toString().equals(ExportDatabaseDialogFragment.this.password2.getText().toString())) {
                        ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                ExportDatabaseDialogFragment.this.encryptDatabase(outputFileUri, outputDocumentFile, contentResolver);
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), R.string.toast_error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
                        ExportDatabaseDialogFragment.this.enableUI(true);
                    }
                } else {
                    ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // To mark when actually file needs to be renamed. When renaming a
                                // file to the same filename function adds "(n)" to the filename
                                // before file extension
                                boolean rename = false;
                                InputStream inputStream = new FileInputStream(ExportDatabaseDialogFragment.this.sharedPreferences.getString("databaseUri", null));
                                OutputStream outputStream = getContext().getContentResolver().openOutputStream(Uri.parse(getArguments().getString("exportFileUri")), "wt");
                                ExportDatabaseDialogFragment.this.exportDatabase(inputStream, outputStream);
                                String pickedFileName = outputDocumentFile.getName();
                                if (ExportDatabaseDialogFragment.this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
                                    // Getting extension of the file that MirrorDatabase will search
                                    // for in MirrorDatabase folder
                                    String databaseExtension = sharedPreferences.getString("mirrorDatabaseFilename", null).substring(sharedPreferences.getString("mirrorDatabaseFilename", null).lastIndexOf(".") + 1);
                                    if (databaseExtension.equals("ctz")) {
                                        // XML
                                        databaseExtension = "ctd";
                                        rename = true;
                                    } else if (databaseExtension.equals("ctx")) {
                                        // SQL
                                        databaseExtension = "ctb";
                                        rename = true;
                                    }
                                    SharedPreferences.Editor editor = ExportDatabaseDialogFragment.this.sharedPreferences.edit();
                                    if (rename) {
                                        // If file will be renamed filename in the settings have to be saved too
                                        pickedFileName = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s.%2$s", pickedFileName.split("\\.")[0], databaseExtension);
                                        editor.putString("mirrorDatabaseFilename", pickedFileName);
                                    }
                                    editor.putLong("mirrorDatabaseLastModified", System.currentTimeMillis());
                                    editor.apply();
                                } else {
                                    // If there was the same filename as user wrote/accepted in the folder where user wants
                                    // to save the file in android file picker appends "(1)" to the filename. That makes file
                                    // Not recognizable to the SourCherry app.
                                    if (outputDocumentFile.getName().endsWith(")")) {
                                        pickedFileName = outputDocumentFile.getName().substring(0, outputDocumentFile.getName().lastIndexOf(" "));
                                        String pickedFileExtension = pickedFileName.split("\\.")[1];
                                        pickedFileName = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s_%2$d.%3$s", pickedFileName.split("\\.")[0], System.currentTimeMillis(), pickedFileExtension);
                                        rename = true;
                                    }
                                }
                                if (rename) {
                                    try {
                                        DocumentsContract.renameDocument(contentResolver, outputFileUri, pickedFileName);
                                    } catch (Exception ex) {
                                        Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                                    }
                                }
                                ExportDatabaseDialogFragment.this.dismiss();
                            } catch (FileNotFoundException e) {
                                Toast.makeText(getContext(), R.string.toast_error_failed_to_open_input_or_output_stream, Toast.LENGTH_SHORT).show();
                                ExportDatabaseDialogFragment.this.dismiss();
                            }
                        }
                    });
                }
            }
        });
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
        this.passwordCheckBox.setEnabled(enable);
        this.password1.setEnabled(enable);
        this.password2.setEnabled(enable);
        this.buttonCancel.setEnabled(enable);
        this.buttonExport.setEnabled(enable);
    }

    /**
     * Set up for compressing currently opened database to encrypted archive
     * Changes filename inside the archive to the one that user chose when prompted
     * for file location
     * @param outputFileUri URI of the output file
     * @param outputDocumentFile output file DocumentFile object
     * @param contentResolver app content resolver
     */
    private void encryptDatabase(Uri outputFileUri, DocumentFile outputDocumentFile, ContentResolver contentResolver) {
        String databaseExtensionOriginal = this.sharedPreferences.getString("databaseFileExtension", null);
        String databaseExtensionCompressed = null;
        if (databaseExtensionOriginal.equals("ctd")) {
            // XML
            databaseExtensionCompressed = "ctz";
        } else if (databaseExtensionOriginal.equals("ctb")) {
            // SQL
            databaseExtensionCompressed = "ctx";
        }
        String databaseFilename = outputDocumentFile.getName().substring(0, outputDocumentFile.getName().lastIndexOf('.'));
        File tmpCompressedDatabase = this.compressDatabase(String.format("%1$s.%2$s", databaseFilename, databaseExtensionOriginal));
        if (tmpCompressedDatabase != null) {
            try {
                // Opening streams
                InputStream inputStream = new FileInputStream(tmpCompressedDatabase);
                OutputStream outputStream = getContext().getContentResolver().openOutputStream(outputFileUri, "wt");
                ExportDatabaseDialogFragment.this.exportDatabase(inputStream, outputStream);
                String exportedDatabaseFilename = String.format("%1$s.%2$s", databaseFilename, databaseExtensionCompressed);
                if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
                    SharedPreferences.Editor editor = ExportDatabaseDialogFragment.this.sharedPreferences.edit();
                    String mirrorDatabaseFilename = this.sharedPreferences.getString("mirrorDatabaseFilename", null);
                    String mirrorDatabaseFilenameExtension = mirrorDatabaseFilename.substring(mirrorDatabaseFilename.lastIndexOf(".") + 1);
                    if (!mirrorDatabaseFilenameExtension.equals(databaseExtensionCompressed)) {
                        // If extension of the new file does not match the one saved in the MirrorDatabase
                        // settings - file will be renamed to match the extracted database and
                        // settings will be updated to match it
                        try {
                            exportedDatabaseFilename = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s.%2$s", databaseFilename, databaseExtensionCompressed);
                            DocumentsContract.renameDocument(contentResolver, outputFileUri, exportedDatabaseFilename);
                        } catch (FileNotFoundException ex) {
                            Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                        }
                        editor.putString("mirrorDatabaseFilename", exportedDatabaseFilename);
                    }
                    editor.putLong("mirrorDatabaseLastModified", System.currentTimeMillis());
                    editor.apply();
                } else {
                    // Changing filename extension to match database type if database is being exported to user selected file
                    try {
                        // Trying to rename to the file user specified
                        DocumentsContract.renameDocument(contentResolver, outputFileUri, exportedDatabaseFilename);
                    } catch (Exception e) {
                        try {
                            // If rename operation failed because there was already a file with that name
                            // Trying to rename file with a timestamp
                            exportedDatabaseFilename = String.format(getResources().getConfiguration().getLocales().get(0), "%1$s_%2$d.%3$s", databaseFilename, System.currentTimeMillis(), databaseExtensionCompressed);
                            DocumentsContract.renameDocument(contentResolver, outputFileUri, exportedDatabaseFilename);
                        } catch (FileNotFoundException ex) {
                            Toast.makeText(getContext(), R.string.toast_error_failed_to_rename_exported_database, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                this.dismiss();
            } catch (FileNotFoundException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_open_input_or_output_stream, Toast.LENGTH_SHORT).show();
                ExportDatabaseDialogFragment.this.dismiss();
            }
        } else {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
            this.dismiss();
        }
    }

    /**
     * Compresses currently opened database file to an encrypted archive
     * @param filenameInsideArchive filename to use for database inside archive
     * @return File object with compressed database file
     */
    private File compressDatabase(String filenameInsideArchive) {
        File tmpCompressedDatabase = null;
        try {
            String databaseFilename = this.sharedPreferences.getString("databaseFilename", null).split("\\.")[0];
            tmpCompressedDatabase = File.createTempFile(databaseFilename, null);
            RandomAccessFile randomAccessFile = new RandomAccessFile(tmpCompressedDatabase, "rw");
            OutCreateCallback outCreateCallback = new OutCreateCallback(filenameInsideArchive);
            IOutCreateArchive7z outArchive = SevenZip.openOutArchive7z();
            outArchive.setLevel(1);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ExportDatabaseDialogFragment.this.progressBar.setIndeterminate(false);
                    ExportDatabaseDialogFragment.this.message.setText(R.string.dialog_fragment_export_database_message_compressing_database);
                }
            });
            outArchive.createArchive(new RandomAccessFileOutStream(randomAccessFile), 1, outCreateCallback);
            // Cleaning up
            outArchive.close();
            randomAccessFile.close();
            outCreateCallback.closeRandomAccessFileInStream();
        } catch (IOException e) {
            Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
            this.dismiss();
        }
        return tmpCompressedDatabase;
    }

    /**
     * Copies file from app-specific storage to external storage
     * @param inputStream stream of the file to be exported
     * @param outputStream stream of the file to which database has to be exported to
     */
    private void exportDatabase(InputStream inputStream, OutputStream outputStream){
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ExportDatabaseDialogFragment.this.progressBar.setIndeterminate(false);
                    ExportDatabaseDialogFragment.this.message.setText(R.string.dialog_fragment_export_database_message_exporting_database);
                }
            });

            // Copying files
            this.fileSize = inputStream.available();
            long totalLen = 0;
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / this.fileSize);
                ExportDatabaseDialogFragment.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ExportDatabaseDialogFragment.this.progressBar.setProgress(percent);
                    }
                });
            }
            // Cleaning up and closing
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_export_database, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }

    /**
     * Class to provide necessary information about new archive items and to receive information about the progress of the operation.
     */
    private final class OutCreateCallback implements IOutCreateCallback<IOutItem7z>, ICryptoGetTextPassword {
        private final File currentDatabase;
        private RandomAccessFileInStream randomAccessFileInStream;
        private final String filenameInsideArchive;

        /**
         * Constructor that creates current opened database's File object
         */
        public OutCreateCallback(String filenameInsideArchive) {
            this.currentDatabase = new File(sharedPreferences.getString("databaseUri", null));
            this.filenameInsideArchive = filenameInsideArchive;
        }

        public void closeRandomAccessFileInStream() {
            try {
                this.randomAccessFileInStream.close();
            } catch (IOException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_close_compression_input_stream, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void setOperationResult(boolean operationResultOk) throws SevenZipException {

        }

        @Override
        public IOutItem7z getItemInformation(int index, OutItemFactory<IOutItem7z> outItemFactory) throws SevenZipException {
            IOutItem7z item = outItemFactory.createOutItem();
            item.setDataSize(this.currentDatabase.length());
            item.setPropertyIsDir(false);
            item.setPropertyIsAnti(false);
            item.setDataSize(this.currentDatabase.length());
            item.setPropertyPath(this.filenameInsideArchive);
            return item;
        }

        @Override
        public ISequentialInStream getStream(int index) throws SevenZipException {
            try {
                this.randomAccessFileInStream = new RandomAccessFileInStream(new RandomAccessFile(currentDatabase, "r"));
            } catch (FileNotFoundException e) {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
            }
            return randomAccessFileInStream;
        }

        @Override
        public void setTotal(long total) throws SevenZipException {
            ExportDatabaseDialogFragment.this.fileSize = total;
        }

        /**
         * Calculates how many percentages where process and set's progress bar to that value
         * @param complete amount of bytes that were processed
         * @throws SevenZipException Error during compression
         */
        @Override
        public void setCompleted(long complete) throws SevenZipException {
            int percent = (int) (complete * 100 / ExportDatabaseDialogFragment.this.fileSize);
            ExportDatabaseDialogFragment.this.handler.post(new Runnable() {
                @Override
                public void run() {
                    ExportDatabaseDialogFragment.this.progressBar.setProgress(percent);
                }
            });
        }

        @Override
        public String cryptoGetTextPassword() throws SevenZipException {
            return ExportDatabaseDialogFragment.this.password1.getText().toString();
        }
    }
}
