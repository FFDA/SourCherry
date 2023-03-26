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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;

public class ExportDatabaseDialogFragment extends DialogFragment {
    private ExecutorService executor;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private ProgressBar progressBar;
    private TextView message;
//    private Button buttonCancel;
//    private Button buttonExport;

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
        this.message = view.findViewById(R.id.dialog_fragment_export_database_message);

//        CheckBox checkBox = view.findViewById(R.id.dialog_fragment_export_database_checkbox_password_protect);
//        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                TextInputLayout editTextPassword1 = view.findViewById(R.id.dialog_fragment_export_database_password1_layout);
//                TextInputLayout editTextPassword2 = view.findViewById(R.id.dialog_fragment_export_database_password2_layout);
//                if (isChecked) {
//                    editTextPassword1.setVisibility(View.VISIBLE);
//                    editTextPassword2.setVisibility(View.VISIBLE);
//                } else {
//                    editTextPassword1.setVisibility(View.GONE);
//                    editTextPassword2.setVisibility(View.GONE);
//                }
//            }
//        });
//        this.buttonCancel = view.findViewById(R.id.dialog_fragment_export_database_button_cancel);
//        buttonCancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismiss();
//            }
//        });
//
//        this.buttonExport = view.findViewById(R.id.dialog_fragment_export_database_button_export);
//        buttonExport.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // At this point user can't dismiss dialog fragment
//                // It will be done after export action is done
////                ExportDatabaseDialogFragment.this.enableUI(false);
//                getDialog().setCancelable(false);
//                if (checkBox.isChecked()) {
//                    ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            ExportDatabaseDialogFragment.this.encryptDatabase();
//                        }
//                    });
//                } else {
//                    ExportDatabaseDialogFragment.this.executor.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            ExportDatabaseDialogFragment.this.exportDatabase();
//                        }
//                    });
//                }
//            }
//        });
        getDialog().setCancelable(false);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                ExportDatabaseDialogFragment.this.exportDatabase();
            }
        });
    }

//    private void enableUI(boolean enable) {
//        getDialog().setCancelable(enable);
//        this.buttonCancel.setEnabled(enable);
//        this.buttonExport.setEnabled(enable);
//    }

//    private void encryptDatabase() {
//        EditText password1 = getView().findViewById(R.id.dialog_fragment_export_database_password1);
//        EditText password2 = getView().findViewById(R.id.dialog_fragment_export_database_password2);
//        if (password1.getText().toString().equals(password2.getText().toString())) {
//            // Setting appropriate file extension for archive
//            String databaseExtension = this.sharedPreferences.getString("databaseFileExtension", null);
//            if (databaseExtension.equals("ctd")) {
//                // XML
//                databaseExtension = "ctz";
//            } else if (databaseExtension.equals("ctb")) {
//                // SQL
//                databaseExtension = "ctx";
//            }
//        } else {
//            this.handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getContext(), R.string.toast_error_passwords_do_not_match, Toast.LENGTH_SHORT).show();
//                    ExportDatabaseDialogFragment.this.enableUI(true);
//                }
//            });
//        }
//    }

    /**
     * Copies file from app-specific storage to external storage
     */
    private void exportDatabase(){
        try {
            InputStream inputStream = new FileInputStream(this.sharedPreferences.getString("databaseUri", null));
            OutputStream outputStream = getContext().getContentResolver().openOutputStream(Uri.parse(getArguments().getString("exportFileUri")));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    ExportDatabaseDialogFragment.this.progressBar.setIndeterminate(false);
                    ExportDatabaseDialogFragment.this.message.setText(R.string.dialog_fragment_export_database_message_exporting_database);
                }
            });

            // Copying files
            long fileSize = inputStream.available();
            long totalLen = 0;
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                ExportDatabaseDialogFragment.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ExportDatabaseDialogFragment.this.progressBar.setProgress(percent);
                    }
                });
            }
            // Cleaning uo and closing
            inputStream.close();
            outputStream.close();
            getDialog().dismiss();
        } catch (Exception e) {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_failed_to_export_database, Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        }
    }
}
