package com.ffda.sourcherry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainView extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private InputStream is;
    private ArrayList<String[]> nodes;
    private XMLReader xmlReader;
    private MenuItemAdapter adapter;
    private String[] currentNode;
    private int currentNodePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
        String databaseString = sharedPref.getString("databaseUri", null);
        getContentResolver().takePersistableUriPermission(Uri.parse(databaseString), Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            this.is = getContentResolver().openInputStream(Uri.parse(databaseString));
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.xmlReader = new XMLReader(this.is, this);
        this.nodes = xmlReader.getMainNodes();
        this.currentNode = null;

        RecyclerView rvMenu = (RecyclerView) findViewById(R.id.recyclerView);
        this.adapter = new MenuItemAdapter(this.nodes, this);
        adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                MainView.this.currentNode = nodes.get(position);
                if (nodes.get(position)[2].equals("true")) { // Checks if node is marked to have subnodes
                    MainView.this.openSubmenu();
                } else {
                    MainView.this.currentNodePosition = position;
                }
                MainView.this.resetSearchView();
                MainView.this.loadNodeContent();
            }
        });
        rvMenu.setAdapter(adapter);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        SearchView searchView = findViewById(R.id.navigation_drawer_search);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                MainView.this.filterNodes(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if (MainView.this.currentNode[2].equals("true")) { // Checks if node is marked to have subnodes
                    MainView.this.openSubmenu();
                } else {
                    MainView.this.resetMenuToCurrentNode();
                }
                return false;
            }
        });

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openSubmenu() {
        // Clears existing menu and recreate with submenu of the currentNode
        this.nodes.clear();
        this.nodes.addAll(this.xmlReader.getSubnodes(this.currentNode[1]));
        MainView.this.currentNodePosition = 0;
        this.adapter.notifyDataSetChanged();
    }

    public void goNodeUp(View view) {
        ArrayList<String[]> nodes = xmlReader.getParentWithSubnodes(this.nodes.get(0)[1]);
        if (nodes != null) {
            this.currentNode = nodes.get(0);
            this.nodes.clear();
            this.nodes.addAll(nodes);
            this.currentNodePosition = -1;
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        }
    }

    public void goHome(View view) {
        // Reloads drawer menu to show main menu
        this.nodes.clear();
        this.nodes.addAll(xmlReader.getMainNodes());
        this.currentNodePosition = -1;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
    }

    public void loadNodeContent() {
        LinearLayout mainLinearLayout = findViewById(R.id.mainLinearLayout);
        SpannableStringBuilder nodeContent = xmlReader.getNodeContent(this.currentNode[1]);
        mainLinearLayout.removeAllViews();

        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();

        TextView tv = new TextView(this);
        tv.setTextSize(16);
        tv.setTextIsSelectable(true);
        tv.setText(nodeContent, TextView.BufferType.EDITABLE);
        mainLinearLayout.addView(tv);
    }

    public void filterNodes(String query) {
        // Filters node list by the name of the node
        // Changes the node list that represents menu and updates it
        // Case insensitive

        this.nodes.clear();
        this.adapter.markItemSelected(-1);
        this.nodes.addAll(xmlReader.getAllNodes());

        ArrayList<String[]> filteredNodes = this.nodes.stream()
                .filter(node -> node[0].toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));

        this.nodes.clear();
        this.nodes.addAll(filteredNodes);
        this.adapter.notifyDataSetChanged();
    }

    public void resetMenuToCurrentNode() {
        // Restores drawer menu to current selection after user search
        // when no node was selected
        this.nodes.clear();
        this.nodes.addAll(MainView.this.xmlReader.getParentWithSubnodes(MainView.this.currentNode[1]));

        for (int index = 0; index < this.nodes.size(); index++) {
            if (this.nodes.get(index)[1].equals(this.currentNode[1])) {
                this.currentNodePosition = index;
                this.adapter.markItemSelected(this.currentNodePosition);
            }
        }

        this.adapter.notifyDataSetChanged();
    }

    public void resetSearchView() {
        // Deflates search field
        SearchView searchView = (SearchView) findViewById(R.id.navigation_drawer_search);
        if (!searchView.isIconified()) {
            searchView.setQuery("", false);
            searchView.clearFocus();
            searchView.setIconified(true);
        }

    }
}