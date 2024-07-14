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

public class NodeContentEditorMenuTableFragment extends Fragment {
    private NodeContentEditorTableMenuActions nodeContentEditorTableMenuActions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.edit_node_fragment_button_row_table_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton insertCol = view.findViewById(R.id.edit_node_fragment_table_button_row_insert_col);
        insertCol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodeContentEditorTableMenuActions.insertColumn();
            }
        });
        ImageButton insertRow = view.findViewById(R.id.edit_node_fragment_table_button_row_insert_row);
        insertRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodeContentEditorTableMenuActions.insertRow();
            }
        });
        ImageButton deleteCol = view.findViewById(R.id.edit_node_fragment_table_button_row_delete_col);
        deleteCol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nodeContentEditorTableMenuActions.deleteColumn();
            }
        });
    }

    /**
     * Set the instance of the parent fragment that implements NodeContentEditorTableMenuActions
     * to allow editing of tables
     * @param nodeContentEditorTableMenuActions instance of NodeContentEditorTableMenuActions
     */
    public void setNodeContentEditorTableMenuActions(NodeContentEditorTableMenuActions nodeContentEditorTableMenuActions) {
        this.nodeContentEditorTableMenuActions = nodeContentEditorTableMenuActions;
    }
}
