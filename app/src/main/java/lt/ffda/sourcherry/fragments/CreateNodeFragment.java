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

public class CreateNodeFragment extends Fragment {
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
        this.radioGroupNodeType.check(R.id.radio_button_rich_text);
        String nodeUniqueID = getArguments().getString("nodeUniqueID");
        int relation = getArguments().getInt("relation");

        EditText editTextNodeName = view.findViewById(R.id.edit_text_node_name);
        editTextNodeName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handle = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handle = true;
                    CreateNodeFragment.this.createNode(nodeUniqueID, relation, CreateNodeFragment.this.validateNodeName(editTextNodeName.getText().toString()));
                    // Hides keyboard
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editTextNodeName.getWindowToken(), 0);
                }
                return handle;
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
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateNodeFragment.this.createNode(nodeUniqueID, relation, validateNodeName(editTextNodeName.getText().toString()));
            }
        });
    }

    /**
     * Convenience function to call createNewNode in MainView
     * @param nodeUniqueID unique ID of the node that new node will be created in relation with
     * @param relation relation to the node. 0 - sibling, 1 - subnode
     * @param name node name
     */
    private void createNode(String nodeUniqueID, int relation, String name) {
        ((MainView) getActivity()).createNewNode(nodeUniqueID, relation, name, getNodeProgLangSelection(), getNoSearchMeState(), getNoSearchChState());
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
     * @param nodeName node name
     * @return node name that can be used for node creation
     */
    private String validateNodeName(String nodeName) {
        nodeName = nodeName.trim();
        if (nodeName.length() == 0) {
            nodeName = "?";
        }
        return nodeName;
    }
}
