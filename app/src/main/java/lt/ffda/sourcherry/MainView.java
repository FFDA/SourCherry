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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MainView extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private DatabaseReader reader;
    private MenuItemAdapter adapter;
    private String[] currentNode;
    private int currentNodePosition; // In menu / MenuItemAdapter for marking menu item opened/selected
    private boolean bookmarksToggle; // To save state for bookmarks. True means bookmarks are being displayed
    private int tempCurrentNodePosition; // Needed to save selected node position when user opens bookmarks;
    private boolean backToExit;
    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        this.currentNode = null; // This needs to be placed before restoring the instance if there was one
        this.bookmarksToggle = false;
        this.currentNodePosition = -1;
        this.backToExit = false;

        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        SearchView searchView = findViewById(R.id.navigation_drawer_search);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.com_ffda_SourCherry_PREFERENCE_FILE_KEY), Context.MODE_PRIVATE);
        String databaseString = sharedPref.getString("databaseUri", null);

        try {
            if (sharedPref.getString("databaseStorageType", null).equals("shared")) {
                // If file is in external storage
                getContentResolver().takePersistableUriPermission(Uri.parse(databaseString), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (sharedPref.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = getContentResolver().openInputStream(Uri.parse(databaseString));
                    this.reader = new XMLReader(is, this, getSupportFragmentManager());
                    is.close();
                }
            } else {
                // If file is in internal app storage
                if (sharedPref.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = new FileInputStream(sharedPref.getString("databaseUri", null));
                    this.reader = new XMLReader(is, this, getSupportFragmentManager());
                    is.close();
                } else {
                    // If file is sql (password protected or not)
                    SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READONLY);
                    this.reader = new SQLReader(sqlite, this, getSupportFragmentManager());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true).setReorderingAllowed(true)
                    .add(R.id.main_view_fragment, NodeContentFragment.class, null, "main")
                    .addToBackStack("main")
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            this.mainViewModel.setNodes(this.reader.getMainNodes());
        } else {
            // Restoring some variable to make it possible restore content fragment after the screen rotation
            this.currentNodePosition = savedInstanceState.getInt("currentNodePosition");
            this.tempCurrentNodePosition = savedInstanceState.getInt("tempCurrentNodePosition");
            this.currentNode = savedInstanceState.getStringArray("currentNode");
            this.bookmarksToggle = savedInstanceState.getBoolean("bookmarksToggle");
        }

        RecyclerView rvMenu = (RecyclerView) findViewById(R.id.recyclerView);
        this.adapter = new MenuItemAdapter(this.mainViewModel.getNodes(), this);
        this.adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                MainView.this.currentNode = MainView.this.mainViewModel.getNodes().get(position);
                if (MainView.this.mainViewModel.getNodes().get(position)[2].equals("true")) { // Checks if node is marked to have subnodes
                    // In this case it does not matter if node was selected from normal menu, bookmarks or search
                    if (!searchView.isIconified()) {
                        searchView.onActionViewCollapsed();
                        MainView.this.hideNavigation(false);
                    }
                    MainView.this.openSubmenu();
                } else {
                    if (bookmarksToggle) {
                        // If node was selected from bookmarks
                        MainView.this.setClickedItemInSubmenu();
                    } else if (!searchView.isIconified()) {
                        // Node selected from the search
                        searchView.onActionViewCollapsed();
                        MainView.this.hideNavigation(false);
                        MainView.this.setClickedItemInSubmenu();
                    } else {
                        // Node selected from normal menu
                        MainView.this.currentNodePosition = position;
                        MainView.this.adapter.markItemSelected(MainView.this.currentNodePosition);
                    }
                    MainView.this.adapter.notifyDataSetChanged();
                }
                if (bookmarksToggle) {
                    MainView.this.navigationNormalMode(true);
                    MainView.this.bookmarkVariablesReset();
                }

                MainView.this.loadNodeContent();
            }
        });
        rvMenu.setAdapter(this.adapter);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!searchView.isIconified()) { // This check fixes bug where all database's nodes were displayed after screen rotation
                    MainView.this.filterNodes(newText);
                }
                return false;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            // When the user closes search view without selecting a node
            @Override
            public boolean onClose() {
                if (MainView.this.currentNode != null) {
                    MainView.this.mainViewModel.restoreSavedCurrentNodes();
                    MainView.this.currentNodePosition = MainView.this.tempCurrentNodePosition;
                    MainView.this.adapter.markItemSelected(MainView.this.currentNodePosition);
                    MainView.this.adapter.notifyDataSetChanged();
                } else {
                    // If there is no node selected that means that main menu has to be loaded
                    MainView.this.mainViewModel.setNodes(MainView.this.reader.getMainNodes());
                }
                MainView.this.hideNavigation(false);
                MainView.this.mainViewModel.tempSearchNodesToggle(false);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new SearchView.OnClickListener() {
            // When user taps search icon
            @Override
            public void onClick(View v) {
                if (bookmarksToggle) {
                    // If bookmark menu was showed at the time of selecting search
                    // There is less things to change in menu
                    MainView.this.navigationNormalMode(true);
                    MainView.this.bookmarksToggle = false;
                } else {
                    // If search was selected from normal menu current menu items, selected item have to be saved
                    MainView.this.mainViewModel.saveCurrentNodes();
                    MainView.this.tempCurrentNodePosition = MainView.this.currentNodePosition;
                    MainView.this.currentNodePosition = -1;
                    MainView.this.adapter.markItemSelected(MainView.this.currentNodePosition); // Removing selection from menu item
                }
                MainView.this.hideNavigation(true);
                MainView.this.mainViewModel.setNodes(MainView.this.reader.getAllNodes());
                MainView.this.mainViewModel.tempSearchNodesToggle(true);
                // Previously it showed menu from which search view was opened
                MainView.this.adapter.notifyDataSetChanged();
            }
        });

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Registers listener for back button clicks
        getOnBackPressedDispatcher().addCallback(this, callbackDisplayToastBeforeExit);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@Nullable Bundle outState) {
        // Saving some variables to make it possible to restore the content after screen rotation
        outState.putInt("currentNodePosition", this.currentNodePosition);
        outState.putInt("tempCurrentNodePosition", this.tempCurrentNodePosition);
        outState.putStringArray("currentNode", this.currentNode);
        outState.putBoolean("bookmarksToggle", this.bookmarksToggle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        // At this stage it is possible to load the content to the fragment after the screen rotation
        // at earlier point app will crash
        super.onResume();
        if (this.currentNode != null) {
            this.setToolbarTitle();
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyItemChanged(this.currentNodePosition);
            if (getSupportFragmentManager().findFragmentByTag("image") == null) {
                this.loadNodeContent();
            }
        }

        if (bookmarksToggle) {
            this.navigationNormalMode(false);
        }
    }

    OnBackPressedCallback callbackDisplayToastBeforeExit = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {

            if (backToExit) { // If button back was already pressed once
                MainView.this.finish();
                return;
            }

            backToExit = true; // Marks that back button was pressed once
            Toast.makeText(MainView.this, R.string.toast_confirm_mainview_exit, Toast.LENGTH_SHORT).show();


            new Handler().postDelayed(new Runnable() {
                // Reverts boolean that marks if user pressed back button once after 2 seconds
                @Override
                public void run() {
                    backToExit = false;
                }
            }, 2000);
        }
    };

    private void openSubmenu() {
        // Clears existing menu and recreate with submenu of the currentNode
        this.mainViewModel.setNodes(this.reader.getSubnodes(this.currentNode[1]));
        this.currentNodePosition = 0;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();

    }

    private void setClickedItemInSubmenu() {
        // When user chooses to open bookmarked or search provided node
        // it's not always clear where said node is in menu.
        // This function gets the new menu and marks node as opened
        this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
        for (int index = 0; index < this.mainViewModel.getNodes().size(); index++) {
            if (this.mainViewModel.getNodes().get(index)[1].equals(this.currentNode[1])) {
                this.currentNodePosition = index;
                this.adapter.markItemSelected(this.currentNodePosition);
            }
        }
    }

    public void goBack(View view) {
        this.closeBookmarks();
    }

    public void goNodeUp(View view) {
        // Moves navigation menu one node up
        // If menu is already at the top it shows a message to the user
        ArrayList<String[]> nodes = this.reader.getParentWithSubnodes(this.mainViewModel.getNodes().get(0)[1]);
        if (nodes != null && nodes.size() != this.mainViewModel.getNodes().size()) {
            // If retrieved nodes are not null and array size do not match the one displayed
            // it is definitely not the same node so it can go up
            this.currentNode = this.mainViewModel.getNodes().get(0);
            this.mainViewModel.setNodes(nodes);
            this.currentNodePosition = -1;
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
        } else {
            // If both node arrays matches in size it might be the same node (especially main/top)
            // This part checks if first and last nodes in arrays matches by comparing uniqueID of both
            if (nodes.get(0)[1].equals(this.mainViewModel.getNodes().get(0)[1]) && nodes.get(nodes.size() -1 )[1].equals(this.mainViewModel.getNodes().get(this.mainViewModel.getNodes().size() -1 )[1])) {
                Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
            } else {
                this.currentNode = nodes.get(0);
                this.mainViewModel.setNodes(nodes);
                this.currentNodePosition = -1;
                this.adapter.markItemSelected(this.currentNodePosition);
                this.adapter.notifyDataSetChanged();
            }
        }
    }

    public void goHome(View view) {
        // Reloads drawer menu to show main menu
        // if it is not at the top yet
        // otherwise shows a message to the user that the top was already reached
        if (bookmarksToggle) {
            this.navigationNormalMode(true);
            this.bookmarkVariablesReset();
        }

        ArrayList<String[]> tempMainNodes = this.reader.getMainNodes();

        // Compares node sizes, first and last node's uniqueIDs in both arrays
        if (tempMainNodes.size() == this.mainViewModel.getNodes().size() && tempMainNodes.get(0)[1].equals(this.mainViewModel.getNodes().get(0)[1]) && tempMainNodes.get(this.mainViewModel.getNodes().size() -1 )[1].equals(this.mainViewModel.getNodes().get(this.mainViewModel.getNodes().size() -1 )[1])) {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        } else {
            this.mainViewModel.setNodes(tempMainNodes);
            this.currentNodePosition = -1;
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
        }
    }

    private void loadNodeContent() {

        FragmentManager fragmentManager = getSupportFragmentManager();
        // Checks if there is fragment with tag "image" in backStack and removes it if there it
        // it's not needed if user want's to see another content node.
        if (fragmentManager.findFragmentByTag("image") != null) {
            fragmentManager.beginTransaction()
                    .remove(fragmentManager.findFragmentByTag("image"))
                    .commit();
            fragmentManager.popBackStack();
        }

        // Gets instance of the fragment
        NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
        // Sends ArrayList to fragment to be added added to view
        mainViewModel.setNodeContent(this.reader.getNodeContent(this.currentNode[1]));

        this.setToolbarTitle();

        nodeContentFragment.loadContent();
    }

    private void setToolbarTitle() {
        // Sets toolbar title to the current node name
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(this.currentNode[0]);
    }

    private void filterNodes(String query) {
        // Filters node list by the name of the node
        // Changes the node list that represents menu and updates it
        // Case insensitive
        this.mainViewModel.setNodes(this.mainViewModel.getTempSearchNodes());

        ArrayList<String[]> filteredNodes = this.mainViewModel.getNodes().stream()
                .filter(node -> node[0].toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));

        this.mainViewModel.setNodes(filteredNodes);
        this.adapter.notifyDataSetChanged();
    }

    private void resetMenuToCurrentNode() {
        // Restores drawer menu selected item to currently opened node

        if (this.currentNode != null) {

            if (MainView.this.currentNode[2].equals("true")) { // Checks if node is marked to have subnodes
                this.mainViewModel.setNodes(this.reader.getSubnodes(this.currentNode[1]));
                this.currentNodePosition = 0;
                this.adapter.markItemSelected(this.currentNodePosition);
            } else {
                this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
                for (int index = 0; index < this.mainViewModel.getNodes().size(); index++) {
                    if (this.mainViewModel.getNodes().get(index)[1].equals(this.currentNode[1])) {
                        this.currentNodePosition = index;
                        this.adapter.markItemSelected(this.currentNodePosition);
                    }
                }
            }

            this.adapter.notifyDataSetChanged();
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
        this.resetMenuToCurrentNode();
        this.loadNodeContent();
    }

    public void openCloseBookmarks(View view) {
        // Toggles between displaying and hiding of bookmarks
        if (this.bookmarksToggle) {
            // Showing normal menu
            this.closeBookmarks();
        } else {
            showBookmarks();
        }
    }

    private void showBookmarks() {
        // Displays bookmarks instead of normal navigation menu in navigation drawer

        ArrayList<String[]> bookmarkedNodes = this.reader.getBookmarkedNodes();

        // Check if there are any bookmarks
        // If no bookmarks were found a message is displayed
        // No other action is taken
        if (bookmarkedNodes == null) {
            Toast.makeText(this, R.string.toast_no_bookmarks_message, Toast.LENGTH_SHORT).show();
        } else {
            // Displaying bookmarks
            this.navigationNormalMode(false);
            // Saving current state of the menu
            this.mainViewModel.saveCurrentNodes();
            this.tempCurrentNodePosition = this.currentNodePosition;

            // Displaying bookmarks
            this.mainViewModel.setNodes(bookmarkedNodes);
            this.adapter.markItemSelected(-1);
            this.adapter.notifyDataSetChanged();
            this.bookmarksToggle = true;
        }
    }

    private void closeBookmarks() {
        // Restoring saved node status
        this.mainViewModel.restoreSavedCurrentNodes();
        this.currentNodePosition = this.tempCurrentNodePosition;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
        this.navigationNormalMode(true);
        this.bookmarkVariablesReset();
    }

    private void navigationNormalMode(boolean status) {
        // This function restores navigation buttons to the normal state
        // as opposite to Bookmark navigation mode
        // true - normal mode, false - bookmark mode

        ImageButton goBackButton = findViewById(R.id.navigation_drawer_button_back);
        ImageButton goUpButton = findViewById(R.id.navigation_drawer_button_up);
        ImageButton bookmarksButton = findViewById(R.id.navigation_drawer_button_bookmarks);

        if (status) {
            goBackButton.setVisibility(View.GONE);
            goUpButton.setVisibility(View.VISIBLE);
            bookmarksButton.setImageDrawable(getDrawable(R.drawable.ic_outline_bookmarks_off_24));
        } else {
            goBackButton.setVisibility(View.VISIBLE);
            goUpButton.setVisibility(View.GONE);
            bookmarksButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_bookmarks_on_24));
        }
    }

    private void bookmarkVariablesReset() {
        // Sets variables that were used to display bookmarks to they default values
        this.mainViewModel.resetTempNodes();
        this.tempCurrentNodePosition = -1;
        this.bookmarksToggle = false;
    }

    private void hideNavigation(boolean status) {
        // Hides or displays navigation buttons at the top of drawer menu
        // Used when user taps on search icon to make the search field bigger
        ImageButton goBackButton = findViewById(R.id.navigation_drawer_button_back);
        ImageButton upButton = findViewById(R.id.navigation_drawer_button_up);
        ImageButton homeButton = findViewById(R.id.navigation_drawer_button_home);
        ImageButton bookmarksButton = findViewById(R.id.navigation_drawer_button_bookmarks);

        if (status == true) {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.GONE);
            homeButton.setVisibility(View.GONE);
            bookmarksButton.setVisibility(View.GONE);
        } else {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.VISIBLE);
            homeButton.setVisibility(View.VISIBLE);
            bookmarksButton.setVisibility(View.VISIBLE);
        }
    }
}