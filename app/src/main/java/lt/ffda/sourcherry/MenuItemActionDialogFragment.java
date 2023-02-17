/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import lt.ffda.sourcherry.utils.MenuItemAction;

public class MenuItemActionDialogFragment extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.menu_item_action_dialog_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button buttonAddSiblingNode = view.findViewById(R.id.menu_item_action_menu_add_node);
        buttonAddSiblingNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItemActionDialogFragment.this.sendResult(MenuItemAction.ADD_SIBLING_NODE, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"));
            }
        });

        Button buttonAddSubnode = view.findViewById(R.id.menu_item_action_menu_add_subnode);
        buttonAddSubnode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItemActionDialogFragment.this.sendResult(MenuItemAction.ADD_SUBNODE, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"));
            }
        });

        // Depending if node is bookmarked or not showing just one menu item
        // and adding click listener just for it
        if (getArguments().getBoolean("bookmarked")) {
            Button buttonRemoveFromBookmarks = view.findViewById(R.id.menu_item_action_menu_remove_from_bookmarks);
            buttonRemoveFromBookmarks.setVisibility(View.VISIBLE);
            buttonRemoveFromBookmarks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MenuItemActionDialogFragment.this.sendResult(MenuItemAction.REMOVE_FROM_BOOKMARKS, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"), MenuItemActionDialogFragment.this.getArguments().getInt("position"));
                }
            });
        } else {
            Button buttonAddToBookmarks = view.findViewById(R.id.menu_item_action_menu_add_to_bookmarks);
            buttonAddToBookmarks.setVisibility(View.VISIBLE);
            buttonAddToBookmarks.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MenuItemActionDialogFragment.this.sendResult(MenuItemAction.ADD_TO_BOOKMARKS, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"));
                }
            });
        }

        Button buttonDeleteNode = view.findViewById(R.id.menu_item_action_menu_delete_node);
        buttonDeleteNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItemActionDialogFragment.this.sendResult(MenuItemAction.DELETE_NODE, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"));
            }
        });

        Button buttonProperties = view.findViewById(R.id.menu_item_action_menu_properties);
        buttonProperties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuItemActionDialogFragment.this.sendResult(MenuItemAction.PROPERTIES, MenuItemActionDialogFragment.this.getArguments().getStringArray("node"));
            }
        });

        String[] node = getArguments().getStringArray("node");
        getDialog().setTitle(node[0]);
    }

    /**
     * Sends option (result) that user chose back to MainView
     * @param menuItemAction action code that user chose
     * @param node node menu item information on which action was initiated
     */
    private void sendResult(MenuItemAction menuItemAction, String[] node) {
        Bundle result = new Bundle();
        result.putSerializable("menuItemActionCode", menuItemAction);
        result.putStringArray("node", node);
        getParentFragmentManager().setFragmentResult("menuItemAction", result);
        dismiss();
    }

    /**
     * Sends option (result) that user chose back to MainView
     * @param menuItemAction action code that user chose
     * @param node node menu item information on which action was initiated
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void sendResult(MenuItemAction menuItemAction, String[] node, int position) {
        Bundle result = new Bundle();
        result.putSerializable("menuItemActionCode", menuItemAction);
        result.putStringArray("node", node);
        result.putInt("position", position);
        getParentFragmentManager().setFragmentResult("menuItemAction", result);
        dismiss();
    }
}
