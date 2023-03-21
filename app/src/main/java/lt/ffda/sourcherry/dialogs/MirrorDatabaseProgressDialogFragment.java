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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

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

import lt.ffda.sourcherry.MainActivity;
import lt.ffda.sourcherry.R;

public class MirrorDatabaseProgressDialogFragment extends DialogFragment {
    private SharedPreferences sharedPreferences;
    private ProgressBar progressBar;
    private TextView message;
    private EditText passwordTextedit;
    private LinearLayout buttonLayout;
    private Button buttonOK;
    private Button buttonCancel;
    private ExecutorService executor;
    private Handler handler;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        //// Dialog fragment layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_mirror_database_progress, null);
        builder.setView(view);

        setCancelable(false); // Not allowing user to cancel the the dialog fragment

        // Setting up variables
        this.progressBar = view.findViewById(R.id.mirror_database_progress_fragment_progressBar);
        this.message = view.findViewById(R.id.mirror_database_progress_fragment_message);
        this.passwordTextedit = view.findViewById(R.id.mirror_database_progress_fragment_password_textedit);
        this.buttonLayout = view.findViewById(R.id.mirror_database_progress_fragment_button_layout);
        this.buttonOK = view.findViewById(R.id.mirror_database_progress_fragment_password_button_ok);
        this.buttonCancel = view.findViewById(R.id.mirror_database_progress_fragment_password_button_cancel);
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        String databaseFileExtension = getArguments().getString("mirrorDatabaseFileExtension");

        if (databaseFileExtension.equals("ctb")) {
            this.message.setText(R.string.open_database_fragment_copying_database_message);
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    MirrorDatabaseProgressDialogFragment.this.copyDatabaseToAppSpecificStorage();
                    MirrorDatabaseProgressDialogFragment.this.getDialog().cancel();
                }
            });
        }
        if (databaseFileExtension.equals("ctz") || databaseFileExtension.equals("ctx")) {
            this.passwordTextedit.setVisibility(View.VISIBLE);
            this.buttonLayout.setVisibility(View.VISIBLE);

            //// Code to show keyboard
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            this.passwordTextedit.requestFocus();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Shows keyboard on API 30 (Android 11) reliably
                WindowCompat.getInsetsController(getDialog().getWindow(), this.passwordTextedit).show(WindowInsetsCompat.Type.ime());
            } else {
                new Handler().postDelayed(new Runnable() {
                    // Delays to show soft keyboard by few milliseconds
                    // Otherwise keyboard does not show up
                    // It's a bit hacky (should be fixed)
                    @Override
                    public void run() {
                        imm.showSoftInput(MirrorDatabaseProgressDialogFragment.this.passwordTextedit, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 20);
            }
            ////

            this.message.setText(R.string.mirror_database_fragment_enter_password_message);

            this.buttonOK.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MirrorDatabaseProgressDialogFragment.this.runExtractDatabase();
                }
            });

            this.buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MirrorDatabaseProgressDialogFragment.this.dismiss();
                }
            });

            this.passwordTextedit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    boolean handle = false;
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        handle = true;
                        MirrorDatabaseProgressDialogFragment.this.runExtractDatabase();
                    }
                    return handle;
                }
            });
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        dismissNow();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if (this.sharedPreferences.getBoolean("checkboxAutoOpen", false)) {
            ((MainActivity) getActivity()).startMainViewActivity();
        }
        dismissNow();
    }

    @Override
    public void onDestroyView() {
        executor.shutdownNow();
        super.onDestroyView();
    }

    private void copyDatabaseToAppSpecificStorage() {
        // Set directory where database will be saved depending on user's choice in options menu
        String databaseOutputFile = this.sharedPreferences.getString("databaseUri", null);
        Uri databaseUri = Uri.parse(getArguments().getString("mirrorDatabaseUri"));
        long totalLen = 0;

        try {
            InputStream databaseInputStream = getContext().getContentResolver().openInputStream(databaseUri);
            OutputStream databaseOutputStream = new FileOutputStream(databaseOutputFile, false);
            long fileSize = databaseInputStream.available();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MirrorDatabaseProgressDialogFragment.this.progressBar.setIndeterminate(false);
                }
            });

            // Copying files
            byte[] buf = new byte[8 * 1024];
            int len;
            while ((len = databaseInputStream.read(buf)) > 0) {
                databaseOutputStream.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MirrorDatabaseProgressDialogFragment.this.progressBar.setProgress(percent);
                    }
                });
            }

            databaseInputStream.close();
            databaseOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), R.string.toast_error_could_not_open_a_file_to_copy_the_database, Toast.LENGTH_LONG).show();
                }
            });
            getDialog().dismiss();
        } catch (IOException e) {
            // Disabled because clashed with intent-filters
            // Delete later if no issues arises
            // This exception is for close() method
            // If app continues to work after this exception it can be ignored
            // It could be that input stream was already closed by fragment before this function was called
//            e.printStackTrace();
//            handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getContext(), R.string.toast_error_could_not_copy_the_database, Toast.LENGTH_LONG).show();
//                }
//            });
            getDialog().dismiss();
        }

        this.saveDatabaseToPrefs(getArguments().getLong("mirrorDatabaseLastModified"));
    }

    private void extractDatabase(String password) {
        String databaseString = getArguments().getString("mirrorDatabaseUri");

        String tmpDatabaseFilename = "";
        long totalLen = 0; // Used to calculate percentage

        try {
            //// Copying file to temporary internal apps storage (cache)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MirrorDatabaseProgressDialogFragment.this.message.setText(R.string.open_database_fragment_copying_database_message);
                }
            });

            File tmpCompressedDatabase = File.createTempFile("tmpDatabaseFile", null);
            OutputStream os = new FileOutputStream(tmpCompressedDatabase, false);
            InputStream is = getContext().getContentResolver().openInputStream(Uri.parse(databaseString));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MirrorDatabaseProgressDialogFragment.this.progressBar.setIndeterminate(false);
                }
            });
            // Copying files
            long fileSize = is.available();
            byte[] buf = new byte[4 * 1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
                totalLen += len;
                int percent = (int) (totalLen * 100 / fileSize);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MirrorDatabaseProgressDialogFragment.this.progressBar.setProgress(percent);
                    }
                });
            }
            ////
            is.close();
            os.close();

            //// Extracting file to permanent internal apps storage
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MirrorDatabaseProgressDialogFragment.this.message.setText(R.string.open_database_fragment_extracting_database_message);
                    MirrorDatabaseProgressDialogFragment.this.progressBar.setProgress(0);
                }
            });

            SevenZFile sevenZFile = new SevenZFile(tmpCompressedDatabase, password.toCharArray());
            // Entry is a file inside the archive
            // In CherryTree database file there always should be one
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();

            while (entry != null) {
                tmpDatabaseFilename = entry.getName(); // Getting the (current) name of the file inside the archive
                // At some point filenames inside CherryTree password protected archives were changed to include a random(?) integer
                // in the middle of the filename. To make it look normal again I had to remove it
                String[] tmpDatabaseFilenameArray = tmpDatabaseFilename.split("\\."); // Splitting filename in to array at avery dot
                tmpDatabaseFilename = tmpDatabaseFilenameArray[0] + "." + tmpDatabaseFilenameArray[tmpDatabaseFilenameArray.length -1]; // Joining first and last part of the filename array
                totalLen = 0; // Resetting totalLen value
                FileOutputStream out = new FileOutputStream(new File(this.sharedPreferences.getString("databaseUri", null)));
                InputStream in = sevenZFile.getInputStream(entry);
                fileSize = entry.getSize();
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    totalLen += len;
                    int percent = (int) (totalLen * 100 / fileSize);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MirrorDatabaseProgressDialogFragment.this.progressBar.setProgress(percent);
                        }
                    });
                }
                out.close();
                in.close();
                entry = sevenZFile.getNextEntry();
            }

            this.saveDatabaseToPrefs(getArguments().getLong("mirrorDatabaseLastModified"));

            //// Cleaning up
