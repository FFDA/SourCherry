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

import lt.ffda.sourcherry.model.ScNode;

public class MoveNodeFragmentItemAdapter extends RecyclerView.Adapter<MoveNodeFragmentItemAdapter.ViewHolder> {
    private ArrayList<ScNode> nodeList;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private OnLongClickListener onLongClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView menuItemPadding; // This item is only needed to make menu items look indented
        public ImageView menuItemArrow;
        public TextView menuItemText;
        public LinearLayout menuItemLinearLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.menuItemPadding = itemView.findViewById(R.id.menu_item_padding);
            this.menuItemArrow = itemView.findViewById(R.id.menu_item_arrow);
            this.menuItemText = itemView.findViewById(R.id.menu_item_name);
            this.menuItemLinearLayout = itemView.findViewById(R.id.menu_linear_layout);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(view, position);
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onLongClickListener.onLongClick(view, position);
                    }
                    return true;
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    public interface OnLongClickListener {
        void onLongClick(View itemView, int position);
    }

    public void setOnItemClickListener(MoveNodeFragmentItemAdapter.OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnLongClickListener(MoveNodeFragmentItemAdapter.OnLongClickListener longClickListener) {
        this.onLongClickListener = longClickListener;
    }

    public MoveNodeFragmentItemAdapter(ArrayList<ScNode> nodeList) {
        this.nodeList = nodeList;
    }

    @NonNull
    @Override
    public MoveNodeFragmentItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_move_node_fragment, parent, false);
        this.context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoveNodeFragmentItemAdapter.ViewHolder holder, int position) {
        String nodeName = nodeList.get(position).getName();
        boolean nodeHasSubnodes = nodeList.get(position).hasSubnodes();
        boolean nodeIsParent = nodeList.get(position).isParent();
        boolean nodeIsSubnode = nodeList.get(position).isSubnode();

        // Setting selected items background color
        holder.itemView.setBackgroundColor(selectedPosition == position ? this.context.getResources().getColor(R.color.cherry_red_500, this.context.getTheme()) : Color.TRANSPARENT);

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

        menuItemText.setText(nodeName);
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }

    public void markItemSelected(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }
}
