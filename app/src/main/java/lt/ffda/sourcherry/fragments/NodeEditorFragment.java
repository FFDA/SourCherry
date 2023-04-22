/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;

public class NodeEditorFragment extends Fragment {
    private LinearLayout nodeEditorFragmentLinearLayout;
    private MainViewModel mainViewModel;
    private Handler handler;
    private ExecutorService executor;
    private SharedPreferences sharedPreferences;
    private boolean textChanged = false;
    private boolean changesSaved = false;
    private TextWatcher textWatcher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_node_editor, container, false);
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.nodeEditorFragmentLinearLayout = view.findViewById(R.id.node_edit_fragment_linearlayout);
        this.handler = ((MainView) getActivity()).getHandler();
        this.executor = ((MainView) getActivity()).getExecutor();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                NodeEditorFragment.this.textChanged = true;
                NodeEditorFragment.this.removeTextChangedListeners();
            }
        };

        // Loading new menu for the fragment
        // that only have a save button in it
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.options_menu_node_editor_fragment, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    getActivity().onBackPressed();
                    return true;
                } else if (menuItem.getItemId() == R.id.toolbar_button_save_node) {
                    NodeEditorFragment.this.saveNodeContent();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        this.requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                NodeEditorFragment.this.loadContent();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Top and bottom paddings are always the same: 14px (5dp)
        this.nodeEditorFragmentLinearLayout.setPadding(this.sharedPreferences.getInt("paddingStart", 14), 14, this.sharedPreferences.getInt("paddingEnd", 14), 14);
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (NodeEditorFragment.this.textChanged) {
                String unsavedChangesDefaultPreference = NodeEditorFragment.this.sharedPreferences.getString("preferences_unsaved_changes", null);
                if (unsavedChangesDefaultPreference == null || unsavedChangesDefaultPreference.equals("ask")) {
                    NodeEditorFragment.this.createUnsavedChangesAlertDialog();
                } else if (unsavedChangesDefaultPreference.equals("save")) {
                    NodeEditorFragment.this.saveNodeContent();
                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    remove(); // Otherwise there will be onBackPressed infinite loop
                    ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
                } else {
                    View view = getActivity().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    remove(); // Otherwise there will be onBackPressed infinite loop
                    ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
                }
            } else {
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                remove(); // Otherwise there will be onBackPressed infinite loop
                ((MainView) getActivity()).returnFromFragmentWithHomeButton(changesSaved);
            }
        }
    };

    /**
     * Loads node content to UI
     */
    public void loadContent() {
        // Clears layout just in case. Most of the time it is needed
        if (this.nodeEditorFragmentLinearLayout != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeEditorFragment.this.nodeEditorFragmentLinearLayout.removeAllViews();
                }
            });
        }

        for (ArrayList<CharSequence[]> part : mainViewModel.getNodeContent()) {
            CharSequence[] type = part.get(0);
            if (type[0].equals("text")) {
                // This adds not only text, but images, codeboxes
                CharSequence[] textContent = part.get(1);
                SpannableStringBuilder nodeContentSSB = (SpannableStringBuilder) textContent[0];
                EditText editText = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
                editText.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                editText.addTextChangedListener(textWatcher);
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeEditorFragment.this.nodeEditorFragmentLinearLayout.addView(editText);
                    }
                });
            }
