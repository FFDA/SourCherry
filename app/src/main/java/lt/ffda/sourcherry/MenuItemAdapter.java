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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView menuItemPadding; // This item is only needed to make menu items look indented
        public ImageView menuItemArrow;
        public TextView menuItemText;
        public LinearLayout menuItemLinearLayout;
        public ImageView menuItemAction;

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

    // nodeList has values in this order: {name, unique_id, has_subnodes, is_parent, is_subnode}
    private final ArrayList<String[]> nodeList;
    private OnItemClickListener listener;
    private OnLongClickListener longClickListener;
    private OnActionIconClickListener onActionIconClickListener;
    private int selectedPos = RecyclerView.NO_POSITION;
    private final Context context;

    public MenuItemAdapter(ArrayList<String[]> nodeList, Context context) {
        this.nodeList = nodeList;
        this.context = context;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    public interface OnLongClickListener {
        void onLongClick(View itemView, int position);
    }

    public interface OnActionIconClickListener {
        void onActionIconClick(View itemView, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setOnItemActionMenuClickListener(OnActionIconClickListener listener) {
        this.onActionIconClickListener = listener;
    }

    @NonNull
    @Override
    public MenuItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View menuView = inflater.inflate(R.layout.menu_item, parent, false);

        return new ViewHolder(menuView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String nodeName = nodeList.get(position)[0];
        String nodeHasSubnodes = nodeList.get(position)[2];
        String nodeIsParent = nodeList.get(position)[3];
        String nodeIsSubnode = nodeList.get(position)[4];

        // Setting selected items background color
        holder.itemView.setBackgroundColor(selectedPos == position ? this.context.getResources().getColor(R.color.cherry_red_500, this.context.getTheme()) : Color.TRANSPARENT);

        ImageView menuItemPadding = holder.menuItemPadding;
        ImageView menuItemArrow = holder.menuItemArrow;
        TextView menuItemText = holder.menuItemText;

        // Making visible/invisible an arrow that indicates that node has subnodes
        if (nodeHasSubnodes.equals("true")) {
            menuItemArrow.setVisibility(View.VISIBLE);
            menuItemArrow.setImageResource(R.drawable.ic_baseline_arrow_has_subnodes_24);
        } else {
            menuItemArrow.setVisibility(View.INVISIBLE);
        }

        // Adding arrow that make menu item took differently to indicate that this node is a parent (top) node
        if (nodeIsParent.equals("true")) {
            menuItemArrow.setVisibility(View.VISIBLE);
//            menuItemArrow.setImageDrawable((ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_baseline_arrow_is_parent_24))); // Leaving this as a reminder
            menuItemArrow.setImageResource(R.drawable.ic_baseline_arrow_is_parent_24);
        }

        // If node is a subnode - adds small ImageView item to make it look indented.
        if (nodeIsSubnode.equals("true")) {
            menuItemPadding.setVisibility(View.INVISIBLE);
        } else {
            menuItemPadding.setVisibility(View.GONE);
        }

        menuItemText.setText(nodeName);
    }

    public void markItemSelected(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }
}
