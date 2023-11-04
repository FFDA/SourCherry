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

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import lt.ffda.sourcherry.model.ScNode;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    private final Context context;
    // nodeList has values in this order: {name, unique_id, has_subnodes, is_parent, is_subnode}
    private final ArrayList<ScNode> nodeList;
    private final int textColorSecondary;
    private OnItemClickListener listener;
    private OnLongClickListener longClickListener;
    private OnActionIconClickListener onActionIconClickListener;
    private int selectedPos = RecyclerView.NO_POSITION;
    public MenuItemAdapter(ArrayList<ScNode> nodeList, Context context) {
        this.nodeList = nodeList;
        this.context = context;
        TypedValue typedValue = new TypedValue();
        this.context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        textColorSecondary = ContextCompat.getColor(this.context, typedValue.resourceId);
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }

    public void markItemSelected(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String nodeName = nodeList.get(position).getName();
        boolean nodeIsParent = nodeList.get(position).isParent();
        boolean nodeHasSubnodes = nodeList.get(position).hasSubnodes();
        boolean nodeIsSubnode = nodeList.get(position).isSubnode();

        // Setting selected items background color
        holder.itemView.setBackgroundColor(selectedPos == position ? this.context.getResources().getColor(R.color.cherry_red_500, this.context.getTheme()) : Color.TRANSPARENT);

        ImageView menuItemPadding = holder.menuItemPadding;
        ImageView menuItemArrow = holder.menuItemArrow;
        TextView menuItemText = holder.menuItemText;

        // Making visible/invisible an arrow that indicates that node has subnodes
        if (nodeHasSubnodes) {
            menuItemArrow.setVisibility(View.VISIBLE);
            menuItemArrow.setImageResource(R.drawable.ic_baseline_arrow_has_subnodes_24);
        } else {
            menuItemArrow.setVisibility(View.INVISIBLE);
        }

        // Adding arrow that make menu item took differently to indicate that this node is a parent (top) node
        if (nodeIsParent) {
            menuItemArrow.setVisibility(View.VISIBLE);
//            menuItemArrow.setImageDrawable((ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_baseline_arrow_is_parent_24))); // Leaving this as a reminder
            menuItemArrow.setImageResource(R.drawable.ic_baseline_arrow_is_parent_24);
        }

        // If node is a subnode - adds small ImageView item to make it look indented.
        if (nodeIsSubnode) {
            menuItemPadding.setVisibility(View.INVISIBLE);
        } else {
            menuItemPadding.setVisibility(View.GONE);
        }

        if (!nodeList.get(position).getForegroundColor().equals("")) {
            menuItemText.setTextColor(Color.parseColor(nodeList.get(position).getForegroundColor()));
        } else {
            menuItemText.setTextColor(textColorSecondary);
        }

        menuItemText.setText(nodeName);
    }

    @NonNull
    @Override
    public MenuItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View menuView = inflater.inflate(R.layout.item_drawer_menu, parent, false);
        return new ViewHolder(menuView);
    }

    public void setOnItemActionMenuClickListener(OnActionIconClickListener listener) {
        this.onActionIconClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public interface OnActionIconClickListener {
        void onActionIconClick(View itemView, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    public interface OnLongClickListener {
        void onLongClick(View itemView, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView menuItemAction;
        public ImageView menuItemArrow;
        public LinearLayout menuItemLinearLayout;
        public ImageView menuItemPadding; // This item is only needed to make menu items look indented
        public TextView menuItemText;

        public ViewHolder(View itemView) {
            super(itemView);

            menuItemPadding = itemView.findViewById(R.id.menu_item_padding);
            menuItemArrow = itemView.findViewById(R.id.menu_item_arrow);
            menuItemText = itemView.findViewById(R.id.menu_item_name);
            menuItemLinearLayout = itemView.findViewById(R.id.menu_linear_layout);
            menuItemAction = itemView.findViewById(R.id.menu_item_action);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View itemView) {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(itemView, position);
                        }
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            longClickListener.onLongClick(itemView, position);
                            return true;
                        }
                    }
                    return false;
                }
            });

            menuItemAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getBindingAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onActionIconClickListener.onActionIconClick(itemView, position);
                        }
                    }
                }
            });
        }

        @Override
        public void onClick(View itemView) {

        }
    }
}
