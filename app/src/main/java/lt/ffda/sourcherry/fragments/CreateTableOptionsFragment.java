/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;

public class CreateTableOptionsFragment extends Fragment {

    /**
     * Creates custom OnBackPressedCallback for this fragment do handle back button press
     * @return OnBackPressedCallback
     */
    private OnBackPressedCallback createOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                remove();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return getLayoutInflater().inflate(R.layout.fragment_create_table_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText rows = view.findViewById(R.id.create_table_table_size_rows_input);
        rows.setText("3", TextView.BufferType.NORMAL);
        EditText cols = view.findViewById(R.id.create_table_table_size_cols_input);
        cols.setText("3", TextView.BufferType.NORMAL);
        EditText defaultWidth = view.findViewById(R.id.create_table_col_properties_default_width_input);
        defaultWidth.setText("100", TextView.BufferType.NORMAL);
        CheckBox lightInterface = view.findViewById(R.id.create_table_col_properties_lightwreight_interface);
        Button insertButton = view.findViewById(R.id.create_table_button_insert);
        insertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateInputs()) {
                    return;
                }
                Bundle result = new Bundle();
                result.putInt("rows", Integer.valueOf(rows.getText().toString()));
                result.putInt("cols", Integer.valueOf(cols.getText().toString()));
                result.putInt("defaultWidth", Integer.valueOf(defaultWidth.getText().toString()));
                result.putByte("lightInterface", lightInterface.isChecked() ? (byte) 1 : (byte) 0);
                getParentFragmentManager().setFragmentResult("tablePropertiesListener", result);
                resetTitleBar();
                getParentFragmentManager().popBackStack();
            }
        });
        Button cancelButton = view.findViewById(R.id.create_table_button_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTitleBar();
                getParentFragmentManager().popBackStack();
            }
        });
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.options_menu_fragment_with_home_button, menu);
                getActivity().setTitle("Insert table");
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == android.R.id.home) {
                    getParentFragmentManager().popBackStack();
                    resetTitleBar();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), createOnBackPressedCallback());
    }

    /**
     * Set's title bar of the activity to the current opened node
     */
    private void resetTitleBar() {
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        getActivity().setTitle(mainViewModel.getCurrentNode().getName());
    }

    /**
     * Displays toast message with provided text
     * @param message message text
     */
    private void showTaost(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks all inputs if they have allowed values. Displays a message if thelua in a field is not
     * allowed.
     * @return true if all values in the fields are correct, false - otherwise.
     */
    private boolean validateInputs() {
        EditText rows = getView().findViewById(R.id.create_table_table_size_rows_input);
        EditText cols = getView().findViewById(R.id.create_table_table_size_cols_input);
        EditText defaultWidth = getView().findViewById(R.id.create_table_col_properties_default_width_input);
        if (rows.getText().length() == 0) {
            showTaost(getString(R.string.toast_message_fill_in_the_row_count));
            return false;
        }
        if (Integer.valueOf(rows.getText().toString()) == 0) {
            showTaost(getString(R.string.toast_message_row_count_cant_be_0));
            return false;
        }
        if (cols.getText().length() == 0) {
            showTaost(getString(R.string.toast_message_fill_in_the_cplumn_count));
            return false;
        }
        if (Integer.valueOf(cols.getText().toString()) == 0) {
            showTaost(getString(R.string.toast_message_column_count_cant_be_0));
            return false;
        }
        if (defaultWidth.getText().length() == 0) {
            showTaost(getString(R.string.toast_message_fill_in_default_col_width));
            return false;
        }
        if (Integer.valueOf(cols.getText().toString()) == 0) {
            showTaost(getString(R.string.toast_message_default_column_width_cant_be_0));
            return false;
        }
        return true;
    }
}
