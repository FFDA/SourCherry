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

import static lt.ffda.sourcherry.fragments.NodeContentFragment.CONTENT_FRAGMENT_LINEARLAYOUT;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.database.MultiDbFileShare;
import lt.ffda.sourcherry.database.MultiReader;
import lt.ffda.sourcherry.dialogs.ExportDatabaseDialogFragment;
import lt.ffda.sourcherry.dialogs.MenuItemActionDialogFragment;
import lt.ffda.sourcherry.dialogs.SaveOpenDialogFragment;
import lt.ffda.sourcherry.fragments.CreateNodeFragment;
import lt.ffda.sourcherry.fragments.ImageViewFragment;
import lt.ffda.sourcherry.fragments.MoveNodeFragment;
import lt.ffda.sourcherry.fragments.NodeContentEditorFragment;
import lt.ffda.sourcherry.fragments.NodeContentFragment;
import lt.ffda.sourcherry.fragments.NodePropertiesFragment;
import lt.ffda.sourcherry.fragments.SearchFragment;
import lt.ffda.sourcherry.model.FileInfo;
import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.preferences.PreferencesActivity;
import lt.ffda.sourcherry.runnables.CollectNodesBackgroundRunnable;
import lt.ffda.sourcherry.runnables.FindInNodeRunnable;
import lt.ffda.sourcherry.runnables.FindInNodeRunnableCallback;
import lt.ffda.sourcherry.runnables.NodesCollectedCallback;
import lt.ffda.sourcherry.services.DatabaseExportService;
import lt.ffda.sourcherry.utils.DatabaseType;
import lt.ffda.sourcherry.utils.Files;
import lt.ffda.sourcherry.utils.MenuItemAction;
import lt.ffda.sourcherry.utils.ReturnSelectedFileUriForSaving;

public class MainView extends AppCompatActivity {

    ActivityResultLauncher<String> exportDatabaseToFile = registerExportDatabaseDialogFragment();
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private MenuItemAdapter adapter;
    private boolean bookmarksToggle; // To save state for bookmarks. True means bookmarks are being displayed
    private int currentFindInNodeMarked; // Index of the result that is marked from FindInNode results. -1 Means nothing is selected
    private int currentNodePosition; // In menu / MenuItemAdapter for marking menu item opened/selected
    private DrawerLayout drawerLayout;
    private ScheduledThreadPoolExecutor executor;
    private boolean filterNodeToggle;
    private boolean findInNodeToggle; // Holds true when FindInNode view is initiated
    private Handler handler;
    private MainViewModel mainViewModel;
    ActivityResultLauncher<Intent> exportPdf = registerExportPdf();
    private DatabaseReader reader;
    ActivityResultLauncher<String[]> saveFile = registerSaveFile();
    private SharedPreferences sharedPreferences;
    private int tempCurrentNodePosition; // Needed to save selected node position when user opens bookmarks;

    /**
     * Adds node to bookmark list
     * @param nodeUniqueID unique ID of the node which to add to bookmarks
     */
    private void addNodeToBookmarks(String nodeUniqueID) {
        reader.addNodeToBookmarks(nodeUniqueID);
    }

    /**
     * Sets variables that were used to display bookmarks to their default values
     */
    private void bookmarkVariablesReset() {
        mainViewModel.resetTempNodes();
        tempCurrentNodePosition = -1;
        bookmarksToggle = false;
    }

    /**
     * Restoring saved node status
     */
    private void closeBookmarks() {
        mainViewModel.restoreSavedCurrentNodes();
        currentNodePosition = tempCurrentNodePosition;
        adapter.markItemSelected(currentNodePosition);
        adapter.notifyDataSetChanged();
        navigationNormalMode(true);
        bookmarkVariablesReset();
    }

    /**
     * Close findInNode view, keyboard and restores variables to initial values
     */
    private void closeFindInNode() {
        // * This prevents crashes when user makes a sudden decision to close findInNode view while last search hasn't finished
        handler.removeCallbacksAndMessages(null);
        findInNodeToggle = false;
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);
        findInNodeEditText.setText("");
        findInNodeEditText.clearFocus();

        restoreHighlightedView();

