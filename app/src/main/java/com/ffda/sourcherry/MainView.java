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
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;

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
        setContentView(R.layout.activity_xmlview);

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
                    MainView.this.currentNodePosition = 0;
                } else {
                    MainView.this.currentNodePosition = position;
                }

                MainView.this.loadNodeContent();
            }
        });
        rvMenu.setAdapter(adapter);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

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
        this.adapter.notifyDataSetChanged();
    }

    public void goNodeUp(View view) {
        ArrayList<String[]> nodes = xmlReader.getParentWithSubnodes(this.nodes.get(0)[1]);
        if (nodes != null) {
//            Toast.makeText(this, this.currentNode[0], Toast.LENGTH_SHORT).show(); // Test line. Delete later
            this.currentNode = nodes.get(0);
            this.nodes.clear();
            this.nodes.addAll(nodes);
            this.adapter.markItemSelected(-1);
            this.adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadNodeContent() {
        LinearLayout mainLinearLayout = findViewById(R.id.mainLinearLayout);
        SpannableStringBuilder nodeContent = xmlReader.getNodeContent(this.currentNode[1]);
        mainLinearLayout.removeAllViews();

        this.adapter.markItemSelected(currentNodePosition);
        this.adapter.notifyDataSetChanged();

        TextView tv = new TextView(this);
        tv.setTextIsSelectable(true);
        tv.setText(nodeContent, TextView.BufferType.EDITABLE);
        mainLinearLayout.addView(tv);
    }
}