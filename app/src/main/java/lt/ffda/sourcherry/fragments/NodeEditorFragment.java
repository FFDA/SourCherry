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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;

public class NodeEditorFragment extends Fragment {
    private LinearLayout nodeEditorFragmentLinearLayout;
    private MainViewModel mainViewModel;
    private Handler handler;
    private SharedPreferences sharedPreferences;
    private boolean changed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_node_editor, container, false);
        this.mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        this.nodeEditorFragmentLinearLayout = view.findViewById(R.id.node_edit_fragment_linearlayout);
        this.handler = ((MainView) getActivity()).getHandler();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                    changed = true;
                    NodeEditorFragment.this.saveNodeContent();
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), this.onBackPressedCallback);
        loadContent();
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
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            remove(); // Otherwise there will be onBackPressed infinite loop
            ((MainView) getActivity()).returnFromFragmentWithHomeButton(changed);
        }
    };

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

        for (ArrayList<CharSequence[]> part: mainViewModel.getNodeContent()) {
            CharSequence[] type = part.get(0);
            if (type[0].equals("text")) {
                // This adds not only text, but images, codeboxes
                CharSequence[] textContent = part.get(1);
                SpannableStringBuilder nodeContentSSB = (SpannableStringBuilder) textContent[0];
                EditText editText = (EditText) getLayoutInflater().inflate(R.layout.custom_edittext, this.nodeEditorFragmentLinearLayout, false);
                editText.setText(nodeContentSSB, TextView.BufferType.EDITABLE);
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, this.sharedPreferences.getInt("preferences_text_size" , 15));
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
    }

    /**
     * Saves current content of the that is being displayed to
     * the database
     */
    private void saveNodeContent() {
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                if (nodeEditorFragmentLinearLayout.getChildCount() == 1) {
                    EditText editText = (EditText) nodeEditorFragmentLinearLayout.getChildAt(0);
                    ((MainView) getActivity()).getReader().saveNodeContent(getArguments().getString("nodeUniqueID"), editText.getText().toString());
                }
            }
        });
    }
}
