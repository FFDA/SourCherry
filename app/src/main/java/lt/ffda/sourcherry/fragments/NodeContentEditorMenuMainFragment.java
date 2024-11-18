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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.utils.ColorPickerPresets;
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog;
import me.jfenn.colorpickerdialog.interfaces.OnColorPickedListener;

public class NodeContentEditorMenuMainFragment extends Fragment {
    private int color;
    private SharedPreferences sharedPreferences;
    private NodeContentEditorMainMenuActions nodeContentEditorMenuActions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        color = sharedPreferences.getInt("colorPickerColor", ColorPickerPresets.BLACK.getColor());
        return inflater.inflate(R.layout.edit_node_fragment_button_row_main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton clearFormattingButton = view.findViewById(R.id.edit_node_fragment_button_row_clear_formatting);
        clearFormattingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.clearFormatting();
                }
            }
        });
        ImageButton colorPicker = view.findViewById(R.id.edit_node_fragment_button_row_color_picker);
        colorPicker.setColorFilter(color);
        colorPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPicker();
            }
        });
        ImageButton foregroundColorButton = view.findViewById(R.id.edit_node_fragment_button_row_foreground_color);
        foregroundColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.changeForegroundColor();
                }
            }
        });
        ImageButton backgroundColorButton = view.findViewById(R.id.edit_node_fragment_button_row_background_color);
        backgroundColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.changeBackgroundColor();
                }
            }
        });
        ImageButton boldButton = view.findViewById(R.id.edit_node_fragment_button_row_bold);
        boldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.toggleFontBold();
                }
            }
        });
        ImageButton italicButton = view.findViewById(R.id.edit_node_fragment_button_row_italic);
        italicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.toggleFontItalic();
                }
            }
        });
        ImageButton underlineButton = view.findViewById(R.id.edit_node_fragment_button_row_underline);
        underlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.toggleFontUnderline();
                }
            }
        });
        ImageButton strikethoughButton = view.findViewById(R.id.edit_node_fragment_button_row_strikethrough);
        strikethoughButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.toggleFontStrikethrough();
                }
            }
        });
        ImageButton monospaceButton = view.findViewById(R.id.edit_node_fragment_button_row_monospace);
        monospaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.toggleFontMonospace();
                }
            }
        });
        ImageButton showInsertRowButton = view.findViewById(R.id.edit_node_fragment_button_row_insert);
        showInsertRowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuActions != null) {
                    nodeContentEditorMenuActions.showInsertRow();
                }
            }
        });
    }

    /**
     * Set an instance of the parent fragment that implements NodeContentEditorMenuActions to be
     * able to manipulate it's content
     * @param nodeContentEditorMenuActions instance of fragment that implelents NodeContentEditorMenuActions
     */
    public void setNodeContentEditorMenuActions(NodeContentEditorMainMenuActions nodeContentEditorMenuActions) {
        this.nodeContentEditorMenuActions = nodeContentEditorMenuActions;
    }

    /**
     * Shows color picker fragment for user to choose color that will be used when changing
     * background or foreground color of a text
     */
    private void showColorPicker() {
        ImageButton colorPicker = getView().findViewById(R.id.edit_node_fragment_button_row_color_picker);
        colorPicker.setColorFilter(color);
        new ColorPickerDialog()
                .withColor(color)
                .withPresets(
                        ColorPickerPresets.BLUE.getColor(),
                        ColorPickerPresets.GREEN.getColor(),
                        ColorPickerPresets.YELLOW.getColor(),
                        ColorPickerPresets.ORANGE.getColor(),
                        ColorPickerPresets.RED.getColor(),
                        ColorPickerPresets.VIOLET.getColor(),
                        ColorPickerPresets.BROWN.getColor(),
                        ColorPickerPresets.WHITE.getColor(),
                        ColorPickerPresets.BLACK.getColor()
                )
                .withListener(new OnColorPickedListener<ColorPickerDialog>() {
                    @Override
                    public void onColorPicked(@Nullable ColorPickerDialog pickerView, int color) {
                        NodeContentEditorMenuMainFragment.this.color = color;
                        if (nodeContentEditorMenuActions != null) {
                            nodeContentEditorMenuActions.setColor(color);
                        }
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        colorPicker.setColorFilter(color);
                        editor.putInt("colorPickerColor", color);
                        editor.apply();
                    }
                })
                .show(getParentFragmentManager(), "colorPicker");
    }
}
