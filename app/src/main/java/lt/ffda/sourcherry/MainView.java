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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.dialogs.ExportDatabaseDialogFragment;
import lt.ffda.sourcherry.dialogs.MenuItemActionDialogFragment;
import lt.ffda.sourcherry.dialogs.SaveOpenDialogFragment;
import lt.ffda.sourcherry.fragments.CreateNodeFragment;
import lt.ffda.sourcherry.fragments.ImageViewFragment;
import lt.ffda.sourcherry.fragments.NodeContentFragment;
import lt.ffda.sourcherry.fragments.NodeEditorFragment;
import lt.ffda.sourcherry.fragments.MoveNodeFragment;
import lt.ffda.sourcherry.fragments.NodePropertiesFragment;
import lt.ffda.sourcherry.fragments.SearchFragment;
import lt.ffda.sourcherry.preferences.PreferencesActivity;
import lt.ffda.sourcherry.utils.MenuItemAction;
import lt.ffda.sourcherry.utils.ReturnSelectedFileUriForSaving;

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
    private DrawerLayout drawerLayout;
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
        this.drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.string.nav_open, R.string.nav_close);
        SearchView searchView = findViewById(R.id.navigation_drawer_search);

        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        DatabaseReaderFactory databaseReaderFactory = new DatabaseReaderFactory();
        try {
            this.reader = databaseReaderFactory.getReader(this, this.handler, this.sharedPreferences);
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_error_failed_to_read_database, Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true).setReorderingAllowed(true)
                    .add(R.id.main_view_fragment, NodeContentFragment.class, null, "main")
