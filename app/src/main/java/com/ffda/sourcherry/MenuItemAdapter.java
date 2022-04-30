package com.ffda.sourcherry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView menuItemPadding; // This item is only needed to make menu items look indented
        public ImageView menuItemArrow;
        public TextView menuItemText;

        public ViewHolder(View itemView) {
            super(itemView);

            menuItemPadding = (ImageView) itemView.findViewById(R.id.menu_item_padding);
            menuItemArrow = (ImageView) itemView.findViewById(R.id.menu_item_arrow);
            menuItemText = (TextView) itemView.findViewById(R.id.menu_item_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View itemView) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(itemView, position);
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
    private ArrayList<String[]> nodeList;
    private OnItemClickListener listener;

    public MenuItemAdapter(ArrayList<String[]> nodeList) {
        this.nodeList = nodeList;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public MenuItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View menuView = inflater.inflate(R.layout.menu_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(menuView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String nodeName = nodeList.get(position)[0];
        String nodeUniqueID = nodeList.get(position)[1];
        String nodeHasSubnodes = nodeList.get(position)[2];
        String nodeIsParent = nodeList.get(position)[3];
        String nodeIsSubnode = nodeList.get(position)[4];

        ImageView menuItemPaddig = holder.menuItemPadding;
        ImageView menuItemArrow = holder.menuItemArrow;
        TextView menuItemText = holder.menuItemText;

        // Making visible/invisible an arrow that indicates that node has subnodes
        if (nodeHasSubnodes.equals("true")) {
            menuItemArrow.setVisibility(View.VISIBLE);
        } else {
            menuItemArrow.setVisibility(View.INVISIBLE);
        }

        // Adding arrow that make menu item took differently to indicate that this node is a parent (top) node
        if (nodeIsParent.equals("true")) {
//            menuItemArrow.setImageDrawable((ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_baseline_arrow_is_parent_24))); //Pasilieku kaip priminimą
            menuItemArrow.setImageResource(R.drawable.ic_baseline_arrow_is_parent_24);
        }

        // If node is a subnode - adds small ImageView item to make it look indented.
        if (nodeIsSubnode.equals("true")) {
            menuItemPaddig.setVisibility(View.INVISIBLE);
        } else {
            menuItemPaddig.setVisibility(View.GONE);
        }

        menuItemText.setText(nodeName);

    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }
}
