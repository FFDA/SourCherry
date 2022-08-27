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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainView extends AppCompatActivity {

    private ActionBarDrawerToggle actionBarDrawerToggle;
    private DatabaseReader reader;
    private MenuItemAdapter adapter;
    private String[] currentNode;
    private int currentNodePosition; // In menu / MenuItemAdapter for marking menu item opened/selected
    private boolean bookmarksToggle; // To save state for bookmarks. True means bookmarks are being displayed
    private boolean filterNodeToggle;
    private boolean findInNodeToggle; // Holds true when FindInNode view is initiated
    private int tempCurrentNodePosition; // Needed to save selected node position when user opens bookmarks;
    private boolean backToExit;
    private MainViewModel mainViewModel;
    private SharedPreferences sharedPreferences;
    private ExecutorService executor;
    private Handler handler;
    private int currentFindInNodeMarked; // Index of the result that is marked from FindInNode results. -1 Means nothing is selected

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        this.backToExit = false;
        this.executor = Executors.newSingleThreadExecutor();
        this.handler = new Handler(Looper.getMainLooper());
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        SearchView searchView = findViewById(R.id.navigation_drawer_search);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String databaseString = sharedPreferences.getString("databaseUri", null);

        try {
            if (sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
                // If file is in external storage
                if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = getContentResolver().openInputStream(Uri.parse(databaseString));
                    this.reader = new XMLReader(is, this, this.handler);
                    is.close();
                }
            } else {
                // If file is in internal app storage
                if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
                    // If file is xml
                    InputStream is = new FileInputStream(sharedPreferences.getString("databaseUri", null));
                    this.reader = new XMLReader(is, this, this.handler);
                    is.close();
                } else {
                    // If file is sql (password protected or not)
                    SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(Uri.parse(databaseString).getPath(), null, SQLiteDatabase.OPEN_READONLY);
                    this.reader = new SQLReader(sqlite, this, this.handler);
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
            this.bookmarksToggle = false;
            this.filterNodeToggle = false;
            this.findInNodeToggle = false;
            this.currentFindInNodeMarked = -1;
            if (this.sharedPreferences.getBoolean("restore_last_node", false) && this.sharedPreferences.getString("last_node_name", null) != null) {
                // Restores node on startup if user set this in settings
                this.currentNodePosition = this.sharedPreferences.getInt("last_node_position", -1);
                this.currentNode = new String[]{this.sharedPreferences.getString("last_node_name", null), this.sharedPreferences.getString("last_node_unique_id", null), this.sharedPreferences.getString("last_node_has_subnodes", null), this.sharedPreferences.getString("last_node_is_parent", null), this.sharedPreferences.getString("last_node_is_subnode", null)};
                if (this.currentNode[2].equals("true")) { // Checks if menu has subnodes and creates appropriate menu
                    this.mainViewModel.setNodes(this.reader.getSubnodes(this.currentNode[1]));
                } else {
                    this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
                }
            }  else {
                this.currentNodePosition = -1;
                this.currentNode = null; // This needs to be placed before restoring the instance if there was one
                this.mainViewModel.setNodes(this.reader.getMainNodes());
            }
        } else {
            // Restoring some variable to make it possible restore content fragment after the screen rotation
            this.currentNodePosition = savedInstanceState.getInt("currentNodePosition");
            this.tempCurrentNodePosition = savedInstanceState.getInt("tempCurrentNodePosition");
            this.currentNode = savedInstanceState.getStringArray("currentNode");
            this.bookmarksToggle = savedInstanceState.getBoolean("bookmarksToggle");
            this.filterNodeToggle = savedInstanceState.getBoolean("filterNodeToggle");
            this.findInNodeToggle = savedInstanceState.getBoolean("findInNodeToggle");
        }

        RecyclerView rvMenu = findViewById(R.id.recyclerView);
        this.adapter = new MenuItemAdapter(this.mainViewModel.getNodes(), this);
        this.adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (MainView.this.currentNode == null || !MainView.this.mainViewModel.getNodes().get(position)[1].equals(MainView.this.currentNode[1])) {
                    // If current node is null (empty/nothing opened yet) or selected uniqueID is not the same as selected one
                    MainView.this.currentNode = MainView.this.mainViewModel.getNodes().get(position);
                    MainView.this.loadNodeContent();
                    if (MainView.this.mainViewModel.getNodes().get(position)[2].equals("true")) { // Checks if node is marked to have subnodes
                        // In this case it does not matter if node was selected from normal menu, bookmarks or search
                        if (MainView.this.filterNodeToggle) {
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.filterNodeToggle = false;
                        }
                        MainView.this.openSubmenu();
                    } else {
                        if (MainView.this.sharedPreferences.getBoolean("auto_open", false)) {
                            drawerLayout.close();
                        }
                        if (MainView.this.bookmarksToggle) {
                            // If node was selected from bookmarks
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.adapter.notifyDataSetChanged();
                        } else if (MainView.this.filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.filterNodeToggle = false;
                            MainView.this.adapter.notifyDataSetChanged();
                        } else {
                            // Node selected from normal menu
                            int previousNodePosition = MainView.this.currentNodePosition;
                            MainView.this.currentNodePosition = position;
                            MainView.this.adapter.markItemSelected(MainView.this.currentNodePosition);
                            MainView.this.adapter.notifyItemChanged(previousNodePosition);
                            MainView.this.adapter.notifyItemChanged(position);
                        }
                    }
                    if (MainView.this.bookmarksToggle) {
                        MainView.this.navigationNormalMode(true);
                        MainView.this.bookmarkVariablesReset();
                    }
                } else {
                    // If already opened node was selected by the user
                    // Helps to save some reads from database and reloading of navigation menu
                    if (MainView.this.sharedPreferences.getBoolean("auto_open", false)) {
                        drawerLayout.close();
                    }
                    if (MainView.this.mainViewModel.getNodes().get(position)[2].equals("true")) { // Checks if node is marked as having subnodes
                        if (MainView.this.filterNodeToggle) {
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.filterNodeToggle = false;
                        }
                        MainView.this.openSubmenu();
                    } else {
                        if (MainView.this.bookmarksToggle) {
                            // If node was selected from bookmarks
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.adapter.notifyDataSetChanged();
                        } else if (MainView.this.filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.filterNodeToggle = false;
                            MainView.this.adapter.notifyDataSetChanged();
                        }
                    }
                    if (MainView.this.bookmarksToggle) {
                        MainView.this.navigationNormalMode(true);
                        MainView.this.bookmarkVariablesReset();
                    }
                }
            }
        });
        rvMenu.setAdapter(this.adapter);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        CheckBox checkBoxExcludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);
        checkBoxExcludeFromSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (MainView.this.filterNodeToggle) {
                    // Gets new menu list only if filter mode is activated
                    MainView.this.mainViewModel.setTempSearchNodes(MainView.this.reader.getAllNodes(isChecked));
                    MainView.this.adapter.notifyDataSetChanged();
                    searchView.setQuery("", false);
                    searchView.requestFocus();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (MainView.this.filterNodeToggle) { // This check fixes bug where all database's nodes were displayed after screen rotation
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
                MainView.this.filterNodeToggle = false;
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
                MainView.this.mainViewModel.setNodes(MainView.this.reader.getAllNodes(checkBoxExcludeFromSearch.isChecked()));
                MainView.this.mainViewModel.tempSearchNodesToggle(true);
                MainView.this.filterNodeToggle = true;
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

        // Button in findInView to close it
        ImageButton findInNodeCloseButton = findViewById(R.id.find_in_node_button_close);
        findInNodeCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainView.this.closeFindInNode();
            }
        });

        // Button in findInView to jump/show next result
        ImageButton findInNodeButtonNext = findViewById(R.id.find_in_node_button_next);
        findInNodeButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only if there are more than one result
                if (MainView.this.mainViewModel.getFindInNodeResultCount() > 1) {
                    MainView.this.findInNodeNext();
                }

            }
        });

        // Button in findInView to jump/show previous result
        ImageButton findInNodeButtonPrevious = findViewById(R.id.find_in_node_button_previous);
        findInNodeButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only if there are more than one result
                if (MainView.this.mainViewModel.getFindInNodeResultCount() > 1) {
                    MainView.this.findInNodePrevious();
                }

            }
        });

        // Listener for FindInNode search text change
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);
        findInNodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // After user types the text in search field
                // There is a delay of 400 milliseconds
                // to start the search only when user stops typing
                if (MainView.this.findInNodeToggle && findInNodeEditText.isFocused()) {
                    MainView.this.handler.removeCallbacksAndMessages(null);
                    MainView.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainView.this.findInNode(s.toString());
                        }
                    }, 400);
                }
            }
        });

        // Listener for FindInNode "enter" button click
        // Moves to the next findInNode result
        findInNodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_NEXT && MainView.this.mainViewModel.getFindInNodeResultCount() > 1) {
                    MainView.this.findInNodeNext();
                    handled = true;
                }
                return handled;
            }
        });

        // Listener for drawerMenu states
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                // If FindInNode view is on when user opens a drawer menu
                // Coses FindInNode view
                // Otherwise when user preses findInNodeNext/findInNodePrevious button in new node
                // content of the previous node will be loaded
                if (MainView.this.findInNodeToggle) {
                    MainView.this.closeFindInNode();
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            // Drawer menu items
            return true;
        } else {
            // Options menu items
            switch (item.getItemId()) {
                case (R.id.options_menu_export_to_pdf):
                    this.exportPdfSetup();
                    return true;
                case (R.id.options_menu_find_in_node):
                    if (!findInNodeToggle) {
                        // Opens findInNode (sets the variables) only if it hasn't been opened yet
                        this.openFindInNode();
                    }
                    return true;
                case (R.id.options_menu_search):
                    if (findInNodeToggle) {
                        // Closes findInNode if it was opened when searchActivity was selected to be opened
                        // Otherwise it will prevent to displayed node content selected from search
                        this.closeFindInNode();
                    }
                    Intent openSearchActivity = new Intent(this, SearchActivity.class);
                    searchActivity.launch(openSearchActivity);
                    return true;
                case (R.id.options_menu_settings):
                    Intent openSettingsActivity = new Intent(this, PreferencesActivity.class);
                    startActivity(openSettingsActivity);
                    return true;
                case (R.id.options_menu_about):
                    Intent openAboutActivity = new Intent(this, AboutActivity.class);
                    startActivity(openAboutActivity);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
    }

    private ActivityResultLauncher<Intent> searchActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent selectedNode = result.getData();
                    MainView.this.currentNode = selectedNode.getStringArrayExtra("selectedNode");
                    MainView.this.resetMenuToCurrentNode();
                    MainView.this.loadNodeContent();
                }
            }
        });

    @Override
    public void onSaveInstanceState(@Nullable Bundle outState) {
        // Saving some variables to make it possible to restore the content after screen rotation
        outState.putInt("currentNodePosition", this.currentNodePosition);
        outState.putInt("tempCurrentNodePosition", this.tempCurrentNodePosition);
        outState.putStringArray("currentNode", this.currentNode);
        outState.putBoolean("bookmarksToggle", this.bookmarksToggle);
        outState.putBoolean("filterNodeToggle", this.filterNodeToggle);
        outState.putBoolean("findInNodeToggle", this.findInNodeToggle);
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
            this.loadNodeContent();
        }

        if (this.filterNodeToggle) {
            this.hideNavigation(true);
        }

        if (this.bookmarksToggle) {
            this.navigationNormalMode(false);
        }

        // Restoring FindInNode variables to original state
        if (this.findInNodeToggle) {
            this.closeFindInNode();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.sharedPreferences.getBoolean("restore_last_node", false) && this.currentNode != null) {
            // Saving current current node state to be able to load it on next startup
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
            sharedPreferencesEditor.putString("last_node_name", this.currentNode[0]);
            sharedPreferencesEditor.putString("last_node_unique_id", this.currentNode[1]);
            sharedPreferencesEditor.putString("last_node_has_subnodes", this.currentNode[2]);
            sharedPreferencesEditor.putString("last_node_is_parent", this.currentNode[3]);
            sharedPreferencesEditor.putString("last_node_is_subnode", this.currentNode[4]);
            if (this.bookmarksToggle || this.filterNodeToggle) {
                // If search or bookmarks were being shown temporary node position needs to be saved
                sharedPreferencesEditor.putInt("last_node_position", this.tempCurrentNodePosition);
            } else {
                sharedPreferencesEditor.putInt("last_node_position", this.currentNodePosition);
            }
            sharedPreferencesEditor.apply();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.executor.shutdownNow();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        menu.setGroupVisible(R.id.options_menu_main_activity_group, false);
        return true;
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

        ArrayList<String[]> tempMainNodes = this.reader.getMainNodes();

        // Compares node sizes, first and last node's uniqueIDs in both arrays
        if (tempMainNodes.size() == this.mainViewModel.getNodes().size() && tempMainNodes.get(0)[1].equals(this.mainViewModel.getNodes().get(0)[1]) && tempMainNodes.get(this.mainViewModel.getNodes().size() -1 )[1].equals(this.mainViewModel.getNodes().get(this.mainViewModel.getNodes().size() -1 )[1])) {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        } else {
            this.mainViewModel.setNodes(tempMainNodes);
            this.currentNodePosition = -1;
            if (bookmarksToggle && this.currentNode != null) {
                // Just in case user chose to come back from bookmarks to home and a node is selected
                // it might me that selected node is in main menu
                // this part checks for that and marks the node if it finds it
                for (int i = 0; i < this.mainViewModel.getNodes().size(); i++) {
                    // Checks uniqueID of current node against node in main menu
                    if (this.mainViewModel.getNodes().get(i)[1].equals(this.currentNode[1])) {
                        this.currentNodePosition = i;
                        break;
                    }
                }
            }
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
        }

        if (bookmarksToggle) {
            this.navigationNormalMode(true);
            this.bookmarkVariablesReset();
        }
    }

    private void loadNodeContent() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Gets instance of the fragment
        NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
        // Sends ArrayList to fragment to be added added to view
        this.setToolbarTitle();
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                MainView.this.mainViewModel.setNodeContent(MainView.this.reader.getNodeContent(MainView.this.currentNode[1]));
                nodeContentFragment.loadContent();
            }
        });
    }

    private void setToolbarTitle() {
        // Sets toolbar title to the current node name
        Toolbar toolbar = findViewById(R.id.toolbar);
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

    public void openAnchorLink(String[] nodeArray) {
        if (this.findInNodeToggle) {
            // Closes findInNode view to clear all variables
            // Otherwise loaded node in some cases might display previous node's content
            this.closeFindInNode();
        }
        this.currentNode = nodeArray;
        this.resetMenuToCurrentNode();
        this.loadNodeContent();
    }

    public void fileFolderLinkFilepath(String filename) {
        // Displays Snackbar with string that was passed as an argument
        // Used to display file path of link to file/folder
        Snackbar.make(findViewById(R.id.main_view_fragment), filename, Snackbar.LENGTH_LONG)
        .setAction(R.string.snackbar_dismiss_action, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        })
        .show();
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
            this.currentNodePosition = -1;
            this.adapter.markItemSelected(this.currentNodePosition);
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
        CheckBox excludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);

        if (status) {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.GONE);
            homeButton.setVisibility(View.GONE);
            bookmarksButton.setVisibility(View.GONE);
            excludeFromSearch.setVisibility(View.VISIBLE);
        } else {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.VISIBLE);
            homeButton.setVisibility(View.VISIBLE);
            bookmarksButton.setVisibility(View.VISIBLE);
            excludeFromSearch.setVisibility(View.GONE);
        }
    }

    public Handler getHandler() {
        return this.handler;
    }

    private void closeFindInNode() {
        // Close findInNode view, keyboard and restores variables to initial values
        // * This prevents crashes when user makes a sudden decision to close findInNode view while last search hasn't finished
        this.handler.removeCallbacksAndMessages(null);
        // *
        this.findInNodeToggle = false;
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);
        findInNodeEditText.setText("");
        findInNodeEditText.clearFocus();

        this.restoreHighlightedView();

        // * Closing keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Shows keyboard on API 30 (Android 11) reliably
            WindowCompat.getInsetsController(getWindow(), findInNodeEditText).hide(WindowInsetsCompat.Type.ime());
        } else {
            new Handler().postDelayed(new Runnable() {
                // Delays to show soft keyboard by few milliseconds
                // Otherwise keyboard does not show up
                // It's a bit hacky (should be fixed)
                @Override
                public void run() {
                    imm.hideSoftInputFromWindow(findInNodeEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            }, 50);
        }
        // *

        // Clearing search field (restores content to original state too)
        LinearLayout findInNodeLinearLayout = findViewById(R.id.main_view_find_in_node_linear_layout);
        findInNodeLinearLayout.setVisibility(View.GONE);
        this.mainViewModel.findInNodeStorageToggle(false);
    }

    private void openFindInNode() {
        // Searches through the node and highlights matches
        this.findInNodeToggle = true;
        MainView.this.mainViewModel.findInNodeStorageToggle(true); // Created an array to store nodeContent
        LinearLayout findInNodeLinearLayout = findViewById(R.id.main_view_find_in_node_linear_layout);
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);

        findInNodeLinearLayout.setVisibility(View.VISIBLE); // Making findInView visible at the bottom if the window

        // * Displaying / opening keyboard
        findInNodeEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Shows keyboard on API 30 (Android 11) reliably
            WindowCompat.getInsetsController(getWindow(), findInNodeEditText).show(WindowInsetsCompat.Type.ime());
        } else {
            new Handler().postDelayed(new Runnable() {
                // Delays to show soft keyboard by few milliseconds
                // Otherwise keyboard does not show up
                // It's a bit hacky (should be fixed)
                @Override
                public void run() {
                    imm.showSoftInput(findInNodeEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 50);
        }
        // *

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                // * Collecting all nodeContent in ArrayList
                // Every TextView is added as a separate item (SpannableStringBuilder) in array list
                // If node does not have tables, it's most likely is just one
                // However, if node has table(s), every cell of the table is added as an item in array
                for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
                    View view = contentFragmentLinearLayout.getChildAt(i);
                    if (view instanceof TextView) {
                        MainView.this.mainViewModel.addFindInNodeStorage((SpannableStringBuilder) ((TextView) view).getText());
                    } else if (view instanceof HorizontalScrollView) {
                        TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                        for (int row = 0; row < tableLayout.getChildCount(); row++) {
                            TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                            for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                                // Reaches cell and adds it's text to ArrayList
                                TextView rowCell = (TextView) tableRow.getChildAt(cell);
                                SpannableStringBuilder cellSpannableStringBuilder = new SpannableStringBuilder();
                                cellSpannableStringBuilder.append(rowCell.getText());
                                MainView.this.mainViewModel.addFindInNodeStorage(cellSpannableStringBuilder);
                            }
                        }
                    }
                }
                // *
            }
        });
    }

    private void updateCounter(int counter) {
        // Sets the count of the results
        TextView findInNodeEditTextCount = findViewById(R.id.find_in_node_edit_text_result_count);
        handler.post(new Runnable() {
            @Override
            public void run() {
                findInNodeEditTextCount.setText(String.valueOf(counter));
            }
        });
    }

    private void updateMarkedIndex() {
        // Sets/updates index of currently marked result
        TextView findInNodeEditTextMarkedIndex = findViewById(R.id.find_in_node_edit_text_marked_index);
        handler.post(new Runnable() {
            @Override
            public void run() {
                findInNodeEditTextMarkedIndex.setText(String.valueOf(MainView.this.currentFindInNodeMarked + 1));
            }
        });
    }

    private void setFindInNodeProgressBar(Boolean status) {
        // Depending on status starts (true) or stops (false) progress bar
        handler.post(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = findViewById(R.id.find_in_node_progress_bar);
                progressBar.setIndeterminate(status);
            }
        });
    }

    private void findInNode(String query) {
        // Searches for query in nodeContent
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);

        if (query.length() > 0) {
            // If new query is longer when one character
            this.restoreHighlightedView();
            MainView.this.executor.execute(new Runnable() {
                @Override
                public void run() {

                    MainView.this.setFindInNodeProgressBar(true);
                    int counter = 0; // To keep track of which item in the nodeContent array it is
                    for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
                        View view = contentFragmentLinearLayout.getChildAt(i);
                        if (view instanceof TextView) {
                            // if textview
                            int searchLength = query.length();
                            SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                            spannedSearchQuery.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                            int index = 0;
                            while ((index != -1)) {
                                index = spannedSearchQuery.toString().toLowerCase().indexOf(query.toLowerCase(), index); // searches in case insensitive mode
                                if (index != -1) {
                                    // If there was a match
                                    int startIndex = index;
                                    int endIndex = index + searchLength; // End of the substring that has to be marked
                                    MainView.this.mainViewModel.addFindInNodeResult(new int[] {counter, startIndex, endIndex});
                                index += searchLength; // moves search to the end of the last found string
                                }
                            }
                            counter++;
                        } else if (view instanceof HorizontalScrollView) {
                            // if it is a table
                            // Has to go to the cell level to reach text
                            // to be able to mark it at appropriate place
                            int searchLength = query.length();

                            TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                            for (int row = 0; row < tableLayout.getChildCount(); row++) {
                                TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                                for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                                    SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                                    spannedSearchQuery.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                                    int index = 0;
                                    while ((index != -1)) {
                                        index = spannedSearchQuery.toString().toLowerCase().indexOf(query.toLowerCase(), index); // searches in case insensitive mode
                                        if (index != -1) {
                                            int startIndex = index;
                                            int endIndex = index + searchLength; // End of the substring that has to be marked
                                            MainView.this.mainViewModel.addFindInNodeResult(new int[]{counter, startIndex, endIndex});
                                            index += searchLength; // moves search to the end of the last found string
                                        }
                                    }
                                    counter++;
                                }
                            }
                        }
                    }
                    MainView.this.updateCounter(MainView.this.mainViewModel.getFindInNodeResultCount());
                    if (MainView.this.mainViewModel.getFindInNodeResultCount() == 0) {
                        // If user types until there are no matches left
                        MainView.this.restoreHighlightedView();
                    } else {
                        // If there are matches for user query
                        // First result has to be highlighter and scrolled too
                        MainView.this.currentFindInNodeMarked = 0;
                        MainView.this.highlightFindInNodeResult(MainView.this.currentFindInNodeMarked);
                    }
                    MainView.this.setFindInNodeProgressBar(false);
                }
            });
        } else {
            // If new query is 0 characters long, that means that user deleted everything and view should be reset to original
            MainView.this.restoreHighlightedView();
        }
    }

    private void highlightFindInNodeResult(int resultIndex) {
        // Highlights result from findInNodeResultStorage (array list) that is identified by currentFindInNodeMarked
        int viewCounter = this.mainViewModel.getFindInNodeResult(resultIndex)[0]; // Saved index for the view
        int startIndex = this.mainViewModel.getFindInNodeResult(resultIndex)[1];
        int endIndex = this.mainViewModel.getFindInNodeResult(resultIndex)[2];

        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        ScrollView contentFragmentScrollView = findViewById(R.id.content_fragment_scrollview);
        int lineCounter = 0; // Needed to calculate position where view will have to be scrolled to
        int counter = 0; // Iterator of the all the saved views from node content

        for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
            View view = contentFragmentLinearLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView currentTextView = (TextView) view;
                // If substring that has to be marked IS IN the same view as previously marked substring
                // Previous "highlight" will be removed while marking the current one
                if (viewCounter == counter) {
                    SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                    spannedSearchQuery.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                    spannedSearchQuery.setSpan(new BackgroundColorSpan(getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    int line = currentTextView.getLayout().getLineForOffset(startIndex); // Gets the line of the current string in current view
                    int lineHeight = currentTextView.getLineHeight(); // needed to calculate the amount of pixel screen has to be scrolled down
                    int currentLineCounter = lineCounter;
                    this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            currentTextView.setText(spannedSearchQuery);
                            // Scrolls view down to the line of the marked substring (query)
                            // -100 pixels are to make highlighted substring not to be at the top of the screen
                            contentFragmentScrollView.scrollTo(0, (line * lineHeight) + currentLineCounter - 100);
                        }
                    });
                    MainView.this.updateMarkedIndex();
                }
                // Adds all TextView height to lineCounter. Will be used to move screen to correct position if there are more views
                lineCounter += currentTextView.getHeight();
                counter++;
            } else if (view instanceof HorizontalScrollView) {
                // If encountered a table
                TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                    // If substring that has to be marked IS IN the same view as previously marked substring
                for (int row = 0; row < tableLayout.getChildCount(); row++) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                    for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                        if (viewCounter == counter) {
                            // If encountered a view that has to be marked
                            TextView currentCell = (TextView) tableRow.getChildAt(cell);
                            SpannableStringBuilder spannedSearchQuery = new SpannableStringBuilder();
                            spannedSearchQuery.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                            spannedSearchQuery.setSpan(new BackgroundColorSpan(getColor(R.color.cherry_red_200)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            int currentLineCounter = lineCounter;
                            this.handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    currentCell.setText(spannedSearchQuery);
                                    // Scrolls view down to the line of the marked substring (query)
                                    // -100 pixels are to make highlighted substring not to be at the top of the screen
                                    contentFragmentScrollView.scrollTo(0, currentLineCounter - 100);
                                }
                            });
                            MainView.this.updateMarkedIndex();
                        }
                        counter++;
                    }
                    // Adds row's height to lineCounter. Will be used to move screen to correct position if there are more views
                    lineCounter += tableRow.getHeight();
                }
            }
        }
    }

    private void restoreHighlightedView() {
        // Restores TextView to original state that was change changed with highlightFindInNodeResult() function
        // Uses index of the TextView in currentFindInNodeMarked
        // At the end sets currentFindInNodeMarked to 0 (nothing marked)
        // Resets counters and search result storage too
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        if (this.currentFindInNodeMarked != -1 && contentFragmentLinearLayout != null) {
            int viewIndex = this.mainViewModel.getFindInNodeResult(this.currentFindInNodeMarked)[0];
            int counter = 0;
            for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
                View view = contentFragmentLinearLayout.getChildAt(i);
                if (view instanceof TextView) {
                    if (viewIndex == counter) {
                        SpannableStringBuilder originalText = new SpannableStringBuilder();
                        originalText.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                        this.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView) view).setText(originalText);
                            }
                        });
                    }
                    counter++;
                } else if (view instanceof HorizontalScrollView) {
                    TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                    for (int row = 0; row < tableLayout.getChildCount(); row++) {
                        TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                        for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                            if (viewIndex == counter) {
                                TextView currentCell = (TextView) tableRow.getChildAt(cell);
                                SpannableStringBuilder originalText = new SpannableStringBuilder();
                                originalText.append(MainView.this.mainViewModel.getFindInNodeStorageItem(counter));
                                this.handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentCell.setText(originalText);
                                    }
                                });
                            }
                            counter++;
                        }
                    }
                }
            }
            this.currentFindInNodeMarked = -1;
            this.updateCounter(0);
            this.updateMarkedIndex();
            this.mainViewModel.resetFindInNodeResultStorage();
        }
    }

    private void findInNodeNext() {
        // Calculates next result that has to be highlighted
        // and initiates switchFindInNodeHighlight
        this.currentFindInNodeMarked++;
        this.updateMarkedIndex();
        if (this.currentFindInNodeMarked <= this.mainViewModel.getFindInNodeResultCount() - 1) {
            int previouslyHighlightedFindInNode;
            if (this.currentFindInNodeMarked == 0) {
                // Current marked node is first in the array, so previous marked should be the last from array
                previouslyHighlightedFindInNode = this.mainViewModel.getFindInNodeResult(this.mainViewModel.getFindInNodeResultCount() - 1)[0];
            } else {
                // Otherwise it should be previous one in array. However, it can be that it is out off array if array is made of one item.
                previouslyHighlightedFindInNode = this.mainViewModel.getFindInNodeResult(this.currentFindInNodeMarked - 1)[0];
            }
            // Gets instance of the fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
            nodeContentFragment.switchFindInNodeHighlight(previouslyHighlightedFindInNode, this.currentFindInNodeMarked);
        } else {
            // Reached the last index of the result array
            // currentFindInNodeMarked has to be reset to the first index if the result array and this function restarted
            // If you want to though the result in a loop
            this.currentFindInNodeMarked = -1;
            this.findInNodeNext();
        }
    }

    private void findInNodePrevious() {
        // Calculates previous result that has to be highlighted
        // and initiates switchFindInNodeHighlight
        this.currentFindInNodeMarked--;
        this.updateMarkedIndex();
        if (this.currentFindInNodeMarked >= 0) {
            int previouslyHighlightedFindInNode;
            if (this.currentFindInNodeMarked == this.mainViewModel.getFindInNodeResultCount() - 1) {
                // Current marked node is last, so previous marked node should be the first in result ArrayList
                previouslyHighlightedFindInNode = this.mainViewModel.getFindInNodeResult(0)[0];
            } else {
                // Otherwise it should next one in array (index+1). However, it can be that it is out off array if array is made of one item
                previouslyHighlightedFindInNode = this.mainViewModel.getFindInNodeResult(this.currentFindInNodeMarked + 1)[0]; // Saved index for the view
            }
            // Gets instance of the fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
            nodeContentFragment.switchFindInNodeHighlight(previouslyHighlightedFindInNode, this.currentFindInNodeMarked);
        } else {
            // Reached the first index of the result array
            // currentFindInNodeMarked has to be reset to last index of the result array and this function restarted
            // If you want to though the result in a loop
            this.currentFindInNodeMarked =  this.mainViewModel.getFindInNodeResultCount();
            this.findInNodePrevious();
        }
    }

    public void saveOpenFile(String nodeUniqueID, String attachedFileFilename, String time) {
        // Checks preferences if user choice default action for embedded files
        FileNameMap fileNameMap  = URLConnection.getFileNameMap();
        String fileMimeType = fileNameMap.getContentTypeFor(attachedFileFilename);
        if (fileMimeType == null) {
            // Custom file extensions (like CherryTree database extensions) are not recognized by Android
            // If mimeType for selected file can't be recognized. Catch all mimetype has to set
            // Otherwise app will crash while trying to save the file
            fileMimeType = "*/*";
        }
        String saveOpenFilePreference = this.sharedPreferences.getString("preferences_save_open_file", "Ask");
        if (saveOpenFilePreference.equals("Ask")) {
            // Setting up to send arguments to Dialog Fragment
            Bundle bundle = new Bundle();
            bundle.putString("nodeUniqueID", nodeUniqueID);
            bundle.putString("filename", attachedFileFilename);
            bundle.putString("time", time);
            bundle.putString("fileMimeType", fileMimeType);

            // Opening dialog fragment to ask user for a choice
            SaveOpenDialogFragment saveOpenDialogFragment = new SaveOpenDialogFragment();
            saveOpenDialogFragment.setArguments(bundle);
            saveOpenDialogFragment.show(getSupportFragmentManager(), "saveOpenDialog");
        } else if (saveOpenFilePreference.equals("Save")) {
            // Saving file
            saveFile.launch(new String[]{fileMimeType, nodeUniqueID, attachedFileFilename, time});
        } else {
            // Opens file with intent for other apps
            this.openFile(fileMimeType, nodeUniqueID, attachedFileFilename, time);
        }
    }

    private void openFile(String fileMimeType, String nodeUniqueID, String filename, String time) {
        try {
            String[] splitFilename = filename.split("\\.");
            // If attached filename has more than one . (dot) in it temporary filename will not have full original filename in it
            // most important that it will have correct extension
            File tmpAttachedFile = File.createTempFile(splitFilename[0], "." + splitFilename[splitFilename.length - 1]); // Temporary file that will shared

            // Writes Base64 encoded string to the temporary file
            FileOutputStream out = new FileOutputStream(tmpAttachedFile);
            out.write(reader.getFileByteArray(nodeUniqueID, filename, time));
            out.close();

            // Getting Uri to share
            Uri tmpFileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tmpAttachedFile);

            // Intent to open file
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(tmpFileUri, fileMimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_error_failed_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    ActivityResultLauncher<String[]> saveFile = registerForActivityResult(new ReturnSelectedFileUriForSaving(), result -> {
        if (result != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(result.getData(), "w"); // Output file
                outputStream.write(reader.getFileByteArray(result.getExtras().getString("uniqueNodeID"), result.getExtras().getString("filename"), result.getExtras().getString("time")));
                outputStream.close();
            } catch (Exception e) {
                Toast.makeText(this, R.string.toast_error_failed_to_save_file, Toast.LENGTH_SHORT).show();
            }
        }
    });

    private void exportPdfSetup() {
        // Sets the intent for asking user to choose a location where to save a file
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, this.currentNode[0]);
        exportPdf.launch(intent);
    }

    ActivityResultLauncher<Intent> exportPdf = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            // If user actually chose a location to save a file
            try {
                LinearLayout nodeContent = findViewById(R.id.content_fragment_linearlayout);
                PdfDocument document = new PdfDocument();
                int padding = 25; // It's used not only pad the document, but to calculate where title will be placed on the page
                int top = padding * 4; // This will used to move (translate) cursor where everything has to be drawn on canvas
                int width = nodeContent.getWidth(); // Width of the PDF page

                for (int i= 0; i < nodeContent.getChildCount(); i++) {
                    // Going through all the views in node to find if there is a table
                    // Tables might be wider than screen
                    View v = nodeContent.getChildAt(i);
                    if (v instanceof HorizontalScrollView) {
                        // If table was encountered
                        TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) v).getChildAt(0);
                        if (tableLayout.getWidth() > width) {
                            // If table is wider than normal view
                            width = tableLayout.getWidth();

                        }
                    }
                }

                //* Creating a title view that will be drawn to PDF
                //** textPrimaryColor for the theme
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                int color = ContextCompat.getColor(this, typedValue.resourceId);
                //**
                TextPaint paint = new TextPaint();
                paint.setColor(color);
                paint.setTextSize(50);

                StaticLayout title = StaticLayout.Builder.obtain(this.currentNode[0], 0, this.currentNode[0].length(), paint, nodeContent.getWidth())
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .build();
                //*

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width + (padding * 2), nodeContent.getHeight() + (padding * 4) + title.getHeight(), 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);

                Canvas canvas = page.getCanvas();

                if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    // Changing background color of the canvas if drawing views from night mode
                    // Otherwise text wont be visible
                    canvas.drawColor(getColor(R.color.night_theme_windowBackground));
                }

                //* Drawing title to the canvas
                canvas.save(); // Saves current coordinates system
                canvas.translate(padding, padding * 2); // Moves coordinate system
                title.draw(canvas);
                top += title.getHeight();
                canvas.restore();
                //*

                for (int i= 0; i < nodeContent.getChildCount(); i++) {
                    View view = nodeContent.getChildAt(i);
                    canvas.save(); // Saves current coordinates system
                    canvas.translate(padding, top); // Moves coordinate system
                    if (view instanceof HorizontalScrollView) {
                        // If it is a table - TableLayout has to be drawn to canvas an not ScrollView
                        // Otherwise only visible part of the table will be showed
                        TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                        tableLayout.draw(canvas);
                    } else {
                        // TextView
                        view.draw(canvas);
                    }
                    canvas.restore(); // Restores coordinates system to saved state
                    top += view.getHeight();
                }

                document.finishPage(page);

                // Saving to file
                OutputStream outputStream = getContentResolver().openOutputStream(result.getData().getData(), "w"); // Output file
                document.writeTo(outputStream);

                // Cleaning up
                outputStream.close();
                document.close();
            } catch (Exception e) {
                Toast.makeText(this, R.string.toast_error_failed_to_export_node_to_pdf, Toast.LENGTH_SHORT).show();
            }
        }
    });
}