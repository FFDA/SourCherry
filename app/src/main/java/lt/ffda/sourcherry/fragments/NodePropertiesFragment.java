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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.R;

public class NodePropertiesFragment extends Fragment {
    RadioGroup radioGroupNodeType;
    CheckBox checkBoxExcludeFromSearchesThisNode;
    CheckBox checkBoxExcludeFromSearchesSubnodes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_add_new_node, container, false);
        this.radioGroupNodeType = view.findViewById(R.id.radio_group_node_type);
        this.checkBoxExcludeFromSearchesThisNode = view.findViewById(R.id.exclude_from_searches_this_node);
        this.checkBoxExcludeFromSearchesSubnodes = view.findViewById(R.id.exclude_from_searches_subnodes);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String nodeUniqueID = getArguments().getString("nodeUniqueID");
        String[] nodeProperties = ((MainView) getActivity()).getReader().getNodeProperties(nodeUniqueID);

        EditText editTextNodeName = view.findViewById(R.id.edit_text_node_name);
        editTextNodeName.setText(nodeProperties[0]);
        editTextNodeName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handle = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handle = true;
                    NodePropertiesFragment.this.updateNode(nodeUniqueID, nodeProperties[1]);
                    // Hides keyboard
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editTextNodeName.getWindowToken(), 0);
                }
                return handle;
            }
        });

        // Setting up node type radio buttons
        if (nodeProperties[1].equals("custom-colors")) {
            radioGroupNodeType.check(R.id.radio_button_rich_text);
        } else if (nodeProperties[1].equals("plain-text")) {
            radioGroupNodeType.check(R.id.radio_button_plain_text);
        } else {
            radioGroupNodeType.check(R.id.radio_button_automatic_syntax_highlighting);
        }

        // Setting up "Exclude from search This node"
        if (nodeProperties[2].equals("1")) {
            this.checkBoxExcludeFromSearchesThisNode.setChecked(true);
        }

        // Setting up "Exclude from search The subnodes"
        if (nodeProperties[3].equals("1")) {
            this.checkBoxExcludeFromSearchesSubnodes.setChecked(true);
        }

        // Display a message if user changes from rich-text to another node type
        // to notify that formatting will be lost
        this.radioGroupNodeType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (nodeProperties[1].equals("custom-colors") && checkedId != R.id.radio_button_rich_text) {
                    AlertDialog.Builder confirmNodeTypeChangeBuilder = new AlertDialog.Builder(getActivity());
                    confirmNodeTypeChangeBuilder.setTitle(R.string.dialog_node_type_change_title);
                    confirmNodeTypeChangeBuilder.setMessage(R.string.dialog_node_type_change_message);
                    confirmNodeTypeChangeBuilder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    confirmNodeTypeChangeBuilder.show();
                }
            }
        });

        Button buttonCancel = view.findViewById(R.id.add_node_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Back button press programmatically
                getActivity().onBackPressed();
            }
        });

        Button buttonCreate = view.findViewById(R.id.add_node_button_create);
        buttonCreate.setText(R.string.button_ok);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NodePropertiesFragment.this.updateNode(nodeUniqueID, nodeProperties[1]);
            }
        });
    }

    /**
     * Convenience function to call createNewNode in MainView
     * @param nodeUniqueID uniqueID of the node that new node will be created in relation with
     * @param progLangFromDatabase node's prog_lang property that was saved in the database when properties were opened
     */
    private void updateNode(String nodeUniqueID, String progLangFromDatabase) {
        String progLang = getNodeProgLangSelection();
        ((MainView) getActivity()).updateNodeProperties(getArguments().getInt("position"), nodeUniqueID, this.getNodeName(), progLang, getNoSearchMeState(), getNoSearchChState(), !progLangFromDatabase.equals(progLang));
    }

    /**
     * Returns value of user's selection of node type
     * that can be used in node creation
     * @return "custom-colors" - rich text, "plain-text' - plain text, "sh" - automatic_syntax_highlighting
     */
    private String getNodeProgLangSelection() {
        String progLang;
        int selectedRadioButtonID = radioGroupNodeType.getCheckedRadioButtonId();
        if (selectedRadioButtonID == R.id.radio_button_rich_text) {
            progLang = "custom-colors";
        } else if (selectedRadioButtonID == R.id.radio_button_plain_text) {
            progLang = "plain-text";
        } else {
            progLang = "sh";
        }
        return progLang;
    }

    /**
     * Returns state of the "Exclude from searches: This node" state
     * in string form that can be used in node creation
     * @return 0" - checkbox not checked, "1" - checkbox checked
     */
    private String getNoSearchMeState() {
        if (this.checkBoxExcludeFromSearchesThisNode.isChecked()) {
            return "1";
        } else {
            return "0";
        }
    }

    /**
     * Returns state of the "Exclude from searches: Subnodes" state
     * in string form that can be used in node creation
     * @return "0" - checkbox not checked, "1" - checkbox checked
     */
    private String getNoSearchChState() {
        if (this.checkBoxExcludeFromSearchesSubnodes.isChecked()) {
            return "1";
        } else {
            return "0";
        }
    }

    /**
     * Removes leading and trailing spaces
     * If name is empty then
     * returns a String containing a question mark (?)
     * @return node name that can be used for node creation
     */
    private String getNodeName() {
        String nodeName = ((EditText) getView().findViewById(R.id.edit_text_node_name)).getText().toString().trim();
        if (nodeName.length() == 0) {
            nodeName = "?";
        }
        return nodeName;
    }
}