//            if (type[0].equals("table")) {
//                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
//                TableLayout table = new TableLayout(getActivity());
//
//                //// Getting max and min column values from table
//                // Multiplying by arbitrary number to make it look better.
//                // For some reason table that looks good in PC version looks worse on android
//                int colMax = (int) (Integer.parseInt((String) type[1]) * 1.3);
//                int colMin = (int) (Integer.parseInt((String) type[2]) * 1.3);
//                ////
//
//                // Wraps content in cell correctly
//                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
//
//                //// Creates and formats header for the table
//                CharSequence[] tableHeaderCells = part.get(part.size() - 1);
//                TableRow tableHeaderRow = new TableRow(getActivity());
//
//                for (CharSequence cell: tableHeaderCells) {
//                    TextView headerTextView = new TextView(getActivity());
//                    headerTextView.setBackground(getActivity().getDrawable(R.drawable.table_header_cell));
//                    headerTextView.setMinWidth(colMin);
//                    headerTextView.setMaxWidth(colMax);
//                    headerTextView.setPadding(10,10,10,10);
//                    headerTextView.setLayoutParams(params);
//                    headerTextView.setText(cell);
//                    headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
//                    tableHeaderRow.addView(headerTextView);
//                }
//                table.addView(tableHeaderRow);
//                ////
//
//                //// Creates and formats data for the table
//                for (int row = 1; row < part.size() - 1; row++) {
//                    TableRow tableRow = new TableRow(getActivity());
//                    CharSequence[] tableRowCells = part.get(row);
//                    for (CharSequence cell: tableRowCells) {
//                        TextView cellTextView = new TextView(getActivity());
//                        cellTextView.setBackground(getActivity().getDrawable(R.drawable.table_data_cell));
//                        cellTextView.setMinWidth(colMin);
//                        cellTextView.setMaxWidth(colMax);
//                        cellTextView.setPadding(10,10,10,10);
//                        cellTextView.setLayoutParams(params);
//                        cellTextView.setText(cell);
//                        cellTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
//                        tableRow.addView(cellTextView);
//                    }
//                    table.addView(tableRow);
//                }
//                ////
//
//                table.setBackground(getActivity().getDrawable(R.drawable.table_border));
//                tableScrollView.addView(table);
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        NodeEditorFragment.this.nodeEditorFragmentLinearLayout.addView(tableScrollView);
//                    }
//                });
//            }
        }
        // Shows keyboard if opened node for editing has less than 2 characters in the EditText
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                if (mainViewModel.getNodeContent().size() == 1) {
                    EditText editText = (EditText) NodeEditorFragment.this.nodeEditorFragmentLinearLayout.getChildAt(0);
                    if (editText.getText().length() <= 1) {
                        editText.requestFocus();
                        WindowCompat.getInsetsController(getActivity().getWindow(), editText).show(WindowInsetsCompat.Type.ime());
                    }
                }
            }
        });

    }

    /**
     * Saves current content of the that is being displayed to
     * the database
     */
    private void saveNodeContent() {
        if (nodeEditorFragmentLinearLayout.getChildCount() == 1) {
            this.changesSaved = true;
            this.textChanged = false;
            EditText editText = (EditText) this.nodeEditorFragmentLinearLayout.getChildAt(0);
            ((MainView) getActivity()).getReader().saveNodeContent(getArguments().getString("nodeUniqueID"), editText.getText().toString());
        }
        this.addTextChangedListeners();
    }

    /**
     * Adds textChangedListeners for all EditText views
     * Does it in the background
     */
    private void addTextChangedListeners() {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
                    View view = nodeEditorFragmentLinearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((EditText) view).addTextChangedListener(textWatcher);
                    }
                }
            }
        });
    }

    /**
     * Removes all textChangedListeners from EditText views
     * Does it in the background
     */
    private void removeTextChangedListeners() {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
                    View view = nodeEditorFragmentLinearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        ((EditText) view).removeTextChangedListener(textWatcher);
                    }
                }
            }
        });
    }

    /**
     * Creates dialog to ask user if changes in the editor has to be saved
     * User can check the checkbox to save the choice to the database
     */
    private void createUnsavedChangesAlertDialog() {
        View checkboxView = View.inflate(getContext(), R.layout.alert_dialog_unsaved_changes, null);
        CheckBox checkBox = checkboxView.findViewById(R.id.alert_dialog_unsaved_changes_remember_choice_checkBox);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(checkboxView);
        builder.setTitle(R.string.alert_dialog_unsaved_changes_title);
        builder.setMessage(R.string.alert_dialog_unsaved_changes_message);
        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                NodeEditorFragment.this.textChanged = false;
                if (checkBox.isChecked()) {
                    NodeEditorFragment.this.saveUnsavedChangesDialogChoice("exit");
                }
                NodeEditorFragment.this.getActivity().onBackPressed();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    NodeEditorFragment.this.saveUnsavedChangesDialogChoice("save");
                }
                NodeEditorFragment.this.saveNodeContent();
                NodeEditorFragment.this.getActivity().onBackPressed();
            }
        });
        builder.show();
    }

    /**
     * Saves user choice to the preference key preferences_unsaved_changes
     * @param choice user choice (ask, exit, save)
     */
    private void saveUnsavedChangesDialogChoice(String choice) {
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString("preferences_unsaved_changes", choice);
        editor.apply();
    }
}