//                tmpCompressedDatabase.delete(); // Using deleteTempFile onDestroy in MainActivity
            sevenZFile.close();
            ////
        } catch (FileNotFoundException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Error FileNotFoundException (1a): " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
            dismiss();
        } catch (IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Error IOException (1b): " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            e.printStackTrace();
            dismiss();
        }
    }

    /**
     * Saved passed parameters to the shared preferences
     * @param mirrorDatabaseLastModified last modified datetime of the mirror database long saved using key "mirrorDatabaseLastModified"
     */
    private void saveDatabaseToPrefs(long mirrorDatabaseLastModified) {
        // Saves passed information about database to preferences
        SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
        sharedPreferencesEditor.putLong("mirrorDatabaseLastModified", mirrorDatabaseLastModified);
        sharedPreferencesEditor.apply();
    }

    /**
     *  Runs extractDatabase(), passes user entered password as an argument
     *  Hides password field, and Cancel and Ok buttons
     *  Hides soft keyboard
     */
    private void runExtractDatabase() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        String password = MirrorDatabaseProgressDialogFragment.this.passwordTextedit.getText().toString();
        imm.hideSoftInputFromWindow(MirrorDatabaseProgressDialogFragment.this.passwordTextedit.getWindowToken(), 0); // Hides keyboard
        MirrorDatabaseProgressDialogFragment.this.passwordTextedit.setVisibility(View.GONE);
        MirrorDatabaseProgressDialogFragment.this.buttonLayout.setVisibility(View.GONE);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                MirrorDatabaseProgressDialogFragment.this.extractDatabase(password);
                MirrorDatabaseProgressDialogFragment.this.getDialog().cancel();
            }
        });
    }
}
