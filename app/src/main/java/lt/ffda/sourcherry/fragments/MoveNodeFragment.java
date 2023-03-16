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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import lt.ffda.sourcherry.DatabaseReader;
import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MoveNodeFragmentItemAdapter;
import lt.ffda.sourcherry.R;

public class MoveNodeFragment extends Fragment {
    private DatabaseReader reader;
    private ArrayList<String[]> nodeList;
    private MoveNodeFragmentItemAdapter adapter;
    private int currentPosition; // currently marked node

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return getLayoutInflater().inflate(R.layout.move_node_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.reader = ((MainView) getActivity()).getReader();
        this.nodeList = reader.getMainNodes();
        this.adapter = new MoveNodeFragmentItemAdapter(nodeList);

        TextView moveNodeTextviewTitle = view.findViewById(R.id.move_node_textview_title);
        moveNodeTextviewTitle.setText(getString(R.string.move_node_textview_title, getArguments().getStringArray("node")[0]));

        RecyclerView recyclerView = view.findViewById(R.id.move_node_fragment_recycle_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter.setOnItemClickListener(new MoveNodeFragmentItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (nodeList.get(position)[2].equals("true")) {
                    MoveNodeFragment.this.setNodes(reader.getSubnodes(nodeList.get(position)[1]));
                }
            }
        });

        adapter.setOnLongClickListener(new MoveNodeFragmentItemAdapter.OnLongClickListener() {
            @Override
            public void onLongClick(View itemView, int position) {
                MoveNodeFragment.this.adapter.markItemSelected(position);
                MoveNodeFragment.this.adapter.notifyItemChanged(currentPosition);
                MoveNodeFragment.this.currentPosition = position;
                MoveNodeFragment.this.adapter.notifyItemChanged(position);
            }
        });

        Button buttonMoveToMainMenu = view.findViewById(R.id.move_node_button_move_to_main_menu);
        buttonMoveToMainMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveNodeFragment.this.moveNode("0");
            }
        });

        ImageButton buttonUp = view.findViewById(R.id.move_node_button_up);
        buttonUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveNodeFragment.this.goNodeUp(MoveNodeFragment.this.nodeList.get(0)[1]);
            }
        });

        ImageButton buttonHome = view.findViewById(R.id.move_node_button_home);
        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoveNodeFragment.this.goHome();
            }
        });

        Button buttonCancel = view.findViewById(R.id.move_node_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        Button buttonMove = view.findViewById(R.id.move_node_button_move);
        buttonMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MoveNodeFragment.this.currentPosition != RecyclerView.NO_POSITION) {
                    MoveNodeFragment.this.moveNode(nodeList.get(currentPosition)[1]);
                }
            }
        });
    }

    /**
     * Changes navagiation menu items with the one that are provided through the argument
     * Removes selection of the node that's currently selected for the move
     * @param nodes node list to load to the navigation menu
     */
    private void setNodes(ArrayList<String[]> nodes) {
        this.nodeList.clear();
        this.nodeList.addAll(nodes);
        this.currentPosition = RecyclerView.NO_POSITION;
        this.adapter.markItemSelected(this.currentPosition);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * Initiates move of the node
     * @param destinationNodeUniqueID unique ID of the node that user wants to make a new parent of the select node
     */
    private void moveNode(String destinationNodeUniqueID) {
        ((MainView) getActivity()).moveNode(getArguments().getStringArray("node")[1], destinationNodeUniqueID);
    }

    /**
     * Moves navigation menu one node up
     * If menu is already at the top it shows a message to the user
     * @param nodeUniqueID unique node ID if the node which is currently at the top (parent node)
     */
    private void goNodeUp(String nodeUniqueID) {
        ArrayList<String[]> nodes = this.reader.getParentWithSubnodes(nodeUniqueID);
        if (nodes != null && nodes.size() != this.nodeList.size()) {
            // If retrieved nodes are not null and array size do not match the one displayed
            // it is definitely not the same node so it can go up
            this.setNodes(nodes);
        } else {
            // If both node arrays matches in size it might be the same node (especially main/top)
            // This part checks if first and last nodes in arrays matches by comparing uniqueID of both
            if (nodes.get(0)[1].equals(this.nodeList.get(0)[1]) && nodes.get(nodes.size() -1 )[1].equals(this.nodeList.get(this.nodeList.size() -1 )[1])) {
                Toast.makeText(getContext(), "Your are at the top", Toast.LENGTH_SHORT).show();
            } else {
                this.setNodes(nodes);
            }
        }
    }

    /**
     * Restores selection of where to move node navigation to the top
     * Displays a message if navigation is already at the top and does not reload the menu
     */
    private void goHome() {
        ArrayList<String[]> tempHomeNodes = this.reader.getMainNodes();
        // Compares node sizes, first and last node's uniqueIDs in both arrays
        if (tempHomeNodes.size() == this.nodeList.size() && tempHomeNodes.get(0)[1].equals(this.nodeList.get(0)[1]) && tempHomeNodes.get(this.nodeList.size() -1 )[1].equals(this.nodeList.get(this.nodeList.size() -1 )[1])) {
            Toast.makeText(getContext(), "Your are at the top", Toast.LENGTH_SHORT).show();
        } else {
            this.setNodes(tempHomeNodes);
        }
    }
}
