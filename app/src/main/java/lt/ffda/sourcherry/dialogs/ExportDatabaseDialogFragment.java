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

import android.content.SharedPreferences;
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
import java.util.concurrent.ExecutorService;

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
        this.executor = ((MainView) getActivity()).getExecutor();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        this.handler = ((MainView) getActivity()).getHandler();
        this.progressBar = view.findViewById(R.id.dialog_fragment_export_database_progress_bar);
        this.passwordCheckBox = view.findViewById(R.id.dialog_fragment_export_database_checkbox_password_protect);
        this.password1 = view.findViewById(R.id.dialog_fragment_export_database_password1);
        this.password2 = view.findViewById(R.id.dialog_fragment_export_database_password2);
        this.message = view.findViewById(R.id.dialog_fragment_export_database_message);

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
                    ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            ExportDatabaseDialogFragment.this.encryptDatabase();
                        }
                    });
                } else {
                    ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                InputStream inputStream = new FileInputStream(ExportDatabaseDialogFragment.this.sharedPreferences.getString("databaseUri", null));
                                OutputStream outputStream = getContext().getContentResolver().openOutputStream(Uri.parse(getArguments().getString("exportFileUri")));
                                ExportDatabaseDialogFragment.this.exportDatabase(inputStream, outputStream);
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
     */
    private void encryptDatabase() {
        if (this.password1.getText().toString().equals(this.password2.getText().toString())) {
            // Setting appropriate file extension for archive
            String databaseExtensionOriginal = this.sharedPreferences.getString("databaseFileExtension", null);
            String databaseExtensionCompressed = null;
            if (databaseExtensionOriginal.equals("ctd")) {
                // XML
                databaseExtensionCompressed = "ctz";
            } else if (databaseExtensionOriginal.equals("ctb")) {
                // SQL
                databaseExtensionCompressed = "ctx";
            }
            Uri outputFileUri = Uri.parse(getArguments().getString("exportFileUri"));
            DocumentFile outputDocumentFile = DocumentFile.fromSingleUri(getContext(), outputFileUri);
            String databaseFilename = outputDocumentFile.getName().substring(0, outputDocumentFile.getName().lastIndexOf('.') + 1);
            File tmpCompressedDatabase = this.compressDatabase(databaseFilename + databaseExtensionOriginal);
            if (tmpCompressedDatabase != null) {
                try {
                    // Opening streams
                    InputStream inputStream = new FileInputStream(tmpCompressedDatabase);
                    OutputStream outputStream = getContext().getContentResolver().openOutputStream(outputFileUri);
                    ExportDatabaseDialogFragment.this.exportDatabase(inputStream, outputStream);
                    // Changing filename extension to match database type
                    DocumentsContract.renameDocument(getContext().getContentResolver(), outputFileUri, databaseFilename + databaseExtensionCompressed);
                } catch (FileNotFoundException e) {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_open_input_or_output_stream, Toast.LENGTH_SHORT).show();
                    ExportDatabaseDialogFragment.this.dismiss();
                }
            } else {
                Toast.makeText(getContext(), R.string.toast_error_failed_to_compress_database, Toast.LENGTH_SHORT).show();
                this.dismiss();
            }
        } else {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
                    ExportDatabaseDialogFragment.this.enableUI(true);
                }
            });
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
            getDialog().dismiss();
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
