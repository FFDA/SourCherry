package com.ffda.sourcherry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView menuItem;

        public ViewHolder(View itemView) {
            super(itemView);

            menuItem = (TextView) itemView.findViewById(R.id.menu_item);

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

    // nodeList has values in this order: {name, unique_id, has_subnodes, is_parent}
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
    public void onBindViewHolder(MenuItemAdapter.ViewHolder holder, int position) {
        String nodeName = nodeList.get(position)[0];

        TextView menuItem = holder.menuItem;
        menuItem.setText(nodeName);
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }
}
