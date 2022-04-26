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
import android.view.MenuItem;
import android.view.View;

import java.io.InputStream;
import java.util.ArrayList;

public class XMLView extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private InputStream is;
    private ArrayList<String[]> nodes;
    private XMLReader xmlReader;

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

        this.xmlReader = new XMLReader(this.is);
        this.nodes = xmlReader.getMainNodes();

        RecyclerView rvMenu = (RecyclerView) findViewById(R.id.recyclerView);
        MenuItemAdapter adapter = new MenuItemAdapter(this.nodes);
        adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                String nodeName = nodes.get(position)[0];
                String nodeUniqueID = nodes.get(position)[1];
                String nodeHasSudnodes = nodes.get(position)[2];
                String nodeIsParent = nodes.get(position)[3];
                if (nodeHasSudnodes.equals("true")) {
                    XMLView.this.updateMenu(adapter, nodeName);
                }

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

    public void updateMenu(MenuItemAdapter adapter, String nodeName) {
        this.nodes.clear();
        this.nodes.addAll(this.xmlReader.getSubNodes(nodeName));
        adapter.notifyDataSetChanged();

    }
}