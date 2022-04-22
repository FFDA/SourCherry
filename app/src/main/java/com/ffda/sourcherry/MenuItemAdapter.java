package com.ffda.sourcherry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MenuItemAdapter extends RecyclerView.Adapter<MenuItemAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView menuItem;

        public ViewHolder(View itemView) {
            super(itemView);

            menuItem = (TextView) itemView.findViewById(R.id.menu_item);
        }
    }

    private ArrayList<String> nodeList;

    public MenuItemAdapter(ArrayList<String> nodeList) {
        this.nodeList = nodeList;
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
        String node = nodeList.get(position);

        TextView menuItem = holder.menuItem;
        menuItem.setText(node);
    }

    @Override
    public int getItemCount() {
        return nodeList.size();
    }
}