//                    .addToBackStack("main") // This not needed it seems. Clean up at the latter date
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            this.bookmarksToggle = false;
            this.filterNodeToggle = false;
            this.findInNodeToggle = false;
            this.currentFindInNodeMarked = -1;
            if (this.sharedPreferences.getBoolean("restore_last_node", false) && this.reader.doesNodeExist(this.sharedPreferences.getString("last_node_unique_id", null))) {
                // Restores node on startup if user set this in settings
                this.currentNode = this.reader.getSingleMenuItem(this.sharedPreferences.getString("last_node_unique_id", null));
                if (this.currentNode[2].equals("true")) { // Checks if menu has subnodes and creates appropriate menu
                    this.mainViewModel.setNodes(this.reader.getSubnodes(this.currentNode[1]));
                } else {
                    this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
                }
                this.setCurrentNodePosition();
            } else {
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

        // Register with fragmentManager to get result from menuItemActionDialogFragment
        getSupportFragmentManager().setFragmentResultListener("menuItemAction", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                MenuItemAction menuItemAction = (MenuItemAction) result.getSerializable("menuItemActionCode");
                String[] node = result.getStringArray("node");
                switch (menuItemAction) {
                    case ADD_SIBLING_NODE:
                        MainView.this.launchCreateNewNodeFragment(node[1], 0);
                        break;
                    case ADD_SUBNODE:
                        MainView.this.launchCreateNewNodeFragment(node[1], 1);
                        break;
                    case ADD_TO_BOOKMARKS:
                        MainView.this.addNodeToBookmarks(node[1]);
                        break;
                    case REMOVE_FROM_BOOKMARKS:
                        MainView.this.removeNodeFromBookmarks(node[1], result.getInt("position"));
                        break;
                    case MOVE_NODE:
                        MainView.this.launchMoveNodeFragment(node);
                        break;
                    case DELETE_NODE:
                        MainView.this.deleteNode(node[1], result.getInt("position"));
                        break;
                    case PROPERTIES:
                        MainView.this.openNodeProperties(node[1], result.getInt("position"));
                        break;
                }
            }
        });

        RecyclerView rvMenu = findViewById(R.id.recyclerView);
        this.adapter = new MenuItemAdapter(this.mainViewModel.getNodes(), this);
        this.adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (MainView.this.currentNode == null || !MainView.this.mainViewModel.getNodes().get(position)[1].equals(MainView.this.currentNode[1])) {
                    // If current node is null (empty/nothing opened yet) or selected nodeUniqueID is not the same as selected one
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

        // Listener for long click on drawer menu item
        this.adapter.setOnLongClickListener(new MenuItemAdapter.OnLongClickListener() {
            @Override
            public void onLongClick(View itemView, int position) {
                MainView.this.openMenuItemActionDialogFragment(MainView.this.mainViewModel.getNodes().get(position), position);
            }
        });

        // Listener for click on drawer menu item's action icon
        this.adapter.setOnItemActionMenuClickListener(new MenuItemAdapter.OnActionIconClickListener() {
            @Override
            public void onActionIconClick(View itemView, int position) {
                MainView.this.openMenuItemActionDialogFragment(MainView.this.mainViewModel.getNodes().get(position), position);
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
            int itemID = item.getItemId();
            if (itemID == R.id.toolbar_button_edit_node) {
                if (this.currentNode != null) {
                    if (this.reader.isNodeRichText(this.currentNode[1])) {
                        Toast.makeText(this, R.string.toast_message_rich_text_node_editing_not_supported, Toast.LENGTH_SHORT).show();
                    } else {
                        this.openNodeEditor();
                    }
                } else {
                    Toast.makeText(this, R.string.toast_message_please_open_a_node, Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemID == R.id.options_menu_export_to_pdf) {
                this.exportPdfSetup();
                return true;
            } else if (itemID == R.id.options_menu_export_database) {
                this.exportDatabaseSetup();
                return true;
            } else if (itemID == R.id.options_menu_find_in_node) {
                if (!findInNodeToggle) {
                    // Opens findInNode (sets the variables) only if it hasn't been opened yet
                    this.openFindInNode();
                }
                return true;
            } else if (itemID == R.id.options_menu_search) {
                if (findInNodeToggle) {
                    // Closes findInNode if it was opened when SearchFragment was selected to be opened
                    // Otherwise it won't let to display node content selected from search
                    this.closeFindInNode();
                }
                this.openSearch();
                return true;
            } else if (itemID == R.id.options_menu_settings) {
                Intent openSettingsActivity = new Intent(this, PreferencesActivity.class);
                startActivity(openSettingsActivity);
                return true;
            } else if (itemID == R.id.options_menu_about) {
                Intent openAboutActivity = new Intent(this, AboutActivity.class);
                startActivity(openAboutActivity);
                return true;
            } else {
                return super.onOptionsItemSelected(item);
            }
        }
    }

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
            this.setToolbarTitle(this.currentNode[0]);
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
            // Saving current nodeUniqueID to be able to load it on next startup
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
            sharedPreferencesEditor.putString("last_node_unique_id", this.currentNode[1]);
            sharedPreferencesEditor.apply();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.executor.shutdownNow();
    }

    /**
     * Deals with back button presses.
     * If there are any fragment in the BackStack - removes one
     * Handles back to exit to make user double press back button to exit
     */
    OnBackPressedCallback callbackDisplayToastBeforeExit = new OnBackPressedCallback(true /* enabled by default */) {
        @Override
        public void handleOnBackPressed() {

            if (backToExit) { // If button back was already pressed once
                MainView.this.finish();
                return;
            }

            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                getSupportActionBar().show();
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

    /**
     * Clears existing menu and recreate with submenu of the currentNode
     */
    private void openSubmenu() {
        this.mainViewModel.setNodes(this.reader.getSubnodes(this.currentNode[1]));
        this.currentNodePosition = 0;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * This function gets the new drawer menu list
     * and marks currently opened node as such.
     */
    private void setClickedItemInSubmenu() {
        this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
        for (int index = 0; index < this.mainViewModel.getNodes().size(); index++) {
            if (this.mainViewModel.getNodes().get(index)[1].equals(this.currentNode[1])) {
                this.currentNodePosition = index;
                this.adapter.markItemSelected(this.currentNodePosition);
            }
        }
    }

    /**
     * Sets current node as opened in drawer menu
     * by finding it's nodeUniqueID in drawer menu items
     * and setting it's index as this.currentNodePosition
     */
    private void setCurrentNodePosition() {
        for (int index = 0; index < this.mainViewModel.getNodes().size(); index++) {
            if (this.mainViewModel.getNodes().get(index)[1].equals(this.currentNode[1])) {
                this.currentNodePosition = index;
            }
        }
    }

    /**
     * Closes bookmarks in drawer menu
     * @param view view needed to associated button with action
     */
    public void goBack(View view) {
        this.closeBookmarks();
    }

    /**
     * Moves navigation menu one node up
     * If menu is already at the top it shows a message to the user
     * @param view view that was clicked
     */
    public void goNodeUp(View view) {
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
            // This part checks if first and last nodes in arrays matches by comparing nodeUniqueID of both
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

    /**
     * Reloads drawer menu to show main menu
     * if it is not displayed
     * otherwise shows a message to the user
     * that the top of the database tree was already reached
     * @param view view that was clicked
     */
    public void goHome(View view) {
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
                this.currentNodePosition = this.openedNodePositionInDrawerMenu();
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
        this.setToolbarTitle(this.currentNode[0]);
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                MainView.this.mainViewModel.setNodeContent(MainView.this.reader.getNodeContent(MainView.this.currentNode[1]));
                nodeContentFragment.loadContent();
            }
        });
    }

    /**
     * Removes node content from view and all references to
     * node in variables.
     * Reset drawer menu to main menu.
     */
    private void removeNodeContent() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Gets instance of the fragment
        NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
        this.setToolbarTitle("SourCherry");
        // Removes all node content
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                nodeContentFragment.removeLoadedNodeContent();
            }
        });
        // Removes all other references to node in UI and variables
        this.mainViewModel.setNodes(this.reader.getMainNodes());
        this.currentNode = null;
        this.currentNodePosition = RecyclerView.NO_POSITION;
        this.adapter.markItemSelected(currentNodePosition);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * Sets toolbar title to the provided string
     * @param title new title for the toolbar
     */
    private void setToolbarTitle(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(title);
    }

    /**
     * Filters node list by the name of the node
     * Changes the drawer menu item list to show only
     * nodes with matching text in the node title.
     * Search is case insensitive
     * @param query search query
     */
    private void filterNodes(String query) {
        this.mainViewModel.setNodes(this.mainViewModel.getTempSearchNodes());

        ArrayList<String[]> filteredNodes = this.mainViewModel.getNodes().stream()
                .filter(node -> node[0].toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));

        this.mainViewModel.setNodes(filteredNodes);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * Restores drawer menu selected item to currently opened node
     */
    private void resetMenuToCurrentNode() {
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
                        break;
                    }
                }
            }
            this.adapter.notifyDataSetChanged();
        }
    }

    /**
     * Returns database reader
     * @return database reader
     */
    public DatabaseReader getReader() {
        return this.reader;
    }

    /**
     * Opens node that user selected by clicking anchor link
     * @param nodeArray array that holds data of one drawer menu / currentNode item
     */
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

    /**
     * Displays Snackbar with the message
     * Used to display file path of link to file/folder
     * @param filename message to display for user
     */
    public void fileFolderLinkFilepath(String filename) {
        Snackbar.make(findViewById(R.id.main_view_fragment), filename, Snackbar.LENGTH_LONG)
        .setAction(R.string.snackbar_dismiss_action, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        })
        .show();
    }

    /**
     * Launches CreateNewNode to create node in main menu
     * @param view view that was clicked by the user
     */
    public void createNode(View view) {
        this.launchCreateNewNodeFragment("0", 1);
    }

    /**
     * Toggles between displaying and hiding of bookmarks
     * @param view view that was clicked by the user
     */
    public void openCloseBookmarks(View view) {
        if (this.bookmarksToggle) {
            // Showing normal menu
            this.closeBookmarks();
        } else {
            showBookmarks();
        }
    }

    /**
     * Displays bookmarks instead of normal navigation menu in navigation drawer
     */
    private void showBookmarks() {
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
            this.currentNodePosition = this.openedNodePositionInDrawerMenu();
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
            this.bookmarksToggle = true;
        }
    }

    /**
     * Restoring saved node status
     */
    private void closeBookmarks() {
        this.mainViewModel.restoreSavedCurrentNodes();
        this.currentNodePosition = this.tempCurrentNodePosition;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
        this.navigationNormalMode(true);
        this.bookmarkVariablesReset();
    }

    /**
     * Restores navigation buttons to the normal state
     * as opposite to Bookmark navigation mode
     * @param status true - normal mode, false - bookmark mode
     */
    private void navigationNormalMode(boolean status) {
        ImageButton goBackButton = findViewById(R.id.navigation_drawer_button_back);
        ImageButton goUpButton = findViewById(R.id.navigation_drawer_button_up);
        ImageButton createNode = findViewById(R.id.navigation_drawer_button_create_node);
        ImageButton bookmarksButton = findViewById(R.id.navigation_drawer_button_bookmarks);
        if (status) {
            goBackButton.setVisibility(View.GONE);
            goUpButton.setVisibility(View.VISIBLE);
            createNode.setVisibility(View.VISIBLE);
            bookmarksButton.setImageDrawable(getDrawable(R.drawable.ic_outline_bookmarks_off_24));
        } else {
            goBackButton.setVisibility(View.VISIBLE);
            goUpButton.setVisibility(View.GONE);
            createNode.setVisibility(View.VISIBLE);
            bookmarksButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_bookmarks_on_24));
        }
    }

    /**
     * Sets variables that were used to display bookmarks to their default values
     */
    private void bookmarkVariablesReset() {
        this.mainViewModel.resetTempNodes();
        this.tempCurrentNodePosition = -1;
        this.bookmarksToggle = false;
    }

    /**
     * Checks if currently opened node is shown in drawer menu
     * @return position of the node in current drawer menu. -1 if node was not found
     */
    private int openedNodePositionInDrawerMenu() {
        int position = -1;
        for (int i = 0; i < this.mainViewModel.getNodes().size(); i++) {
            if (this.currentNode[1].equals(this.mainViewModel.getNodes().get(i)[1])) {
                position = i;
                break;
            }
        }
        return position;
    }

    /**
     * Hides or displays navigation buttons at the top of drawer menu
     * Used when user taps on search icon to make the search field bigger
     * @param status true - hide navigation buttons, false - show navigation buttons
     */
    private void hideNavigation(boolean status) {
        ImageButton goBackButton = findViewById(R.id.navigation_drawer_button_back);
        ImageButton upButton = findViewById(R.id.navigation_drawer_button_up);
        ImageButton homeButton = findViewById(R.id.navigation_drawer_button_home);
        ImageButton bookmarksButton = findViewById(R.id.navigation_drawer_button_bookmarks);
        ImageButton createNode = findViewById(R.id.navigation_drawer_button_create_node);
        CheckBox excludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);
        if (status) {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.GONE);
            homeButton.setVisibility(View.GONE);
            bookmarksButton.setVisibility(View.GONE);
            createNode.setVisibility(View.GONE);
            excludeFromSearch.setVisibility(View.VISIBLE);
        } else {
            goBackButton.setVisibility(View.GONE);
            upButton.setVisibility(View.VISIBLE);
            homeButton.setVisibility(View.VISIBLE);
            bookmarksButton.setVisibility(View.VISIBLE);
            createNode.setVisibility(View.VISIBLE);
            excludeFromSearch.setVisibility(View.GONE);
        }
    }

    /**
     * Returns handler used to run task on main (UI) thread
     * @return handler to run task on the main loop
     */
    public Handler getHandler() {
        return this.handler;
    }

    /**
     * Returns ExecutorService to run tasks in the background
     * @return executor to run tasks in the background
     */
    public ExecutorService getExecutor() {
        return this.executor;
    }

    /**
     * Close findInNode view, keyboard and restores variables to initial values
     */
    private void closeFindInNode() {
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

    /**
     * Sets FindInNode UI
     * and prepares node content for search
     */
    private void openFindInNode() {
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

    /**
     * Sets the count of the results of
     * findInNode to new value
     * @param counter new result count
     */
    private void updateCounter(int counter) {
        TextView findInNodeEditTextCount = findViewById(R.id.find_in_node_edit_text_result_count);
        handler.post(new Runnable() {
            @Override
            public void run() {
                findInNodeEditTextCount.setText(String.valueOf(counter));
            }
        });
    }

    /**
     * Sets/updates index of currently marked result
     */
    private void updateMarkedIndex() {
        TextView findInNodeEditTextMarkedIndex = findViewById(R.id.find_in_node_edit_text_marked_index);
        handler.post(new Runnable() {
            @Override
            public void run() {
                findInNodeEditTextMarkedIndex.setText(String.valueOf(MainView.this.currentFindInNodeMarked + 1));
            }
        });
    }

    /**
     * Start or stops findInView progress bar
     * @param status true - start progress bar, false - stop progress bar
     */
    private void setFindInNodeProgressBar(Boolean status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = findViewById(R.id.find_in_node_progress_bar);
                progressBar.setIndeterminate(status);
            }
        });
    }

    /**
     * Searches for query in nodeContent
     * @param query search query
     */
    private void findInNode(String query) {
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

    /**
     * Highlights result from findInNodeResultStorage (array list) that is identified by currentFindInNodeMarked
     * @param resultIndex index of result to be highlighted
     */
    private void highlightFindInNodeResult(int resultIndex) {
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

    /**
     * Restores TextView to original state that was
     * changed with highlightFindInNodeResult() function
     */
    private void restoreHighlightedView() {
        // Uses index of the TextView in currentFindInNodeMarked
        // At the end sets currentFindInNodeMarked to 0 (nothing marked)
        // Resets counters and search result storage too
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        if (this.currentFindInNodeMarked != -1 && contentFragmentLinearLayout != null && this.mainViewModel.getFindInNodeResultStorage().size() > 0) {
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

    /**
     * Calculates next result that has to be highlighted
     * and initiates switchFindInNodeHighlight
     */
    private void findInNodeNext() {
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

    /**
     * Calculates previous result that has to be highlighted
     * and initiates switchFindInNodeHighlight
     */
    private void findInNodePrevious() {
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

    /**
     * Deals with attached/embedded files into database
     * @param nodeUniqueID unique ID of the node that has attached/embedded file
     * @param attachedFileFilename filename of the attached/embedded file
     * @param time timestamp that was saved to the database with the file
     */
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

    /**
     * Opens attached/embedded with the app on the device
     * @param fileMimeType mime type of the attached/embedded for the device to show relevant app list to open the file with
     * @param nodeUniqueID unique ID of the node that has attached/embedded file
     * @param filename filename of the attached/embedded file
     * @param time timestamp that was saved to the database with the file
     */
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

    /**
     * Launches activity to save the attached file to the device
     */
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

    /**
     * Activates node's action icon/right click dialog fragment
     * @param node node which action menu should be shown
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void openMenuItemActionDialogFragment(String[] node, int position) {
        DialogFragment menuItemActionDialogFragment = new MenuItemActionDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putStringArray("node", node);
        bundle.putInt("position", position);
        bundle.putBoolean("bookmarked", this.reader.isNodeBookmarked(node[1]));
        menuItemActionDialogFragment.setArguments(bundle);
        menuItemActionDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogFragment);
        menuItemActionDialogFragment.show(getSupportFragmentManager(), "menuItemActionDialogFragment");
    }

    /**
     * Displays create new node fragment
     * @param nodeUniqueID unique node ID of the node which action menu was launched
     * @param relation relation to the node selected. 0 - sibling, 1 - subnode
     */
    private void launchCreateNewNodeFragment(String nodeUniqueID, int relation) {
        Bundle bundle = new Bundle();
        bundle.putString("nodeUniqueID", nodeUniqueID);
        bundle.putInt("relation", relation);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, CreateNodeFragment.class, bundle, "createNode")
                .addToBackStack("createNode")
                .commit();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
    }

    /**
     * Creates new node in the database with provided parameters
     * @param nodeUniqueID unique ID of the node that new node will be created in relation with
     * @param relation relation to the node. 0 - sibling, 1 - subnode
     * @param name node name
     * @param progLang prog_lang value if the node. "custom-colors" - means rich text node, "plain-text" - plain text node and "sh" - for the rest
     * @param noSearchMe 0 - marks that node should be searched, 1 - marks that node should be excluded from the search
     * @param noSearchCh 0 - marks that subnodes of the node should be searched, 1 - marks that subnodes should be excluded from the search
     */
    public void createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh) {
        String[] newNodeMenuItem = this.reader.createNewNode(nodeUniqueID, relation, name, progLang, noSearchMe, noSearchCh);
        getSupportFragmentManager().popBackStack();
        MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
        if (newNodeMenuItem != null) {
            // If new node was added - load it
            if (this.bookmarksToggle) {
                this.bookmarksToggle = false;
                this.navigationNormalMode(true);
            }
            this.currentNode = newNodeMenuItem;
            this.loadNodeContent();
            this.setClickedItemInSubmenu();
            this.adapter.notifyDataSetChanged();
        }
    }

    /**
     * Adds node to bookmark list
     * @param nodeUniqueID unique ID of the node which to add to bookmarks
     */
    private void addNodeToBookmarks(String nodeUniqueID) {
        this.reader.addNodeToBookmarks(nodeUniqueID);
    }

    /**
     * Removes node from bookmark list
     * Updates drawer menu if bookmarks are being displayed
     * @param nodeUniqueID unique ID of the node which to remove from bookmarks
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void removeNodeFromBookmarks(String nodeUniqueID, int position) {
        this.reader.removeNodeFromBookmarks(nodeUniqueID);
        if (this.bookmarksToggle) {
            Iterator<String[]> iterator = this.mainViewModel.getNodes().iterator();

            while(iterator.hasNext()) {
                String[] node = iterator.next();
                if (node[1].equals(nodeUniqueID)) {
                    iterator.remove();
                    this.adapter.notifyItemRemoved(position);
                    break;
                }
            }
        }
    }

    /**
     * Displays move node fragment
     * @param node information of the node which action menu was launched
     */
    private void launchMoveNodeFragment(String[] node) {
        Bundle bundle = new Bundle();
        bundle.putStringArray("node", node);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, MoveNodeFragment.class, bundle, "moveNode")
                .addToBackStack("moveNode")
                .commit();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
    }

    /**
     * Moves node to different location of the document tree
     * @param targetNodeUniqueID unique ID of the node that user chose to move
     * @param destinationNodeUniqueID unique ID of the node that has to be a parent of the target node
     */
    public void moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        getSupportFragmentManager().popBackStack();
        MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
        this.reader.moveNode(targetNodeUniqueID, destinationNodeUniqueID);
        if (this.currentNode == null) {
            this.mainViewModel.setNodes(this.reader.getMainNodes());
        } else {
            this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.currentNode[1]));
        }
        this.resetMenuToCurrentNode();
    }

    /**
     * Deletes node from database, removes node from the drawer menu
     * if deleted node is not currently opened (selected)
     * or if node is currently opened reloads drawer menu to main menu,
     * removes nodeContent and resets action bar title
     * @param nodeUniqueID unique ID of the node to delete
     * @param position node's position in drawer menu as reported by adapter
     */
    private void deleteNode(String nodeUniqueID, int position) {
        this.reader.deleteNode(nodeUniqueID);
        if (this.currentNode != null && nodeUniqueID.equals(this.currentNode[1])) {
            // Currently displayed node was selected for deletion
            this.removeNodeContent();
        } else {
            // Another node in drawer menu was selected for deletion
            if (this.mainViewModel.getNodes().size() <= 2) {
                this.currentNode[2] = "false";
                this.resetMenuToCurrentNode();
            } else {
                this.mainViewModel.getNodes().remove(position);
                this.adapter.notifyItemRemoved(position);
            }
        }
    }

    /**
     * Opens a fragment with information about the node
     * @param nodeUniqueID unique ID of the node of which properties has to be shown
     * @param position node's position in drawer menu as reported by adapter
     */
    private void openNodeProperties(String nodeUniqueID, int position) {
        Bundle bundle = new Bundle();
        bundle.putString("nodeUniqueID", nodeUniqueID);
        bundle.putInt("position", position);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, NodePropertiesFragment.class, bundle, "moveNode")
                .addToBackStack("nodeProperties")
                .commit();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
    }

    /**
     * Updates node properties in the database
     * @param position node's position in drawer menu as reported by adapter
     * @param nodeUniqueID unique ID of the node for which properties has to be updated
     * @param name new name of the node
     * @param progLang new node type
     * @param noSearchMe 1 - to exclude node from searches, 0 - keep node searches
     * @param noSearchCh 1 - to exclude subnodes of the node from searches, 0 - keep subnodes of the node in searches
     * @param reloadNodeContent true - reload node content fragment after changing data, false - do nothing
     */
    public void updateNodeProperties(int position, String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh, boolean reloadNodeContent) {
        getSupportFragmentManager().popBackStack();
        MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
        this.getReader().updateNodeProperties(nodeUniqueID, name, progLang, noSearchMe, noSearchCh);
        mainViewModel.getNodes().get(position)[0] = name;
        this.adapter.notifyItemChanged(position);
        if (this.currentNode != null && mainViewModel.getNodes().get(position)[1].equals(this.currentNode[1])) {
            // If opened node was changed - reloads node name in toolbar
            // and reloads node content if reloadNodeContent is true
            this.currentNode[0] = name;
            this.setToolbarTitle(this.currentNode[0]);
            if (reloadNodeContent) {
                this.loadNodeContent();
            }
        }
    }

    /**
     * Opens node content editor in a different fragment
     * Disables drawer menu and changes hamburger menu icon to
     * home button
     */
    private void openNodeEditor() {
        Bundle bundle = new Bundle();
        bundle.putString("nodeUniqueID", this.currentNode[1]);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, NodeEditorFragment.class, bundle, "editNode")
                .addToBackStack("editNode")
                .commit();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Function used when closing NodeEditorFragment
     * depending on passed boolean variable displayed node content
     * will be reloaded or not.
     * Changes home button to hamburger button in toolbar
     * @param reloadContent true - reload node content
     */
    public void returnFromFragmentWithHomeButton(boolean reloadContent) {
        if (reloadContent) {
            this.loadNodeContent();
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
    }

    /**
     * Function used when closing Fragment
     * that only has home button in toolbar
     */
    public void returnFromFragmentWithHomeButton() {
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
    }

    /**
     * Function used when closing Fragment
     * sets toolbar title to currently opened node name
     */
    public void returnFromFragmentWithHomeButtonAndRestoreTitle() {
        if (this.currentNode != null) {
            this.setToolbarTitle(this.currentNode[0]);
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
    }

    /**
     * Function to launch fragment with enlarged image
     * @param nodeUniqueID unique ID of the node that image is embedded into
     * @param imageOffset offset of the image in the node content
     */
    public void openImageView(String nodeUniqueID, String imageOffset) {
        Bundle bundle = new Bundle();
        bundle.putString("type", "image");
        bundle.putString("nodeUniqueID", nodeUniqueID);
        bundle.putString("imageOffset", imageOffset);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, ImageViewFragment.class, bundle, "imageView")
                .addToBackStack("imageView")
                .commit();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Function to launch fragment with enlarged latex image
     * @param latexString latex code extracted from the database
     */
    public void openImageView(String latexString) {
        Bundle bundle = new Bundle();
        bundle.putString("type", "latex");
        bundle.putString("latexString", latexString);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, ImageViewFragment.class, bundle, "imageView")
                .addToBackStack("imageView")
                .commit();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Opens search in a different fragment
     * Sets toolbar's title to "Search"
     */
    private void openSearch() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, SearchFragment.class, null, "search")
                .addToBackStack("search")
                .commit();
        this.setToolbarTitle("Search");
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Opens node that was passed as an argument
     * Used to open search results
     * @param selectedNode String[] of the node that has to be oppend
     */
    public void openSearchResult(String[] selectedNode) {
        this.currentNode = selectedNode;
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
        this.resetMenuToCurrentNode();
        this.loadNodeContent();
    }

    private void exportPdfSetup() {
        // Sets the intent for asking user to choose a location where to save a file
        if (this.currentNode != null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, this.currentNode[0]);
            exportPdf.launch(intent);
        } else {
            Toast.makeText(this, R.string.toast_error_please_select_node, Toast.LENGTH_SHORT).show();
        }
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
                        // If it is a table - TableLayout has to be drawn to canvas and not ScrollView
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

    /**
     * Sets up database for export
     * User can be prompted to choose file location
     * Or confirm to overwrite the newer
     * Mirror database file
     */
    public void exportDatabaseSetup() {
        // XML without password already saved in external file
        if (this.sharedPreferences.getString("databaseFileExtension", null).equals("ctd")) {
            Toast.makeText(this, R.string.toast_message_not_password_protected_xml_saves_changes_externally, Toast.LENGTH_SHORT).show();
            return;
        }

        if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
            // If user uses MirrorDatabase
            // Variables that will be put into bundle for MirrorDatabaseProgressDialogFragment
            Uri mirrorDatabaseFileUri = null; // Uri to the Mirror Database File inside Mirror Database Folder
            long mirrorDatabaseDacumentFileLastModified = 0;

            // Reading through files inside Mirror Database Folder
            Uri mirrorDatabaseFolderUri = Uri.parse(this.sharedPreferences.getString("mirrorDatabaseFolderUri", null));
            Uri mirrorDatabaseFolderChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDatabaseFolderUri, DocumentsContract.getTreeDocumentId(mirrorDatabaseFolderUri));

            Cursor cursor = this.getContentResolver().query(mirrorDatabaseFolderChildrenUri, new String[]{"document_id", "_display_name", "last_modified"}, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                if (cursor.getString(1).equals(this.sharedPreferences.getString("mirrorDatabaseFilename", null))) {
                    // if file with the Mirror Database File filename was wound inside Mirror Database Folder
                    mirrorDatabaseFileUri = DocumentsContract.buildDocumentUriUsingTree(mirrorDatabaseFolderUri, cursor.getString(0));
                    mirrorDatabaseDacumentFileLastModified = cursor.getLong(2);
                    break;
                }
            }
            cursor.close();

            // If found Mirror Database File is older or the same as the last time it was synchronized
            // copying is done immediately
            if (mirrorDatabaseDacumentFileLastModified <= this.sharedPreferences.getLong("mirrorDatabaseLastModified", 0)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("exportFileUri", mirrorDatabaseFileUri.toString());
                    ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
                    exportDatabaseDialogFragment.setArguments(bundle);
                    exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
            } else {
                // If found Mirror Database File is newer that the last time it was synchronized
                // User is prompted to choose to cancel or continue
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_dialog_warning_newer_mirror_database_will_be_overwritten_title);
                builder.setMessage(R.string.alert_dialog_warning_newer_mirror_database_will_be_overwritten_message);
                builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                Uri finalMirrorDatabaseFileUri = mirrorDatabaseFileUri;
                long finalMirrorDatabaseDocumentFileLastModified = mirrorDatabaseDacumentFileLastModified;
                builder.setPositiveButton(R.string.button_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Saving new last modified date to preferences
                        SharedPreferences.Editor sharedPreferencesEditor = MainView.this.sharedPreferences.edit();
                        sharedPreferencesEditor.putLong("mirrorDatabaseLastModified", finalMirrorDatabaseDocumentFileLastModified);
                        sharedPreferencesEditor.apply();
                        // Launching copying dialog
                        Bundle bundle = new Bundle();
                        bundle.putString("exportFileUri", finalMirrorDatabaseFileUri.toString());
                        ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
                        exportDatabaseDialogFragment.setArguments(bundle);
                        exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
                    }
                });
                builder.show();
            }
        } else {
            // If MirrorDatabase isn't turned on
            this.exportDatabaseToFile.launch(this.sharedPreferences.getString("databaseFilename", null));
        }
    }

    /**
     * Launches file chooser to select location
     * where to export database. If user chooses a file - launches a
     * export dialog fragment
     */
    ActivityResultLauncher<String> exportDatabaseToFile = registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), result -> {
        if (result != null) {
            Bundle bundle = new Bundle();
            bundle.putString("exportFileUri", result.toString());
            ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
            exportDatabaseDialogFragment.setArguments(bundle);
            exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
        }
    });

}