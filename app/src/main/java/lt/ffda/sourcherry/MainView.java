/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import lt.ffda.sourcherry.R;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainView extends AppCompatActivity {

    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private InputStream is;
    private ArrayList<String[]> nodes;
    private DatabaseReader reader;
    private MenuItemAdapter adapter;
    private String[] currentNode;
    private int currentNodePosition; // In menu / MenuItemAdapter for marking menu item opened/selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.main_view_fragment, NodeContentFragment.class, null, "main")
                    .commit();
        }

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
        String databaseString = sharedPref.getString("databaseUri", null);

        try {
            if (sharedPref.getString("databaseStorageType", null).equals("shared")) {
                // If file is in external storage
                getContentResolver().takePersistableUriPermission(Uri.parse(databaseString), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (sharedPref.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    this.is = getContentResolver().openInputStream(Uri.parse(databaseString));
                    this.reader = new XMLReader(this.is, this, getSupportFragmentManager());
                }
            } else {
                // If file is in internal app storage
                if (sharedPref.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    this.is = new FileInputStream(sharedPref.getString("databaseUri", null));
                    this.reader = new XMLReader(this.is, this, getSupportFragmentManager());
                } else {
                    // If file is sql (password protected or not)
                    SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READONLY);
                    this.reader = new SQLReader(sqlite, this, getSupportFragmentManager());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.nodes = reader.getMainNodes();
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
                return false;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                if (MainView.this.currentNode != null) {
                    if (MainView.this.currentNode[2].equals("true")) { // Checks if node is marked to have subnodes
                        MainView.this.openSubmenu();
                    } else {
                        MainView.this.resetMenuToCurrentNode();
                    }
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
        this.nodes.addAll(this.reader.getSubnodes(this.currentNode[1]));
        this.currentNodePosition = 0;
        this.adapter.notifyDataSetChanged();
    }

    public void goNodeUp(View view) {
        ArrayList<String[]> nodes = this.reader.getParentWithSubnodes(this.nodes.get(0)[1]);
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
        this.nodes.addAll(this.reader.getMainNodes());
        this.currentNodePosition = -1;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
    }

    public void loadNodeContent() {

        FragmentManager fragmentManager = getSupportFragmentManager();

        // Checks if there is more than 0 fragment in backStack and removes it if there is
        // because right now there is only one possible other fragment in backStack - image
        // it's not needed if use want's to see another node.
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        }

        // Gets instance of the fragment
        NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
        // Sends ArrayList to fragment to be added added to view
        nodeContentFragment.setNodeContent(this.reader.getNodeContent(this.currentNode[1]));

        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(this.currentNode[0]);

        nodeContentFragment.loadContent();
    }

    public void filterNodes(String query) {
        // Filters node list by the name of the node
        // Changes the node list that represents menu and updates it
        // Case insensitive

        //// This would fix the issue that shows all nodes in menu after screen rotation, but not in all instances
//        SearchView searchView = (SearchView) findViewById(R.id.navigation_drawer_search);
//        if (!searchView.isIconified()) {
        ////
        this.nodes.clear();
        this.adapter.markItemSelected(-1);
        this.nodes.addAll(this.reader.getAllNodes());

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

        if (this.currentNode != null) {
            this.nodes.clear();
            this.nodes.addAll(this.reader.getParentWithSubnodes(this.currentNode[1]));

            for (int index = 0; index < this.nodes.size(); index++) {
                if (this.nodes.get(index)[1].equals(this.currentNode[1])) {
                    this.currentNodePosition = index;
                    this.adapter.markItemSelected(this.currentNodePosition);
                }
            }

            this.adapter.notifyDataSetChanged();
        }
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

    public DatabaseReader reader() {
        return this.reader;
    }

    public String getCurrentNodeUniqueID() {
        return this.currentNode[1];
    }

    public void openAnchorLink(String[] nodeArray) {
        this.currentNode = nodeArray;
        this.nodes.clear();
        if (this.currentNode[2].equals("true")) { // Checks if node is marked to have subnodes
            this.openSubmenu();
        } else {
            this.resetMenuToCurrentNode();
        }
        this.loadNodeContent();
    }
}