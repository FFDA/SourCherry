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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lt.ffda.sourcherry.R;

public class NodeContentEditorMenuListsFragment extends Fragment {
    private NodeContentEditorListsMenuActions nodeContentEditorListsMenuActions;
    private NodeContentEditorMenuBackAction nodeContentEditorMenuBackAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.edit_node_fragment_button_row_lists_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton backButton = view.findViewById(R.id.edit_node_fragment_button_row_lists_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorMenuBackAction.back();
                }
            }
        });
        ImageButton startChecklists = view.findViewById(R.id.edit_node_fragment_button_row_lists_start_checklist);
        startChecklists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorListsMenuActions.startChecklist();
                }
            }
        });
        ImageButton startUnordered = view.findViewById(R.id.edit_node_fragment_button_row_lists_start_unordered);
        startUnordered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorListsMenuActions.startUnordered();
                }
            }
        });
        ImageButton startOrdered = view.findViewById(R.id.edit_node_fragment_button_row_lists_start_ordered);
        startOrdered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorListsMenuActions.startOrdered();
                }
            }
        });
        ImageButton decreaseIndentation = view.findViewById(R.id.edit_node_fragment_button_row_lists_indent_decrease);
        decreaseIndentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorListsMenuActions.decreaseListItemIndentation();
                }
            }
        });
        ImageButton increaseIndentation = view.findViewById(R.id.edit_node_fragment_button_row_lists_indent_increase);
        increaseIndentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nodeContentEditorMenuBackAction != null) {
                    nodeContentEditorListsMenuActions.increaseListItemIndentation();
                }
            }
        });
    }

    /**
     * Set an instance of the parent fragment that implements NodeContentEditorInsertMenuActions to
     * be able to manipulate it's content
     * @param nodeContentEditorInsertMenuActions instance of fragment that implelents NodeContentEditorInsertMenuActions
     */
    public void setNodeContentEditorInsertMenuActions(NodeContentEditorListsMenuActions nodeContentEditorListsMenuActions,
                                                      NodeContentEditorMenuBackAction nodeContentEditorMenuBackAction) {
        this.nodeContentEditorListsMenuActions = nodeContentEditorListsMenuActions;
        this.nodeContentEditorMenuBackAction = nodeContentEditorMenuBackAction;
    }
}