        // * Closing keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Shows keyboard on API 30 (Android 11) reliably
            WindowCompat.getInsetsController(getWindow(), findInNodeEditText).hide(WindowInsetsCompat.Type.ime());
        } else {
            handler.postDelayed(new Runnable() {
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
        mainViewModel.findInNodeStorageToggle(false);
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
        ScNode newNodeMenuItem = reader.createNewNode(nodeUniqueID, relation, name, progLang, noSearchMe, noSearchCh);
        getSupportFragmentManager().popBackStack();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
        if (newNodeMenuItem != null) {
            // If new node was added - load it
            if (bookmarksToggle) {
                bookmarksToggle = false;
                navigationNormalMode(true);
            }
            mainViewModel.setCurrentNode(newNodeMenuItem);
            loadNodeContent();
            resetMenuToCurrentNode();
        }
    }

    /**
     * Launches CreateNewNode to create node in currently opened menu
     * Node will be appended to the end of it
     * @param view view that was clicked by the user
     */
    public void createNode(View view) {
        if (mainViewModel.getNodes().isEmpty() || !mainViewModel.getNodes().get(0).isParent()) {
            launchCreateNewNodeFragment("0", 1);
        } else {
            if (mainViewModel.getNodes().get(0).isParent()) {
                launchCreateNewNodeFragment(mainViewModel.getNodes().get(0).getUniqueId(), 1);
            } else {
                launchCreateNewNodeFragment(mainViewModel.getNodes().get(0).getUniqueId(), 1);
            }
        }
    }

    /**
     * Deletes node from database, removes node from the drawer menu
     * or loads another one that's more appropriate,
     * removes nodeContent and resets action bar title if opened node was deleted
     * @param nodeUniqueID unique ID of the node to delete
     */
    private void deleteNode(String nodeUniqueID) {
        if (filterNodeToggle) {
            // Necessary, otherwise it will show up again in other searches until
            // the search function is turn off and on again
            removeNodeFromTempSearchNodes(nodeUniqueID);
        }
        if (bookmarksToggle) {
            // In case deleted node was in the drawer menu that user opened bookmarks form
            // If user comes back to that menu (exists bookmarks) without selecting a
            // bookmarked node to open - deleted node would be visible
            removeNodeFromTempNodes(nodeUniqueID);
        }
        if (mainViewModel.getCurrentNode() != null && nodeUniqueID.equals(mainViewModel.getCurrentNode().getUniqueId())) {
            // Currently opened node was selected for deletion
            String parentNodeUniqueID = reader.getParentNodeUniqueID(nodeUniqueID);
            reader.deleteNode(nodeUniqueID);
            mainViewModel.deleteNodeContent();
            setToolbarTitle(getString(R.string.app_name));
            currentNodePosition = RecyclerView.NO_POSITION;
            adapter.markItemSelected(currentNodePosition);
            mainViewModel.setCurrentNode(null);
            if (parentNodeUniqueID == null) {
                mainViewModel.setNodes(reader.getMainNodes());
            } else {
                if (reader.getChildrenNodeCount(parentNodeUniqueID) > 0) {
                    mainViewModel.setNodes(reader.getMenu(parentNodeUniqueID));
                } else {
                    mainViewModel.setNodes(reader.getParentWithSubnodes(parentNodeUniqueID));
                }
            }
            adapter.notifyDataSetChanged();
        } else if (mainViewModel.getCurrentNode() != null){
            reader.deleteNode(nodeUniqueID);
            ArrayList<ScNode> newMenu;
            if (mainViewModel.getNodes().size() < 2) {
                newMenu = reader.getMenu(mainViewModel.getCurrentNode().getUniqueId());
            } else {
                newMenu = reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId());
            }
            if (mainViewModel.getCurrentNode() != null) {
                // If node is opened it has to be selected as such
                int newPosition = -1;
                for (int i = 0; i < newMenu.size(); i++) {
                    if (newMenu.get(i).getUniqueId().equals(mainViewModel.getNodes().get(0).getUniqueId())) {
                        newMenu.get(i).setHasSubnodes(false);
                    }
                    if (mainViewModel.getCurrentNode() != null) {
                        if (newMenu.get(i).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                            newPosition = i;
                        }
                    }
                }
                adapter.markItemSelected(newPosition);
                currentNodePosition = newPosition;
            }
            mainViewModel.setNodes(newMenu);
            adapter.notifyDataSetChanged();
        } else {
            reader.deleteNode(nodeUniqueID);
            mainViewModel.setNodes(reader.getMainNodes());
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Prepares DrawerLayout for fragments that should not allow user to open the drawer
     * Shows back (home) arrow instead of hamburger icon
     */
    public void disableDrawerMenu() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Displays toast message on the main thread
     * @param message message to show
     */
    private void displayToastOnMainThread(String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainView.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Unlocks drawer menu and shows it
     */
    public void enableDrawer() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
    }

    /**
     * Exists MainView activity with Toast message
     * telling user that error occurred while reading the database
     */
    public void exitWithError() {
        Toast.makeText(this, R.string.toast_error_cant_read_database, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Sets up database for export
     * User can be prompted to choose file location
     * Or confirm to overwrite the newer
     * Mirror database file
     */
    public void exportDatabaseSetup() {
        // XML without password already saved in external file
        if (sharedPreferences.getString("databaseFileExtension", null).equals("ctd") && sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
            Toast.makeText(this, R.string.toast_message_not_password_protected_xml_saves_changes_externally, Toast.LENGTH_SHORT).show();
            return;
        }
        if (sharedPreferences.getBoolean("mirror_database_switch", false)) {
            // If user uses MirrorDatabase
            // Variables that will be put into bundle for MirrorDatabaseProgressDialogFragment
            FileInfo data = Files.getFileUriAndModDate(getContentResolver(), sharedPreferences.getString("mirrorDatabaseFolderUri", null), sharedPreferences.getString("mirrorDatabaseFilename", null));

            if (data.getUri() == null) {
                Toast.makeText(this, R.string.toast_error_failed_to_find_mirror_database, Toast.LENGTH_SHORT).show();
                return;
            }

            // If found Mirror Database File is older or the same as the last time it was synchronized
            // copying is done immediately
            if (data.getModified() <= sharedPreferences.getLong("mirrorDatabaseLastModified", 0)) {
                Bundle bundle = new Bundle();
                bundle.putString("exportFileUri", data.getUri().toString());
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
                builder.setPositiveButton(R.string.button_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Saving new last modified date to preferences
                        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                        sharedPreferencesEditor.putLong("mirrorDatabaseLastModified", data.getModified());
                        sharedPreferencesEditor.apply();
                        // Launching copying dialog
                        Bundle bundle = new Bundle();
                        bundle.putString("exportFileUri", data.getUri().toString());
                        ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
                        exportDatabaseDialogFragment.setArguments(bundle);
                        exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
                    }
                });
                builder.show();
            }
        } else {
            // If MirrorDatabase isn't turned on
            exportDatabaseToFile.launch(sharedPreferences.getString("databaseFilename", null));
        }
    }

    private void exportPdfSetup() {
        // Sets the intent for asking user to choose a location where to save a file
        if (mainViewModel.getCurrentNode() != null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, mainViewModel.getCurrentNode().getName());
            exportPdf.launch(intent);
        } else {
            Toast.makeText(this, R.string.toast_error_please_select_node, Toast.LENGTH_SHORT).show();
        }
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
     * Filters node list by the name of the node
     * Changes the drawer menu item list to show only
     * nodes with matching text in the node title.
     * Search is case insensitive
     * @param query search query
     */
    private void filterNodes(String query) {
        mainViewModel.setNodes(mainViewModel.getTempSearchNodes());
        ArrayList<ScNode> filteredNodes = mainViewModel.getNodes().stream()
                .filter(node -> node.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
        mainViewModel.setNodes(filteredNodes);
        adapter.notifyDataSetChanged();
    }

    /**
     * Searches for query in nodeContent
     * @param query search query
     */
    private void findInNode(String query) {
        if (!query.isEmpty()) {
            // If new query is longer when one character
            restoreHighlightedView();
            executor.submit(new FindInNodeRunnable(mainViewModel, query, new FindInNodeRunnableCallback() {
                @Override
                public void searchFinished() {
                    setFindInNodeProgressBar(false);
                    updateCounter(mainViewModel.getFindInNodeResultCount());
                    if (mainViewModel.getFindInNodeResultCount() == 0) {
                        // If user types until there are no matches left
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                restoreHighlightedView();
                            }
                        });
                    } else {
                        // If there are matches for user query
                        // First result has to be highlighter and scrolled too
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                currentFindInNodeMarked = 0;
                                highlightFindInNodeResult();
                            }
                        });
                    }
                }

                @Override
                public void searchStarted() {
                    setFindInNodeProgressBar(true);
                }
            }));
        } else {
            // If new query is 0 characters long, that means that user deleted everything and view should be reset to original
            restoreHighlightedView();
        }
    }

    /**
     * Calculates next result that has to be highlighted and initiates switchFindInNodeHighlight
     */
    private void findInNodeNext() {
        currentFindInNodeMarked++;
        updateMarkedIndex();
        if (currentFindInNodeMarked <= mainViewModel.getFindInNodeResultCount() - 1) {
            int previouslyHighlightedFindInNode;
            if (currentFindInNodeMarked == 0) {
                // Current marked node is first in the array, so previous marked should be the last from array
                previouslyHighlightedFindInNode = mainViewModel.getFindInNodeResult(mainViewModel.getFindInNodeResultCount() - 1)[0];
            } else {
                // Otherwise it should be previous one in array. However, it can be that it is out off array if array is made of one item.
                previouslyHighlightedFindInNode = mainViewModel.getFindInNodeResult(currentFindInNodeMarked - 1)[0];
            }
            // Gets instance of the fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
            nodeContentFragment.switchFindInNodeHighlight(previouslyHighlightedFindInNode, currentFindInNodeMarked);
        } else {
            // Reached the last index of the result array
            // currentFindInNodeMarked has to be reset to the first index if the result array and this function restarted
            // If you want to though the result in a loop
            currentFindInNodeMarked = -1;
            findInNodeNext();
        }
    }

    /**
     * Calculates previous result that has to be highlighted
     * and initiates switchFindInNodeHighlight
     */
    private void findInNodePrevious() {
        currentFindInNodeMarked--;
        updateMarkedIndex();
        if (currentFindInNodeMarked >= 0) {
            int previouslyHighlightedFindInNode;
            if (currentFindInNodeMarked == mainViewModel.getFindInNodeResultCount() - 1) {
                // Current marked node is last, so previous marked node should be the first in result ArrayList
                previouslyHighlightedFindInNode = mainViewModel.getFindInNodeResult(0)[0];
            } else {
                // Otherwise it should next one in array (index+1). However, it can be that it is out off array if array is made of one item
                previouslyHighlightedFindInNode = mainViewModel.getFindInNodeResult(currentFindInNodeMarked + 1)[0]; // Saved index for the view
            }
            // Gets instance of the fragment
            FragmentManager fragmentManager = getSupportFragmentManager();
            NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
            nodeContentFragment.switchFindInNodeHighlight(previouslyHighlightedFindInNode, currentFindInNodeMarked);
        } else {
            // Reached the first index of the result array
            // currentFindInNodeMarked has to be reset to last index of the result array and this function restarted
            // If you want to though the result in a loop
            currentFindInNodeMarked = mainViewModel.getFindInNodeResultCount();
            findInNodePrevious();
        }
    }

    /**
     * Returns findInNodeToggle value;
     * @return findInNodeToggle value;
     */
    public boolean getFindInNodeToggle() {
        return findInNodeToggle;
    }

    /**
     * Returns MainViewModel used to store all information of the app (DrawerMenu nodes, NodeContent)
     * @return MainViewModel
     */
    public MainViewModel getMainViewModel() {
        return mainViewModel;
    }

    /**
     * Closes bookmarks in drawer menu
     * @param view view needed to associated button with action
     */
    public void goBack(View view) {
        closeBookmarks();
    }

    /**
     * Reloads drawer menu to show main menu
     * if it is not displayed
     * otherwise shows a message to the user
     * that the top of the database tree was already reached
     * @param view view that was clicked
     */
    public void goHome(View view) {
        ArrayList<ScNode> tempMainNodes = reader.getMainNodes();
        // Compares node sizes, first and last node's uniqueIDs in both arrays
        if (tempMainNodes.size() == mainViewModel.getNodes().size() && tempMainNodes.get(0).getUniqueId().equals(mainViewModel.getNodes().get(0).getUniqueId()) && tempMainNodes.get(mainViewModel.getNodes().size() - 1).getUniqueId().equals(mainViewModel.getNodes().get(mainViewModel.getNodes().size() - 1).getUniqueId())) {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        } else {
            mainViewModel.setNodes(tempMainNodes);
            currentNodePosition = -1;
            if (bookmarksToggle && mainViewModel.getCurrentNode() != null) {
                // Just in case user chose to come back from bookmarks to home and a node is selected
                // it might me that selected node is in main menu
                // this part checks for that and marks the node if it finds it
                currentNodePosition = openedNodePositionInDrawerMenu();
            }
            adapter.markItemSelected(currentNodePosition);
            adapter.notifyDataSetChanged();
        }

        if (bookmarksToggle) {
            navigationNormalMode(true);
            bookmarkVariablesReset();
        }
    }

    /**
     * Moves navigation menu one node up
     * If menu is already at the top it shows a message to the user
     * @param view view that was clicked
     */
    public void goNodeUp(View view) {
        ArrayList<ScNode> nodes = reader.getParentWithSubnodes(mainViewModel.getNodes().get(0).getUniqueId());
        if (nodes != null && nodes.size() != mainViewModel.getNodes().size()) {
            // If retrieved nodes are not null and array size do not match the one displayed
            // it is definitely not the same node so it can go up
            mainViewModel.setNodes(nodes);
            currentNodePosition = -1;
            adapter.markItemSelected(currentNodePosition);
            adapter.notifyDataSetChanged();
        } else {
            // If both nodes arrays matches in size it might be the same node (especially main/top)
            // This part checks if first and last nodes in arrays matches by comparing nodeUniqueID of both
            if (nodes != null && nodes.get(0).getUniqueId().equals(mainViewModel.getNodes().get(0).getUniqueId()) && nodes.get(nodes.size() -1 ).getUniqueId().equals(mainViewModel.getNodes().get(mainViewModel.getNodes().size() -1 ).getUniqueId())) {
                Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
            } else {
                mainViewModel.setNodes(nodes);
                currentNodePosition = -1;
                adapter.markItemSelected(currentNodePosition);
                adapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Prepares DrawerLayout for fragments that should not allow user to open the drawer
     * Hide DrawerLayout completely
     */
    private void hideDrawerMenu() {
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
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
     * Highlights all results of the FindInNode search in light red color and call a method that
     * highlights first result in darker red color. Calls methods that updates other FindInNOde
     * UI elements.
     */
    private void highlightFindInNodeResult() {
        mainViewModel.findInNodeStorageReset();
        LinearLayout contentFragmentLinearLayout = findViewById(CONTENT_FRAGMENT_LINEARLAYOUT);
        int counter = 0; // Iterator of the all the saved views from node content
        int resultCounter = 0;
        int[] currentResult;
        for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
            View view = contentFragmentLinearLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView currentTextView = (TextView) view;
                SpannableStringBuilder currentTextViewContent = mainViewModel.getfindInNodeStorageContent(counter);
                while (resultCounter < mainViewModel.getFindInNodeResultCount() && (currentResult = mainViewModel.getFindInNodeResult(resultCounter))[0] == counter) {
                    currentTextViewContent.setSpan(new BackgroundColorSpan(getColor(R.color.cherry_red_100)), currentResult[1], currentResult[2], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    resultCounter++;
                }
                currentTextView.setText(currentTextViewContent);
                counter++;
            } else if (view instanceof HorizontalScrollView) {
                TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                for (int row = 0; row < tableLayout.getChildCount(); row++) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                    for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                        TextView currentCell = (TextView) tableRow.getChildAt(cell);
                        SpannableStringBuilder currentTextViewContent = mainViewModel.getfindInNodeStorageContent(counter);
                        while (resultCounter < mainViewModel.getFindInNodeResultCount() && (currentResult = mainViewModel.getFindInNodeResult(resultCounter))[0] == counter) {
                            currentTextViewContent.setSpan(new BackgroundColorSpan(getColor(R.color.cherry_red_100)), currentResult[1], currentResult[2], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            resultCounter++;
                        }
                        currentCell.setText(currentTextViewContent);
                        counter++;
                    }
                }
            }
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
        nodeContentFragment.switchFindInNodeHighlight(-1, currentFindInNodeMarked);
        updateMarkedIndex();
    }

    /**
     * Initiates all the listeners for DrawerMenu node filter function
     * @param searchView DrawerMenu's search view
     */
    private void initDrawerMenuFilter(SearchView searchView) {
        CheckBox checkBoxExcludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);
        checkBoxExcludeFromSearch.setChecked(sharedPreferences.getBoolean("exclude_from_search", false));
        checkBoxExcludeFromSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (filterNodeToggle) {
                    // Gets new menu list only if filter mode is activated
                    mainViewModel.setTempSearchNodes(reader.getAllNodes(isChecked));
                    adapter.notifyDataSetChanged();
                    searchView.requestFocus();
                    filterNodes(searchView.getQuery().toString());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("exclude_from_search", isChecked);
                    editor.commit();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (filterNodeToggle) { // This check fixes bug where all database's nodes were displayed after screen rotation
                    filterNodes(newText);
                }
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            // When the user closes search view without selecting a node
            @Override
            public boolean onClose() {
                if (mainViewModel.getCurrentNode() != null) {
                    mainViewModel.restoreSavedCurrentNodes();
                    currentNodePosition = tempCurrentNodePosition;
                    adapter.markItemSelected(currentNodePosition);
                    adapter.notifyDataSetChanged();
                } else {
                    // If there is no node selected that means that main menu has to be loaded
                    mainViewModel.setNodes(reader.getMainNodes());
                }
                hideNavigation(false);
                mainViewModel.tempSearchNodesToggle(false);
                filterNodeToggle = false;
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
                    navigationNormalMode(true);
                    bookmarksToggle = false;
                } else {
                    // If search was selected from normal menu current menu items, selected item have to be saved
                    mainViewModel.saveCurrentNodes();
                    tempCurrentNodePosition = currentNodePosition;
                    currentNodePosition = -1;
                    adapter.markItemSelected(currentNodePosition); // Removing selection from menu item
                }
                hideNavigation(true);
                mainViewModel.setNodes(reader.getAllNodes(checkBoxExcludeFromSearch.isChecked()));
                mainViewModel.tempSearchNodesToggle(true);
                filterNodeToggle = true;
                adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Initiates listeners for clicks on DrawerMenu items and logic
     * @param searchView DrawerMenu's search view
     */
    private void initDrawerMenuNavigation(SearchView searchView) {
        adapter = new MenuItemAdapter(mainViewModel.getNodes(), this);
        adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (mainViewModel.getCurrentNode() == null || !mainViewModel.getNodes().get(position).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                    // If current node is null (empty/nothing opened yet) or selected nodeUniqueID is not the same as currently opened node
                    mainViewModel.setCurrentNode(mainViewModel.getNodes().get(position));
                    loadNodeContent();
                    if (mainViewModel.getNodes().get(position).hasSubnodes()) { // Checks if node is marked to have subnodes
                        // In this case it does not matter if node was selected from normal menu, bookmarks or search
                        if (filterNodeToggle) {
                            searchView.onActionViewCollapsed();
                            hideNavigation(false);
                            filterNodeToggle = false;
                        }
                        openSubmenu();
                    } else {
                        if (sharedPreferences.getBoolean("auto_open", false)) {
                            drawerLayout.close();
                        }
                        if (bookmarksToggle) {
                            // If node was selected from bookmarks
                            setClickedItemInSubmenu();
                        } else if (filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            hideNavigation(false);
                            setClickedItemInSubmenu();
                            filterNodeToggle = false;
                        } else {
                            // Node selected from normal menu
                            int previousNodePosition = currentNodePosition;
                            currentNodePosition = position;
                            adapter.markItemSelected(currentNodePosition);
                            adapter.notifyItemChanged(previousNodePosition);
                            adapter.notifyItemChanged(position);
                        }
                    }
                    if (bookmarksToggle) {
                        navigationNormalMode(true);
                        bookmarkVariablesReset();
                    }
                } else {
                    // If already opened node was selected by the user
                    // Helps to save some reads from database and reloading of navigation menu
                    if (sharedPreferences.getBoolean("auto_open", false)) {
                        drawerLayout.close();
                    }
                    if (mainViewModel.getNodes().get(position).hasSubnodes()) { // Checks if node is marked as having subnodes
                        if (filterNodeToggle) {
                            searchView.onActionViewCollapsed();
                            hideNavigation(false);
                            filterNodeToggle = false;
                        }
                        openSubmenu();
                    } else {
                        if (bookmarksToggle) {
                            // If node was selected from bookmarks
                            setClickedItemInSubmenu();
                        } else if (filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            hideNavigation(false);
                            setClickedItemInSubmenu();
                            filterNodeToggle = false;
                        }
                    }
                    if (bookmarksToggle) {
                        navigationNormalMode(true);
                        bookmarkVariablesReset();
                    }
                }
            }
        });

        // Listener for long click on drawer menu item
        adapter.setOnLongClickListener(new MenuItemAdapter.OnLongClickListener() {
            @Override
            public void onLongClick(View itemView, int position) {
                openMenuItemActionDialogFragment(mainViewModel.getNodes().get(position), position);
            }
        });

        // Listener for click on drawer menu item's action icon
        adapter.setOnItemActionMenuClickListener(new MenuItemAdapter.OnActionIconClickListener() {
            @Override
            public void onActionIconClick(View itemView, int position) {
                openMenuItemActionDialogFragment(mainViewModel.getNodes().get(position), position);
            }
        });

    }

    /**
     * Initiates all listeners for find in node functionality that allows user to search for
     * text in currently opened node
     */
    private void initFindInNode() {
        // Listener for FindInNode search text change
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);
        findInNodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // After user types the text in search field
                // There is a delay of 400 milliseconds
                // to start the search only when user stops typing
                if (findInNodeToggle && findInNodeEditText.isFocused()) {
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            findInNode(s.toString());
                        }
                    }, 400);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        // Listener for FindInNode "enter" button click
        // Moves to the next findInNode result
        findInNodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_NEXT && mainViewModel.getFindInNodeResultCount() > 1) {
                    findInNodeNext();
                    handled = true;
                }
                return handled;
            }
        });
        // Button in findInView to close it
        ImageButton findInNodeCloseButton = findViewById(R.id.find_in_node_button_close);
        findInNodeCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFindInNode();
            }
        });
        // Button in findInView to jump/show next result
        ImageButton findInNodeButtonNext = findViewById(R.id.find_in_node_button_next);
        findInNodeButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only if there are more than one result
                if (mainViewModel.getFindInNodeResultCount() > 1) {
                    findInNodeNext();
                }
            }
        });
        // Button in findInView to jump/show previous result
        ImageButton findInNodeButtonPrevious = findViewById(R.id.find_in_node_button_previous);
        findInNodeButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only if there are more than one result
                if (mainViewModel.getFindInNodeResultCount() > 1) {
                    findInNodePrevious();
                }
            }
        });
    }

    /**
     * Sets all necessery view insets for them to not overlap
     * @param toolbar apps toolbar
     */
    private void insetSetup(Toolbar toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view_find_in_node_linear_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets insetsIme = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), Math.max(insets.bottom, insetsIme.bottom));

            // Reapply fragment insets
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_view_fragment);
            if (currentFragment != null && currentFragment.getView() != null) {
                View contentFragmentLinearLayout = currentFragment.getView().findViewById(CONTENT_FRAGMENT_LINEARLAYOUT);
                if (contentFragmentLinearLayout != null) {
                    ViewCompat.requestApplyInsets(contentFragmentLinearLayout);
                }
            }
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigationView), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });
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
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, CreateNodeFragment.class, bundle, "createNode")
                .addToBackStack("createNode")
                .commit();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
    }

    /**
     * Displays move node fragment
     * @param node information of the node which action menu was launched
     */
    private void launchMoveNodeFragment(ScNode node) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("node", node);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, MoveNodeFragment.class, bundle, "moveNode")
                .addToBackStack("moveNode")
                .commit();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        getSupportActionBar().hide(); // Hides action bar
    }

    private void loadNodeContent() {
        setToolbarTitle(mainViewModel.getCurrentNode().getName());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if ("0".equals(mainViewModel.getCurrentNode().getMasterId())) {
                    reader.loadNodeContent(mainViewModel.getCurrentNode().getUniqueId());
                } else {
                    reader.loadNodeContent(mainViewModel.getCurrentNode().getMasterId());
                }
            }
        });
    }

    /**
     * Moves node to different location of the document tree
     * @param targetNodeUniqueID unique ID of the node that user chose to move
     * @param destinationNodeUniqueID unique ID of the node that has to be a parent of the target node
     */
    public void moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        getSupportFragmentManager().popBackStack();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        drawerLayout.open();
        getSupportActionBar().show();
        if (reader.moveNode(targetNodeUniqueID, destinationNodeUniqueID)) {
            if (mainViewModel.getCurrentNode() == null) {
                int targetNodePosition = mainViewModel.getNodePositionInMenu(targetNodeUniqueID);
                int destinationNodePosition = mainViewModel.getNodePositionInMenu(destinationNodeUniqueID);
                mainViewModel.getNodes().get(destinationNodePosition).setHasSubnodes(true);
                mainViewModel.getNodes().remove(targetNodePosition);
                adapter.notifyItemChanged(destinationNodePosition);
                adapter.notifyItemRemoved(targetNodePosition);
            } else {
                if (mainViewModel.getNodes().size() <= 2) {
                    mainViewModel.setCurrentNode(reader.getSingleMenuItem(mainViewModel.getCurrentNode().getUniqueId()));
                    resetMenuToCurrentNode();
                } else {
                    int targetNodePosition = mainViewModel.getNodePositionInMenu(targetNodeUniqueID);
                    int destinationNodePosition = mainViewModel.getNodePositionInMenu(destinationNodeUniqueID);
                    if (destinationNodePosition != -1) {
                        mainViewModel.getNodes().get(destinationNodePosition).setHasSubnodes(true);
                        adapter.notifyItemChanged(destinationNodePosition);
                    }
                    mainViewModel.getNodes().remove(targetNodePosition);
                    adapter.notifyItemRemoved(targetNodePosition);
                }
            }
        }
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
            bookmarksButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_outline_bookmarks_off_24));
        } else {
            goBackButton.setVisibility(View.VISIBLE);
            goUpButton.setVisibility(View.GONE);
            createNode.setVisibility(View.VISIBLE);
            bookmarksButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_bookmarks_on_24));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        insetSetup(toolbar);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        AppContainer appContainer = ((ScApplication) getApplication()).appContainer;
        executor = appContainer.executor;
        handler = appContainer.handler;
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        SearchView searchView = findViewById(R.id.navigation_drawer_search);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            reader = DatabaseReaderFactory.getReader(this, handler, sharedPreferences, mainViewModel);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_error_failed_to_initiate_reader, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (reader == null) {
            Toast.makeText(this, R.string.toast_error_failed_to_initiate_reader, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true).setReorderingAllowed(true)
                    .add(R.id.main_view_fragment, NodeContentFragment.class, null, "main")
                    .commit();
            getSupportFragmentManager().executePendingTransactions();
            bookmarksToggle = false;
            filterNodeToggle = false;
            findInNodeToggle = false;
            mainViewModel.getMultiDatabaseSync().postValue(null);
            currentFindInNodeMarked = -1;
            if (sharedPreferences.getBoolean("restore_last_node", false)) {
                // Restores node on startup if user set this in settings
                ScNode scNode = reader.getSingleMenuItem(sharedPreferences.getString("last_node_unique_id", null));
                if (scNode != null) {
                    mainViewModel.setCurrentNode(scNode);
                    if (mainViewModel.getCurrentNode().hasSubnodes()) {
                        mainViewModel.setNodes(reader.getMenu(mainViewModel.getCurrentNode().getUniqueId()));
                    } else {
                        mainViewModel.setNodes(reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId()));
                    }
                    loadNodeContent();
                    setCurrentNodePosition();
                } else {
                    setMainMenuOnStart();
                }
            } else {
                setMainMenuOnStart();
            }
            if (reader instanceof MultiReader && sharedPreferences.getBoolean("preference_multifile_auto_sync", false)) {
                updateDrawerMenu();
            }
        } else {
            // Restoring some variable to make it possible restore content fragment after the screen rotation
            currentNodePosition = savedInstanceState.getInt("currentNodePosition");
            tempCurrentNodePosition = savedInstanceState.getInt("tempCurrentNodePosition");
            bookmarksToggle = savedInstanceState.getBoolean("bookmarksToggle");
            filterNodeToggle = savedInstanceState.getBoolean("filterNodeToggle");
            findInNodeToggle = savedInstanceState.getBoolean("findInNodeToggle");
        }
        if (reader instanceof MultiReader) {
            mainViewModel.getMultiDatabaseSync().observe(this, new Observer<ScheduledFuture<?>>() {
                // Observer has to be used instead of callback, because after screen orientation
                // current progress bar widget can't be accessed from it.
                @Override
                public void onChanged(ScheduledFuture<?> scheduledFuture) {
                    showHideProgressBar(scheduledFuture != null);
                }
            });
        }
        registerForOptionsMenuResult();
        initDrawerMenuNavigation(searchView);
        initDrawerMenuFilter(searchView);
        initFindInNode();

        RecyclerView rvMenu = findViewById(R.id.recyclerView);
        rvMenu.setAdapter(adapter);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        // pass the Open and Close toggle for the drawer layout listener
        // to toggle the button
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        // to make the Navigation drawer icon always appear on the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Listener for drawerMenu states
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                // If FindInNode view is on when user opens a drawer menu
                // Coses FindInNode view
                // Otherwise when user preses findInNodeNext/findInNodePrevious button in new node
                // content of the previous node will be loaded
                if (findInNodeToggle) {
                    closeFindInNode();
                }
            }

            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations() && mainViewModel.getMultiDatabaseSync().getValue() != null) {
            mainViewModel.getMultiDatabaseSync().getValue().cancel(true);
        }
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
                if (mainViewModel.getCurrentNode() != null) {
                    openNodeEditor();
                } else {
                    Toast.makeText(this, R.string.toast_message_please_open_a_node, Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemID == R.id.options_menu_export_to_pdf) {
                exportPdfSetup();
                return true;
            } else if (itemID == R.id.options_menu_export_database) {
                exportDatabaseSetup();
                return true;
            } else if (itemID == R.id.options_menu_find_in_node) {
                if (!findInNodeToggle) {
                    // Opens findInNode (sets the variables) only if it hasn't been opened yet
                    if (mainViewModel.getCurrentNode() != null) {
                        openFindInNode();
                    } else {
                        Toast.makeText(this, R.string.toast_error_please_select_node, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            } else if (itemID == R.id.options_menu_search) {
                if (findInNodeToggle) {
                    // Closes findInNode if it was opened when SearchFragment was selected to be opened
                    // Otherwise it won't let to display node content selected from search
                    closeFindInNode();
                }
                openSearch();
                return true;
            } else if (itemID == R.id.options_menu_settings) {
                Intent openSettingsActivity = new Intent(this, PreferencesActivity.class);
                startActivity(openSettingsActivity);
                return true;
            } else if (itemID == R.id.options_menu_about) {
                Intent openAboutActivity = new Intent(this, AboutActivity.class);
                startActivity(openAboutActivity);
                return true;
            } else if (itemID == R.id.options_menu_rescan_database) {
                updateDrawerMenu();
                return true;
            } else {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onResume() {
        // At this stage it is possible to load the content to the fragment after the screen rotation
        // at earlier point app will crash
        super.onResume();
        if (mainViewModel.getCurrentNode() != null) {
            setToolbarTitle(mainViewModel.getCurrentNode().getName());
            adapter.markItemSelected(currentNodePosition);
            adapter.notifyItemChanged(currentNodePosition);
            // Even if LiveData can restore node content after screen rotation it has old context
            // and any clicks (like opening images) will cause a crash
            loadNodeContent();
        }

        if (filterNodeToggle) {
            hideNavigation(true);
        }

        if (bookmarksToggle) {
            navigationNormalMode(false);
        }

        // Restoring FindInNode variables to original state
        if (findInNodeToggle) {
            closeFindInNode();
        }
    }

    @Override
    public void onSaveInstanceState(@Nullable Bundle outState) {
        // Saving some variables to make it possible to restore the content after screen rotation
        outState.putInt("currentNodePosition", currentNodePosition);
        outState.putInt("tempCurrentNodePosition", tempCurrentNodePosition);
        outState.putBoolean("bookmarksToggle", bookmarksToggle);
        outState.putBoolean("filterNodeToggle", filterNodeToggle);
        outState.putBoolean("findInNodeToggle", findInNodeToggle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        if (sharedPreferences.getBoolean("restore_last_node", false) && mainViewModel.getCurrentNode() != null) {
            // Saving current nodeUniqueID to be able to load it on next startup
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putString("last_node_unique_id", mainViewModel.getCurrentNode().getUniqueId());
            sharedPreferencesEditor.apply();
        }
        if (!isChangingConfigurations() && sharedPreferences.getBoolean("mirror_database_auto_export_switch", false)) {
            ContextCompat.startForegroundService(this, new Intent(this, DatabaseExportService.class));
        }
        super.onStop();
    }

    /**
     * Opens node that user selected by clicking anchor link
     * @param node array that holds data of one drawer menu / currentNode item
     */
    public void openAnchorLink(ScNode node) {
        if (node != null) {
            if (findInNodeToggle) {
                // Closes findInNode view to clear all variables
                // Otherwise loaded node in some cases might display previous node's content
                closeFindInNode();
            }
            mainViewModel.setCurrentNode(node);
            resetMenuToCurrentNode();
            loadNodeContent();
        } else {
            Toast.makeText(this, R.string.toast_error_node_does_not_exists, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Toggles between displaying and hiding of bookmarks
     * @param view view that was clicked by the user
     */
    public void openCloseBookmarks(View view) {
        if (bookmarksToggle) {
            // Showing normal menu
            closeBookmarks();
        } else {
            showBookmarks();
        }
    }

    /**
     * Opens attached/embedded with the app on the device
     * @param fileMimeType mime type of the attached/embedded for the device to show relevant app list to open the file with
     * @param nodeUniqueID unique ID of the node that has attached/embedded file
     * @param filename filename of the attached/embedded file
     * @param time timestamp that was saved to the database with the file
     * @param control control value of the file to get byte array of the right file. For XML/SQL readers it's offset and sha256sum sum of the file for Multifile database reader
     */
    private void openFile(String fileMimeType, String nodeUniqueID, String filename, String time, String control) {
        try {
            Uri fileToShare;
            if (reader.getDatabaseType() == DatabaseType.MULTI) {
                fileToShare = ((MultiDbFileShare) reader).getAttachedFileUri(nodeUniqueID, filename, control);
            } else {
                // If attached filename has more than one . (dot) in it temporary filename will not have full original filename in it
                // most important that it will have correct extension
                String prefix = Files.getFileName(filename);
                if (prefix.length() < 3) {
                    // Prefixes for temp files can't be shorter than 3 symbols
                    prefix = prefix + "123";
                }
                File tmpAttachedFile = File.createTempFile(prefix, "." + Files.getFileExtension(filename)); // Temporary file that will shared

                // Writes Base64 encoded string to the temporary file
                InputStream in = reader.getFileInputStream(nodeUniqueID, filename, time, control);
                FileOutputStream out = new FileOutputStream(tmpAttachedFile);
                byte[] buf = new byte[4 * 1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    out.write(buf, 0, length);
                }
                in.close();
                out.close();

                // Getting Uri to share
                fileToShare = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tmpAttachedFile);
            }
            // Intent to open file
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(fileToShare, fileMimeType);
            if (reader.getDatabaseType() == DatabaseType.MULTI && sharedPreferences.getBoolean("preference_multifile_use_embedded_file_name_on_disk", false)) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_error_failed_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets FindInNode UI
     */
    private void openFindInNode() {
        findInNodeToggle = true;
        mainViewModel.findInNodeStorageToggle(true); // Created an array to store nodeContent
        ProgressBar progressBar = findViewById(R.id.find_in_node_progress_bar);
        progressBar.setProgress(0);
        LinearLayout findInNodeLinearLayout = findViewById(R.id.main_view_find_in_node_linear_layout);
        EditText findInNodeEditText = findViewById(R.id.find_in_node_edit_text);

        findInNodeLinearLayout.setVisibility(View.VISIBLE); // Making findInView visible at the bottom if the window

        // Displaying / opening keyboard
        findInNodeEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Shows keyboard on API 30 (Android 11) reliably
            WindowCompat.getInsetsController(getWindow(), findInNodeEditText).show(WindowInsetsCompat.Type.ime());
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                // Delays to show soft keyboard by few milliseconds
                // Otherwise keyboard does not show up
                // It's a bit hacky (should be fixed)
                @Override
                public void run() {
                    imm.showSoftInput(findInNodeEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 50);
        }
    }

    /**
     * Function to launch fragment with enlarged image
     * @param nodeUniqueID unique ID of the node that image is embedded into
     * @param control control value of the file to get byte array of the right file. For XML/SQL readers it's offset and sha256sum sum of the file for Multifile database reader
     */
    public void openImageView(String nodeUniqueID, String control) {
        Bundle bundle = new Bundle();
        bundle.putString("type", "image");
        bundle.putString("nodeUniqueID", nodeUniqueID);
        bundle.putString("control", control);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, ImageViewFragment.class, bundle, "imageView")
                .addToBackStack("imageView")
                .commit();
        disableDrawerMenu();
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
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, ImageViewFragment.class, bundle, "imageView")
                .addToBackStack("imageView")
                .commit();
        disableDrawerMenu();
    }

    /**
     * Activates node's action icon/right click dialog fragment
     * @param node node which action menu should be shown
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void openMenuItemActionDialogFragment(ScNode node, int position) {
        DialogFragment menuItemActionDialogFragment = new MenuItemActionDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("node", node);
        bundle.putInt("position", position);
        bundle.putBoolean("bookmarked", reader.isNodeBookmarked(node.getUniqueId()));
        menuItemActionDialogFragment.setArguments(bundle);
        menuItemActionDialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialogFragment);
        menuItemActionDialogFragment.show(getSupportFragmentManager(), "menuItemActionDialogFragment");
    }

    /**
     * Opens node content editor in a different fragment
     * Disables drawer menu and changes hamburger menu icon to
     * home button
     */
    private void openNodeEditor() {
        Bundle bundle = new Bundle();
        ScrollView scrollView = findViewById(R.id.content_fragment_scrollview);
        bundle.putString("nodeUniqueID", mainViewModel.getCurrentNode().getUniqueId());
        bundle.putInt("scrollY", scrollView.getScrollY());
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, NodeContentEditorFragment.class, bundle, "editNode")
                .addToBackStack("editNode")
                .commit();
        disableDrawerMenu();
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
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, NodePropertiesFragment.class, bundle, "moveNode")
                .addToBackStack("nodeProperties")
                .commit();
        hideDrawerMenu();
    }

    /**
     * Opens search in a different fragment
     * Sets toolbar's title to "Search"
     */
    private void openSearch() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, SearchFragment.class, null, "search")
                .addToBackStack("search")
                .commit();
        setToolbarTitle(getString(R.string.options_menu_item_search));
        disableDrawerMenu();
    }

    /**
     * Opens node that was passed as an argument
     * Used to open search results
     * @param selectedNode Node that has to be opened
     */
    public void openSearchResult(ScNode selectedNode) {
        mainViewModel.setCurrentNode(selectedNode);
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        getOnBackPressedDispatcher().onBackPressed();
        resetMenuToCurrentNode();
        loadNodeContent();
    }

    /**
     * Clears existing menu and recreate with submenu of the currentNode
     */
    private void openSubmenu() {
        mainViewModel.setNodes(reader.getMenu(mainViewModel.getCurrentNode().getUniqueId()));
        currentNodePosition = 0;
        adapter.markItemSelected(currentNodePosition);
        adapter.notifyDataSetChanged();
    }

    /**
     * Checks if currently opened node is shown in drawer menu
     * @return position of the node in current drawer menu. -1 if node was not found
     */
    private int openedNodePositionInDrawerMenu() {
        int position = -1;
        if (mainViewModel.getCurrentNode() != null) {
            for (int i = 0; i < mainViewModel.getNodes().size(); i++) {
                if (mainViewModel.getCurrentNode().getUniqueId().equals(mainViewModel.getNodes().get(i).getUniqueId())) {
                    position = i;
                    break;
                }
            }
        }
        return position;
    }

    /**
     * Registered ActivityResultLauncher launches a file chooser. If user select location - opens
     * export dialog fragment for further configuration
     * @return ActivityResultLauncher to select the location for database export
     */
    private ActivityResultLauncher<String> registerExportDatabaseDialogFragment() {
        return registerForActivityResult(new ActivityResultContracts.CreateDocument("*/*"), result -> {
            if (result != null) {
                Bundle bundle = new Bundle();
                bundle.putString("exportFileUri", result.toString());
                ExportDatabaseDialogFragment exportDatabaseDialogFragment = new ExportDatabaseDialogFragment();
                exportDatabaseDialogFragment.setArguments(bundle);
                exportDatabaseDialogFragment.show(getSupportFragmentManager(), "exportDatabaseDialogFragment");
            }
        });
    }

    /**
     * Registered ActivityResultLauncher launches a file chooser to select a file to witch node
     * will be exported to.
     * @return ActivityResultLauncher to select the location for node PDF export
     */
    private ActivityResultLauncher<Intent> registerExportPdf() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                // If user actually chose a location to save a file
                try {
                    LinearLayout nodeContent = findViewById(CONTENT_FRAGMENT_LINEARLAYOUT);
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

                    StaticLayout title = StaticLayout.Builder.obtain(mainViewModel.getCurrentNode().getName(), 0, mainViewModel.getCurrentNode().getName().length(), paint, nodeContent.getWidth())
                            .setAlignment(Layout.Alignment.ALIGN_CENTER)
                            .build();
                    //*

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width + (padding * 2), nodeContent.getHeight() + (padding * 4) + title.getHeight(), 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);

                    Canvas canvas = page.getCanvas();

                    if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                        // Changing background color of the canvas if drawing views from night mode
                        // Otherwise text wont be visible
                        canvas.drawColor(getColor(R.color.window_background));
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
                    try (OutputStream outputStream = getContentResolver().openOutputStream(result.getData().getData(), "w")) { // Output file
                        document.writeTo(outputStream);
                    }
                    // Cleaning up
                    document.close();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.toast_error_failed_to_export_node_to_pdf, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Register with fragmentManager to get result from menuItemActionDialogFragment
     */
    private void registerForOptionsMenuResult() {
        getSupportFragmentManager().setFragmentResultListener("menuItemAction", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (mainViewModel.getMultiDatabaseSync().getValue() == null) {
                    MenuItemAction menuItemAction;
                    if (Build.VERSION.SDK_INT >= 33) {
                        menuItemAction = result.getSerializable("menuItemActionCode", MenuItemAction.class);
                    } else {
                        menuItemAction = (MenuItemAction) result.getSerializable("menuItemActionCode");
                    }
                    ScNode node = result.getParcelable("node");
                    switch (menuItemAction) {
                        case ADD_SIBLING_NODE:
                            launchCreateNewNodeFragment(node.getUniqueId(), 0);
                            break;
                        case ADD_SUBNODE:
                            launchCreateNewNodeFragment(node.getUniqueId(), 1);
                            break;
                        case ADD_TO_BOOKMARKS:
                            addNodeToBookmarks(node.getUniqueId());
                            break;
                        case REMOVE_FROM_BOOKMARKS:
                            removeNodeFromBookmarks(node.getUniqueId(), result.getInt("position"));
                            break;
                        case MOVE_NODE:
                            launchMoveNodeFragment(node);
                            break;
                        case DELETE_NODE:
                            deleteNode(node.getUniqueId());
                            break;
                        case PROPERTIES:
                            openNodeProperties(node.getMasterId() != null && !"0".equals(node.getMasterId()) ? node.getMasterId() : node.getUniqueId(), result.getInt("position"));
                            break;
                    }
                } else {
                    // Prompting user to cancel background Multifile database sync
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainView.this);
                    builder.setTitle(R.string.alert_dialog_sync_in_progress_title);
                    builder.setMessage(R.string.alert_dialog_sync_in_progress_message);
                    builder.setPositiveButton(R.string.button_wait, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mainViewModel.getMultiDatabaseSync().getValue().cancel(true);
                        }
                    });
                    builder.show();
                }
            }
        });
    }

    /**
     * Registered ActivityResultLauncher launches a file chooser that allows user to select where
     * attached file in the node will be saved
     * @return ActivityResultLauncher to a file to attach
     */
    private ActivityResultLauncher<String[]> registerSaveFile() {
        return registerForActivityResult(new ReturnSelectedFileUriForSaving(), result -> {
            if (result != null) {
                try (InputStream inputStream = reader.getFileInputStream(result.getExtras().getString("nodeUniqueID"), result.getExtras().getString("filename"), result.getExtras().getString("time"), result.getExtras().getString("offset"));
                     OutputStream outputStream = getContentResolver().openOutputStream(result.getData(), "w")) {
                    byte[] buf = new byte[4 * 1024];
                    int length;
                    while ((length = inputStream.read(buf)) != -1) {
                        outputStream.write(buf, 0, length);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, R.string.toast_error_failed_to_save_file, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Removes node from bookmark list
     * Updates drawer menu if bookmarks are being displayed
     * @param nodeUniqueID unique ID of the node which to remove from bookmarks
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void removeNodeFromBookmarks(String nodeUniqueID, int position) {
        reader.removeNodeFromBookmarks(nodeUniqueID);
        if (bookmarksToggle) {
            Iterator<ScNode> iterator = mainViewModel.getNodes().iterator();
            while(iterator.hasNext()) {
                ScNode node = iterator.next();
                if (node.getUniqueId().equals(nodeUniqueID)) {
                    iterator.remove();
                    adapter.notifyItemRemoved(position);
                    break;
                }
            }
        }
    }

    /**
     * Searches for node in tempNodes menu item list and removes it if found
     * @param nodeUniqueID unique ID of the node to search for
     */
    private void removeNodeFromTempNodes(String nodeUniqueID) {
        Iterator<ScNode> iterator = mainViewModel.getTempNodes().iterator();
        while (iterator.hasNext()) {
            ScNode currentNode = iterator.next();
            if (currentNode.getUniqueId().equals(nodeUniqueID)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Removes node from tempSearchNodes list
     * @param nodeUniqueID unique ID of the node that was deleted
     */
    private void removeNodeFromTempSearchNodes(String nodeUniqueID) {
        Iterator<ScNode> iterator = mainViewModel.getTempSearchNodes().iterator();
        while (iterator.hasNext()) {
            ScNode currentNode = iterator.next();
            if (currentNode.getUniqueId().equals(nodeUniqueID)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Restores drawer menu selected item to currently opened node
     */
    private void resetMenuToCurrentNode() {
        if (mainViewModel.getCurrentNode() != null) {
            if (mainViewModel.getCurrentNode().hasSubnodes()) {
                mainViewModel.setNodes(reader.getMenu(mainViewModel.getCurrentNode().getUniqueId()));
                currentNodePosition = 0;
                adapter.markItemSelected(currentNodePosition);
            } else {
                mainViewModel.setNodes(reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId()));
                for (int index = 0; index < mainViewModel.getNodes().size(); index++) {
                    if (mainViewModel.getNodes().get(index).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                        currentNodePosition = index;
                        adapter.markItemSelected(currentNodePosition);
                        break;
                    }
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Restores TextView to original state that was changed with highlightFindInNodeResult() function.
     * Resets all other variables and UI elements associated with FindInNode too.
     */
    private void restoreHighlightedView() {
        LinearLayout contentFragmentLinearLayout = findViewById(CONTENT_FRAGMENT_LINEARLAYOUT);
        if (currentFindInNodeMarked != -1 && contentFragmentLinearLayout != null && mainViewModel.getFindInNodeResultStorage().size() > 0) {
            int counter = 0;
            for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
                View view = contentFragmentLinearLayout.getChildAt(i);
                if (view instanceof TextView) {
                    SpannableStringBuilder originalText = new SpannableStringBuilder(mainViewModel.getTextViewContent(counter));
                    ((TextView) view).setText(originalText);
                    counter++;
                } else if (view instanceof HorizontalScrollView) {
                    TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                    for (int row = 0; row < tableLayout.getChildCount(); row++) {
                        TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                        for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                            TextView currentCell = (TextView) tableRow.getChildAt(cell);
                            SpannableStringBuilder originalText = new SpannableStringBuilder(mainViewModel.getTextViewContent(counter));
                            currentCell.setText(originalText);
                            counter++;
                        }
                    }
                }
            }
            currentFindInNodeMarked = -1;
            updateCounter(0);
            updateMarkedIndex();
            mainViewModel.resetFindInNodeResultStorage();
        }
    }

    /**
     * Function used when closing NodeEditorFragment depending on passed boolean variable displayed
     * node content will be reloaded or not. Node content is not read from database but read from
     * MainViewModel, because at every save it is stored there before saving it into database.
     * Changes home button to hamburger button in toolbar
     * @param reloadContent true - reload node content
     */
    public void returnFromFragmentWithHomeButton(boolean reloadContent) {
        if (reloadContent) {
            setToolbarTitle(mainViewModel.getCurrentNode().getName());
            loadNodeContent();
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        getOnBackPressedDispatcher().onBackPressed();
    }

    /**
     * Function to remove fragment from main view. Restore drawer indicator.
     */
    public void returnFromFragmentWithHomeButton() {
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        getOnBackPressedDispatcher().onBackPressed();
    }

    /**
     * Function used when closing Fragment
     * sets toolbar title to currently opened node name
     */
    public void returnFromFragmentWithHomeButtonAndRestoreTitle() {
        if (mainViewModel.getCurrentNode() != null) {
            setToolbarTitle(mainViewModel.getCurrentNode().getName());
        } else {
            setToolbarTitle(getString(R.string.app_name));
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        getOnBackPressedDispatcher().onBackPressed();
    }

    /**
     * Deals with attached/embedded files into database
     * @param nodeUniqueID unique ID of the node that has attached/embedded file
     * @param attachedFileFilename filename of the attached/embedded file
     * @param time timestamp that was saved to the database with the file
     * @param control control value of the file to get byte array of the right file. For XML/SQL readers it's offset and sha256sum sum of the file for Multifile database reader
     */
    public void saveOpenFile(String nodeUniqueID, String attachedFileFilename, String time, String control) {
        // Checks preferences if user choice default action for embedded files
        FileNameMap fileNameMap  = URLConnection.getFileNameMap();
        String fileMimeType = fileNameMap.getContentTypeFor(attachedFileFilename);
        if (fileMimeType == null) {
            // Custom file extensions (like CherryTree database extensions) are not recognized by Android
            // If mimeType for selected file can't be recognized. Catch all mimetype has to set
            // Otherwise app will crash while trying to save the file
            fileMimeType = "*/*";
        }
        String saveOpenFilePreference = sharedPreferences.getString("preferences_save_open_file", "Ask");
        if (saveOpenFilePreference.equals("Ask")) {
            // Setting up to send arguments to Dialog Fragment
            Bundle bundle = new Bundle();
            bundle.putString("nodeUniqueID", nodeUniqueID);
            bundle.putString("filename", attachedFileFilename);
            bundle.putString("time", time);
            bundle.putString("offset", control);
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
            openFile(fileMimeType, nodeUniqueID, attachedFileFilename, time, control);
        }
    }

    /**
     * This function gets the new drawer menu list
     * and marks currently opened node as such.
     */
    private void setClickedItemInSubmenu() {
        mainViewModel.setNodes(reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId()));
        for (int index = 0; index < mainViewModel.getNodes().size(); index++) {
            if (mainViewModel.getNodes().get(index).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                currentNodePosition = index;
                adapter.markItemSelected(currentNodePosition);
                adapter.notifyItemChanged(currentNodePosition);
                break;
            }
        }
    }

    /**
     * Sets current node as opened in drawer menu
     * by finding it's nodeUniqueID in drawer menu items
     * and setting it's index as currentNodePosition
     */
    private void setCurrentNodePosition() {
        for (int index = 0; index < mainViewModel.getNodes().size(); index++) {
            if (mainViewModel.getNodes().get(index).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                currentNodePosition = index;
            }
        }
    }

    /**
     * Start or stops findInView progress bar. Depending on the status value sets progress bar as
     * indeterminate or not and set upper range of the progress bar to FindInNode result count. When
     * indeterminate mode is on - bar will be used to show progress of the change of the results.
     * @param status true - start progress bar, false - stop progress bar
     */
    private void setFindInNodeProgressBar(Boolean status) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = findViewById(R.id.find_in_node_progress_bar);
                if (status) {
                    progressBar.setIndeterminate(true);
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(mainViewModel.getFindInNodeResultCount());
                }
                progressBar.setIndeterminate(status);
            }
        });
    }

    /**
     * Sets main menu items to drawerMenu on start
     */
    private void setMainMenuOnStart() {
        currentNodePosition = -1;
        mainViewModel.setCurrentNode(null); // This needs to be placed before restoring the instance if there was one
        mainViewModel.setNodes(reader.getMainNodes());
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
     * Displays bookmarks instead of normal navigation menu in navigation drawer
     */
    private void showBookmarks() {
        ArrayList<ScNode> bookmarkedNodes = reader.getBookmarkedNodes();
        // Check if there are any bookmarks
        // If no bookmarks were found a message is displayed
        // No other action is taken
        if (bookmarkedNodes == null) {
            Toast.makeText(this, R.string.toast_no_bookmarks_message, Toast.LENGTH_SHORT).show();
        } else {
            // Displaying bookmarks
            navigationNormalMode(false);
            // Saving current state of the menu
            mainViewModel.saveCurrentNodes();
            tempCurrentNodePosition = currentNodePosition;

            // Displaying bookmarks
            mainViewModel.setNodes(bookmarkedNodes);
            currentNodePosition = openedNodePositionInDrawerMenu();
            adapter.markItemSelected(currentNodePosition);
            adapter.notifyDataSetChanged();
            bookmarksToggle = true;
        }
    }

    /**
     * Makes progress bar at the top of the content view visible. It should be shown to indicate
     * that Multifile database scan is in progress.
     * @param show true - show progress bar, false - hide progress bar
     */
    private void showHideProgressBar(boolean show) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    findViewById(R.id.database_sync_progress_bar).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.database_sync_progress_bar).setVisibility(View.GONE);
                }
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
     * Initiates background the Multifile database folder scan and recreates new drawer_menu.xml.
     * Loads it to the MultiReader.
     */
    private void updateDrawerMenu() {
        if (mainViewModel.getMultiDatabaseSync().getValue() != null) {
            Toast.makeText(this, R.string.toast_error_scan_already_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mainViewModel.getMultiDatabaseSync().postValue(executor.schedule(new CollectNodesBackgroundRunnable(
                    Uri.parse(sharedPreferences.getString("databaseUri", null)),
                    MainView.this,
                    new NodesCollectedCallback() {
                        @Override
                        public void onNodesCollected(int result) {
                            if (result == 0) {
                                displayToastOnMainThread(getString(R.string.toast_message_updated_drawer_menu));
                                try {
                                    ((MultiReader) reader).setDrawerMenu();
                                    if (filterNodeToggle) {
                                        // If user was filtering nodes when new drawer menu items were collected
                                        CheckBox checkBoxExcludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);
                                        mainViewModel.setNodes(reader.getAllNodes(checkBoxExcludeFromSearch.isChecked()));
                                        mainViewModel.tempSearchNodesToggle(true);
                                        if (mainViewModel.getCurrentNode() != null) {
                                            // Just in case something changed in currently opened node's menu and user will quit filter without selecting a node to open
                                            mainViewModel.updateSavedCurrentNodes(reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId()));
                                            for (int index = 0; index < mainViewModel.getNodes().size(); index++) {
                                                if (mainViewModel.getTempNodes().get(index).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
                                                    tempCurrentNodePosition = index;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (mainViewModel.getCurrentNode() != null) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                resetMenuToCurrentNode();
                                            }
                                        });
                                    } else {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mainViewModel.setNodes(reader.getMainNodes());
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                } catch (IOException | SAXException e) {
                                    displayToastOnMainThread(getString(R.string.toast_error_failed_update_drawer_menu));
                                    mainViewModel.getMultiDatabaseSync().postValue(null);
                                }
                            } else if (result == 1) {
                                displayToastOnMainThread(getString(R.string.toast_error_failed_update_drawer_menu));
                            }
                            mainViewModel.getMultiDatabaseSync().postValue(null);
                        }
                    }), 0, TimeUnit.SECONDS));
        } catch (ParserConfigurationException e) {
            Toast.makeText(MainView.this, R.string.toast_error_failed_update_drawer_menu, Toast.LENGTH_SHORT).show();
            showHideProgressBar(false);
            mainViewModel.getMultiDatabaseSync().postValue(null);
        }
    }

    /**
     * Sets/updates index of currently marked result in the counter and progress bar
     */
    private void updateMarkedIndex() {
        TextView findInNodeEditTextMarkedIndex = findViewById(R.id.find_in_node_edit_text_marked_index);
        ProgressBar progressBar = findViewById(R.id.find_in_node_progress_bar);
        handler.post(new Runnable() {
            @Override
            public void run() {
                findInNodeEditTextMarkedIndex.setText(String.valueOf(currentFindInNodeMarked + 1));
                progressBar.setProgress(currentFindInNodeMarked + 1);
            }
        });
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
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
        DatabaseReaderFactory.getReader().updateNodeProperties(nodeUniqueID, name, progLang, noSearchMe, noSearchCh);
        // updates drawerMenu items properties for shared nodes
        for (int i = 0; i < mainViewModel.getNodes().size(); i++) {
            ScNode scNode = mainViewModel.getNodes().get(i);
            if (scNode.getMasterId().equals(nodeUniqueID) || scNode.getUniqueId().equals(nodeUniqueID)) {
                scNode.setRichText(progLang.equals("custom-colors"));
                scNode.setName(name);
                adapter.notifyItemChanged(i);
            }
        }
        if (mainViewModel.getCurrentNode() != null && mainViewModel.getNodes().get(position).getUniqueId().equals(mainViewModel.getCurrentNode().getUniqueId())) {
            // If opened node was changed - reloads node name in toolbar
            // and reloads node content if reloadNodeContent is true
            mainViewModel.getCurrentNode().setName(name);
            setToolbarTitle(name);
            if (reloadNodeContent) {
                loadNodeContent();
            }
        }
    }
}