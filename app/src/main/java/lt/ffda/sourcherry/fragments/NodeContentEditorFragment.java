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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableRow;
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
import lt.ffda.sourcherry.customUiElements.ScTableLayout;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;

public class NodeContentEditorFragment extends Fragment {
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
                NodeContentEditorFragment.this.textChanged = true;
                NodeContentEditorFragment.this.removeTextChangedListeners();
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
                    NodeContentEditorFragment.this.saveNodeContent();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        this.requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
        if (savedInstanceState == null) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    NodeContentEditorFragment.this.loadContent();
                }
            });
        } else {
            this.textChanged = savedInstanceState.getBoolean("textChanged");
            this.changesSaved = savedInstanceState.getBoolean("changesSaved");
            EditText editText = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
            editText.setText(savedInstanceState.getString("content"), TextView.BufferType.EDITABLE);
            editText.addTextChangedListener(textWatcher);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(editText);
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.changesSaved || this.textChanged) {
            outState.putBoolean("textChanged", this.textChanged);
            outState.putBoolean("changesSaved", this.changesSaved);
            EditText editText = (EditText) this.nodeEditorFragmentLinearLayout.getChildAt(0);
            outState.putString("content", editText.getText().toString());
        }
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
            if (NodeContentEditorFragment.this.textChanged) {
                String unsavedChangesDefaultPreference = NodeContentEditorFragment.this.sharedPreferences.getString("preferences_unsaved_changes", null);
                if (unsavedChangesDefaultPreference == null || unsavedChangesDefaultPreference.equals("ask")) {
                    NodeContentEditorFragment.this.createUnsavedChangesAlertDialog();
                } else if (unsavedChangesDefaultPreference.equals("save")) {
                    NodeContentEditorFragment.this.saveNodeContent();
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
                    NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.removeAllViews();
                }
            });
        }

        for (ScNodeContent part : mainViewModel.getNodeContent()) {
            if (part.getContentType() == 0) {
                // This adds not only text, but images, codeboxes
                ScNodeContentText scNodeContentText = (ScNodeContentText) part;
                SpannableStringBuilder nodeContentSSB = (SpannableStringBuilder) scNodeContentText.getContent();
                EditText editText = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
                editText.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                editText.addTextChangedListener(textWatcher);
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(editText);
                    }
                });
            } else {
                HorizontalScrollView tableScrollView = new HorizontalScrollView(getActivity());
                ScTableLayout table = new ScTableLayout(getActivity());
                ScNodeContentTable scNodeContentTable = (ScNodeContentTable) part;
                // Setting gravity for the table
                LinearLayout.LayoutParams tableScrollViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                switch (scNodeContentTable.getJustification()) {
                    case "right":
                        tableScrollViewParams.gravity = Gravity.RIGHT;
                        break;
                    case "center":
                        tableScrollViewParams.gravity = Gravity.CENTER;
                        break;
                    case "fill":
                        tableScrollViewParams.gravity = Gravity.FILL;
                        break;
                }
                tableScrollView.setLayoutParams(tableScrollViewParams);
                table.setLightInterface(scNodeContentTable.getLightInterface());
                // Multiplying by arbitrary number to make table cells look better.
                // For some reason table that looks good in PC version looks worse on android
                int colMin = (int) (scNodeContentTable.getColMin() * 1.3);
                int colMax = (int) (scNodeContentTable.getColMax() * 1.3);
                // Wraps content in cell correctly
                TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);

                //// Creates and formats header for the table
                CharSequence[] tableHeaderCells = scNodeContentTable.getContent().get(scNodeContentTable.getContent().size() - 1);
                TableRow tableHeaderRow = new TableRow(getActivity());
                for (CharSequence cell: tableHeaderCells) {
                    EditText headerTextView = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
                    headerTextView.setBackground(getActivity().getDrawable(R.drawable.table_header_cell));
                    headerTextView.setMinWidth(colMin);
                    headerTextView.setMaxWidth(colMax);
                    headerTextView.setPadding(10,10,10,10);
                    headerTextView.setLayoutParams(params);
                    headerTextView.setText(cell);
                    headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
                    tableHeaderRow.addView(headerTextView);
                }
                table.addView(tableHeaderRow);
                ////

                //// Creates and formats data for the table
                for (int row = 0; row < scNodeContentTable.getContent().size() - 1; row++) {
                    TableRow tableRow = new TableRow(getActivity());
                    CharSequence[] tableRowCells = scNodeContentTable.getContent().get(row);
                    for (CharSequence cell: tableRowCells) {
                        EditText cellTextView = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
                        cellTextView.setBackground(getActivity().getDrawable(R.drawable.table_data_cell));
                        cellTextView.setMinWidth(colMin);
                        cellTextView.setMaxWidth(colMax);
                        cellTextView.setPadding(10,10,10,10);
                        cellTextView.setLayoutParams(params);
                        cellTextView.setText(cell);
                        cellTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size", 15));
                        tableRow.addView(cellTextView);
                    }
                    table.addView(tableRow);
                }
                ////

                table.setBackground(getActivity().getDrawable(R.drawable.table_border));
                tableScrollView.addView(table);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.addView(tableScrollView);
                    }
                });
            }
        }
        // Shows keyboard if opened node for editing has less than 2 characters in the EditText
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                if (mainViewModel.getNodeContent().size() == 1) {
                    EditText editText = (EditText) NodeContentEditorFragment.this.nodeEditorFragmentLinearLayout.getChildAt(0);
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
        this.changesSaved = true;
        this.textChanged = false;
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        for (int i = 0; i < nodeEditorFragmentLinearLayout.getChildCount(); i++) {
            View view = this.nodeEditorFragmentLinearLayout.getChildAt(i);
            // if it a table
            if (view instanceof HorizontalScrollView) {
                ScTableLayout tableLayout = (ScTableLayout) ((HorizontalScrollView) view).getChildAt(0);
                String justification;
                // Getting justification of the table
                LinearLayout.LayoutParams tableScrollView = (LinearLayout.LayoutParams) view.getLayoutParams();
                switch (tableScrollView.gravity) {
                    case Gravity.RIGHT:
                        justification = "right";
                        break;
                    case Gravity.CENTER:
                        justification = "center";
                        break;
                    case Gravity.FILL:
                        justification = "fill";
                        break;
                    default:
                        justification = "left";
                        break;
                }
                ArrayList<CharSequence[]> tableContent = new ArrayList<>();
                // Getting table content
                // Starting from second row, because header row has to be stored last in the database
                for (int row = 1; row < tableLayout.getChildCount(); row++) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                    CharSequence[] rowCells = new CharSequence[tableRow.getChildCount()];
                    for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                        EditText currentCell = (EditText) tableRow.getChildAt(cell);
                        currentCell.clearComposingText();
                        rowCells[cell] = currentCell.getText();
                    }
                    tableContent.add(rowCells);
                }
                // Getting table header
                int colMin = 0;
                int colMax = 0;
                TableRow tableHeader = (TableRow) tableLayout.getChildAt(0);
                CharSequence[] headerCells = new CharSequence[tableHeader.getChildCount()];
                for (int cell = 0; cell < tableHeader.getChildCount(); cell++) {
                    EditText currentCell = (EditText) tableHeader.getChildAt(0);
                    currentCell.clearComposingText();
                    headerCells[cell] = currentCell.getText();
                    if (cell == 0) {
                        // Getting colMin & colMan. Dividing by the same arbitrary
                        // value it was multiplied when the table was created
                        colMin = (int) (currentCell.getMinWidth() / 1.3);
                        colMax = (int) (currentCell.getMaxWidth() / 1.3);
                    }
                }
                tableContent.add(headerCells);
                nodeContent.add(new ScNodeContentTable((byte) 1, tableContent, colMin, colMax, tableLayout.getLightInterface(), justification));
            } else {
                EditText editText = (EditText) view;
                editText.clearComposingText();
                nodeContent.add(new ScNodeContentText((byte) 0, (SpannableStringBuilder) editText.getText()));
            }
        }
        // Setting new node content
        this.mainViewModel.setNodeContent(nodeContent);
        DatabaseReaderFactory.getReader().saveNodeContent(getArguments().getString("nodeUniqueID"));
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
                NodeContentEditorFragment.this.textChanged = false;
                if (checkBox.isChecked()) {
                    NodeContentEditorFragment.this.saveUnsavedChangesDialogChoice("exit");
                }
                NodeContentEditorFragment.this.getActivity().onBackPressed();
            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkBox.isChecked()) {
                    NodeContentEditorFragment.this.saveUnsavedChangesDialogChoice("save");
                }
                NodeContentEditorFragment.this.saveNodeContent();
                NodeContentEditorFragment.this.getActivity().onBackPressed();
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
