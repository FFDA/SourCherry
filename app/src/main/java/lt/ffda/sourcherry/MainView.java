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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import lt.ffda.sourcherry.database.DatabaseReader;
import lt.ffda.sourcherry.database.DatabaseReaderFactory;
import lt.ffda.sourcherry.database.MultiReader;
import lt.ffda.sourcherry.dialogs.ExportDatabaseDialogFragment;
import lt.ffda.sourcherry.dialogs.MenuItemActionDialogFragment;
import lt.ffda.sourcherry.dialogs.SaveOpenDialogFragment;
import lt.ffda.sourcherry.fragments.CreateNodeFragment;
import lt.ffda.sourcherry.fragments.ImageViewFragment;
import lt.ffda.sourcherry.fragments.NodeContentFragment;
import lt.ffda.sourcherry.fragments.NodeContentEditorFragment;
import lt.ffda.sourcherry.fragments.MoveNodeFragment;
import lt.ffda.sourcherry.fragments.NodePropertiesFragment;
import lt.ffda.sourcherry.fragments.SearchFragment;
import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.preferences.PreferencesActivity;
import lt.ffda.sourcherry.runnables.CollectNodesBackgroundRunnable;
import lt.ffda.sourcherry.runnables.FindInNodeRunnable;
import lt.ffda.sourcherry.runnables.FindInNodeRunnableCallback;
import lt.ffda.sourcherry.runnables.NodesCollectedCallback;
import lt.ffda.sourcherry.utils.Filenames;
import lt.ffda.sourcherry.utils.MenuItemAction;
import lt.ffda.sourcherry.utils.ReturnSelectedFileUriForSaving;

public class MainView extends AppCompatActivity {

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

                StaticLayout title = StaticLayout.Builder.obtain(this.mainViewModel.getCurrentNode().getName(), 0, this.mainViewModel.getCurrentNode().getName().length(), paint, nodeContent.getWidth())
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
    private DatabaseReader reader;
    /**
     * Launches activity to save the attached file to the device
     */
    ActivityResultLauncher<String[]> saveFile = registerForActivityResult(new ReturnSelectedFileUriForSaving(), result -> {
        if (result != null) {
            try (InputStream inputStream = this.reader.getFileInputStream(result.getExtras().getString("nodeUniqueID"), result.getExtras().getString("filename"), result.getExtras().getString("time"), result.getExtras().getString("offset"));
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
    private SharedPreferences sharedPreferences;
    private int tempCurrentNodePosition; // Needed to save selected node position when user opens bookmarks;

    /**
     * Adds node to bookmark list
     * @param nodeUniqueID unique ID of the node which to add to bookmarks
     */
    private void addNodeToBookmarks(String nodeUniqueID) {
        this.reader.addNodeToBookmarks(nodeUniqueID);
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
            this.handler.postDelayed(new Runnable() {
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
            setClickedItemInSubmenu();
        }
    }

    /**
     * Launches CreateNewNode to create node in currently opened menu
     * Node will be appended to the end of it
     * @param view view that was clicked by the user
     */
    public void createNode(View view) {
        if (this.mainViewModel.getNodes().size() == 0 || !this.mainViewModel.getNodes().get(0).isParent()) {
            this.launchCreateNewNodeFragment("0", 1);
        } else {
            if (this.mainViewModel.getNodes().get(0).isParent()) {
                this.launchCreateNewNodeFragment(this.mainViewModel.getNodes().get(0).getUniqueId(), 1);
            } else {
                this.launchCreateNewNodeFragment(this.mainViewModel.getNodes().get(0).getUniqueId(), 1);
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
            setToolbarTitle("SourCherry");
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
        this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
        this.actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    /**
     * Displays toast message on the main thread
     * @param message message to show
     */
    private void displayToastOnMainThread(String message) {
        this.handler.post(new Runnable() {
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
        MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        getSupportActionBar().show();
    }

    /**
     * Exists MainView activity with Toast message
     * telling user that error occurred while reading the database
     */
    public void exitWithError() {
        Toast.makeText(this, R.string.toast_error_cant_read_database, Toast.LENGTH_SHORT).show();
        this.finish();
    }

    /**
     * Sets up database for export
     * User can be prompted to choose file location
     * Or confirm to overwrite the newer
     * Mirror database file
     */
    public void exportDatabaseSetup() {
        // XML without password already saved in external file
        if (this.sharedPreferences.getString("databaseFileExtension", null).equals("ctd") && this.sharedPreferences.getString("databaseStorageType", null).equals("shared")) {
            Toast.makeText(this, R.string.toast_message_not_password_protected_xml_saves_changes_externally, Toast.LENGTH_SHORT).show();
            return;
        }
        if (this.sharedPreferences.getBoolean("mirror_database_switch", false)) {
            // If user uses MirrorDatabase
            // Variables that will be put into bundle for MirrorDatabaseProgressDialogFragment
            Uri mirrorDatabaseFileUri = null; // Uri to the Mirror Database File inside Mirror Database Folder
            long mirrorDatabaseDocumentFileLastModified = 0;

            // Reading through files inside Mirror Database Folder
            Uri mirrorDatabaseFolderUri = Uri.parse(this.sharedPreferences.getString("mirrorDatabaseFolderUri", null));
            Uri mirrorDatabaseFolderChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mirrorDatabaseFolderUri, DocumentsContract.getTreeDocumentId(mirrorDatabaseFolderUri));

            Cursor cursor = this.getContentResolver().query(mirrorDatabaseFolderChildrenUri, new String[]{"document_id", "_display_name", "last_modified"}, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                if (cursor.getString(1).equals(this.sharedPreferences.getString("mirrorDatabaseFilename", null))) {
                    // if file with the Mirror Database File filename was wound inside Mirror Database Folder
                    mirrorDatabaseFileUri = DocumentsContract.buildDocumentUriUsingTree(mirrorDatabaseFolderUri, cursor.getString(0));
                    mirrorDatabaseDocumentFileLastModified = cursor.getLong(2);
                    break;
                }
            }
            if (cursor != null) {
                cursor.close();
            }

            if (mirrorDatabaseDocumentFileLastModified == 0) {
                Toast.makeText(this, R.string.toast_error_failed_to_find_mirror_database, Toast.LENGTH_SHORT).show();
                return;
            }

            // If found Mirror Database File is older or the same as the last time it was synchronized
            // copying is done immediately
            if (mirrorDatabaseDocumentFileLastModified <= this.sharedPreferences.getLong("mirrorDatabaseLastModified", 0)) {
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
                long finalMirrorDatabaseDocumentFileLastModified = mirrorDatabaseDocumentFileLastModified;
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

    private void exportPdfSetup() {
        // Sets the intent for asking user to choose a location where to save a file
        if (this.mainViewModel.getCurrentNode() != null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_TITLE, this.mainViewModel.getCurrentNode().getName());
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
        this.mainViewModel.setNodes(this.mainViewModel.getTempSearchNodes());
        ArrayList<ScNode> filteredNodes = this.mainViewModel.getNodes().stream()
                .filter(node -> node.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toCollection(ArrayList::new));
        this.mainViewModel.setNodes(filteredNodes);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * Searches for query in nodeContent
     * @param query search query
     */
    private void findInNode(String query) {
        if (query.length() > 0) {
            // If new query is longer when one character
            this.restoreHighlightedView();
            this.executor.submit(new FindInNodeRunnable(this.mainViewModel, query, new FindInNodeRunnableCallback() {
                @Override
                public void searchFinished() {
                    MainView.this.setFindInNodeProgressBar(false);
                    MainView.this.updateCounter(MainView.this.mainViewModel.getFindInNodeResultCount());
                    if (MainView.this.mainViewModel.getFindInNodeResultCount() == 0) {
                        // If user types until there are no matches left
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainView.this.restoreHighlightedView();
                            }
                        });
                    } else {
                        // If there are matches for user query
                        // First result has to be highlighter and scrolled too
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                MainView.this.currentFindInNodeMarked = 0;
                                MainView.this.highlightFindInNodeResult();
                            }
                        });
                    }
                }

                @Override
                public void searchStarted() {
                    MainView.this.setFindInNodeProgressBar(true);
                }
            }));
        } else {
            // If new query is 0 characters long, that means that user deleted everything and view should be reset to original
            MainView.this.restoreHighlightedView();
        }
    }

    /**
     * Calculates next result that has to be highlighted and initiates switchFindInNodeHighlight
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
     * Returns MainViewModel used to store all information of the app (DrawerMenu nodes, NodeContent)
     * @return MainViewModel
     */
    public MainViewModel getMainViewModel() {
        return this.mainViewModel;
    }

    /**
     * Closes bookmarks in drawer menu
     * @param view view needed to associated button with action
     */
    public void goBack(View view) {
        this.closeBookmarks();
    }

    /**
     * Reloads drawer menu to show main menu
     * if it is not displayed
     * otherwise shows a message to the user
     * that the top of the database tree was already reached
     * @param view view that was clicked
     */
    public void goHome(View view) {
        ArrayList<ScNode> tempMainNodes = this.reader.getMainNodes();
        // Compares node sizes, first and last node's uniqueIDs in both arrays
        if (tempMainNodes.size() == this.mainViewModel.getNodes().size() && tempMainNodes.get(0).getUniqueId().equals(this.mainViewModel.getNodes().get(0).getUniqueId()) && tempMainNodes.get(this.mainViewModel.getNodes().size() -1 ).getUniqueId().equals(this.mainViewModel.getNodes().get(this.mainViewModel.getNodes().size() -1 ).getUniqueId())) {
            Toast.makeText(this, "Your are at the top", Toast.LENGTH_SHORT).show();
        } else {
            this.mainViewModel.setNodes(tempMainNodes);
            this.currentNodePosition = -1;
            if (bookmarksToggle && this.mainViewModel.getCurrentNode() != null) {
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

    /**
     * Moves navigation menu one node up
     * If menu is already at the top it shows a message to the user
     * @param view view that was clicked
     */
    public void goNodeUp(View view) {
        ArrayList<ScNode> nodes = this.reader.getParentWithSubnodes(this.mainViewModel.getNodes().get(0).getUniqueId());
        if (nodes != null && nodes.size() != this.mainViewModel.getNodes().size()) {
            // If retrieved nodes are not null and array size do not match the one displayed
            // it is definitely not the same node so it can go up
            this.mainViewModel.setNodes(nodes);
            this.currentNodePosition = -1;
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyDataSetChanged();
        } else {
            // If both nodes arrays matches in size it might be the same node (especially main/top)
            // This part checks if first and last nodes in arrays matches by comparing nodeUniqueID of both
            if (nodes != null && nodes.get(0).getUniqueId().equals(this.mainViewModel.getNodes().get(0).getUniqueId()) && nodes.get(nodes.size() -1 ).getUniqueId().equals(this.mainViewModel.getNodes().get(this.mainViewModel.getNodes().size() -1 ).getUniqueId())) {
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
     * Prepares DrawerLayout for fragments that should not allow user to open the drawer
     * Hide DrawerLayout completely
     */
    private void hideDrawerMenu() {
        this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Locks drawer menu
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
        this.mainViewModel.findInNodeStorageReset();
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        int counter = 0; // Iterator of the all the saved views from node content
        int resultCounter = 0;
        int[] currentResult;
        for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
            View view = contentFragmentLinearLayout.getChildAt(i);
            if (view instanceof TextView) {
                TextView currentTextView = (TextView) view;
                SpannableStringBuilder currentTextViewContent = MainView.this.mainViewModel.getfindInNodeStorageContent(counter);
                while (resultCounter < this.mainViewModel.getFindInNodeResultCount() && (currentResult = this.mainViewModel.getFindInNodeResult(resultCounter))[0] == counter) {
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
                        SpannableStringBuilder currentTextViewContent = MainView.this.mainViewModel.getfindInNodeStorageContent(counter);
                        while (resultCounter < this.mainViewModel.getFindInNodeResultCount() && (currentResult = this.mainViewModel.getFindInNodeResult(resultCounter))[0] == counter) {
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
        nodeContentFragment.switchFindInNodeHighlight(-1, this.currentFindInNodeMarked);
        this.updateMarkedIndex();
    }

    /**
     * Initiates all the listeners for DrawerMenu node filter function
     * @param searchView DrawerMenu's search view
     */
    private void initDrawerMenuFilter(SearchView searchView) {
        CheckBox checkBoxExcludeFromSearch = findViewById(R.id.navigation_drawer_omit_marked_to_exclude);
        checkBoxExcludeFromSearch.setChecked(this.sharedPreferences.getBoolean("exclude_from_search", false));
        checkBoxExcludeFromSearch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (MainView.this.filterNodeToggle) {
                    // Gets new menu list only if filter mode is activated
                    MainView.this.mainViewModel.setTempSearchNodes(MainView.this.reader.getAllNodes(isChecked));
                    MainView.this.adapter.notifyDataSetChanged();
                    searchView.requestFocus();
                    MainView.this.filterNodes(searchView.getQuery().toString());
                    SharedPreferences.Editor editor = MainView.this.sharedPreferences.edit();
                    editor.putBoolean("exclude_from_search", isChecked);
                    editor.commit();
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (MainView.this.filterNodeToggle) { // This check fixes bug where all database's nodes were displayed after screen rotation
                    MainView.this.filterNodes(newText);
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
                if (MainView.this.mainViewModel.getCurrentNode() != null) {
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
    }

    /**
     * Initiates listeners for clicks on DrawerMenu items and logic
     * @param searchView DrawerMenu's search view
     */
    private void initDrawerMenuNavigation(SearchView searchView) {
        this.adapter = new MenuItemAdapter(this.mainViewModel.getNodes(), this);
        this.adapter.setOnItemClickListener(new MenuItemAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {
                if (MainView.this.mainViewModel.getCurrentNode() == null || !MainView.this.mainViewModel.getNodes().get(position).getUniqueId().equals(MainView.this.mainViewModel.getCurrentNode().getUniqueId())) {
                    // If current node is null (empty/nothing opened yet) or selected nodeUniqueID is not the same as currently opened node
                    MainView.this.mainViewModel.setCurrentNode(MainView.this.mainViewModel.getNodes().get(position));
                    MainView.this.loadNodeContent();
                    if (MainView.this.mainViewModel.getNodes().get(position).hasSubnodes()) { // Checks if node is marked to have subnodes
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
                        } else if (MainView.this.filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.filterNodeToggle = false;
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
                    if (MainView.this.mainViewModel.getNodes().get(position).hasSubnodes()) { // Checks if node is marked as having subnodes
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
                        } else if (MainView.this.filterNodeToggle) {
                            // Node selected from the search
                            searchView.onActionViewCollapsed();
                            MainView.this.hideNavigation(false);
                            MainView.this.setClickedItemInSubmenu();
                            MainView.this.filterNodeToggle = false;
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
                if (actionId == EditorInfo.IME_ACTION_NEXT && MainView.this.mainViewModel.getFindInNodeResultCount() > 1) {
                    MainView.this.findInNodeNext();
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
        this.setToolbarTitle(this.mainViewModel.getCurrentNode().getName());
        this.executor.execute(new Runnable() {
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
        MainView.this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        MainView.this.drawerLayout.open();
        getSupportActionBar().show();
        if (this.reader.moveNode(targetNodeUniqueID, destinationNodeUniqueID)) {
            if (this.mainViewModel.getCurrentNode() == null) {
                int targetNodePosition = this.mainViewModel.getNodePositionInMenu(targetNodeUniqueID);
                int destinationNodePosition = this.mainViewModel.getNodePositionInMenu(destinationNodeUniqueID);
                this.mainViewModel.getNodes().get(destinationNodePosition).setHasSubnodes(true);
                this.mainViewModel.getNodes().remove(targetNodePosition);
                this.adapter.notifyItemChanged(destinationNodePosition);
                this.adapter.notifyItemRemoved(targetNodePosition);
            } else {
                if (this.mainViewModel.getNodes().size() <= 2) {
                    this.mainViewModel.setCurrentNode(this.reader.getSingleMenuItem(this.mainViewModel.getCurrentNode().getUniqueId()));
                    this.resetMenuToCurrentNode();
                } else {
                    int targetNodePosition = this.mainViewModel.getNodePositionInMenu(targetNodeUniqueID);
                    int destinationNodePosition = this.mainViewModel.getNodePositionInMenu(destinationNodeUniqueID);
                    if (destinationNodePosition != -1) {
                        this.mainViewModel.getNodes().get(destinationNodePosition).setHasSubnodes(true);
                        this.adapter.notifyItemChanged(destinationNodePosition);
                    }
                    this.mainViewModel.getNodes().remove(targetNodePosition);
                    this.adapter.notifyItemRemoved(targetNodePosition);
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        AppContainer appContainer = ((ScApplication) getApplication()).appContainer;
        executor = appContainer.executor;
        handler = appContainer.handler;
        // drawer layout instance to toggle the menu icon to open
        // drawer and back button to close drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);
        SearchView searchView = findViewById(R.id.navigation_drawer_search);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            this.reader = DatabaseReaderFactory.getReader(this, handler, sharedPreferences, mainViewModel);
        } catch (IOException | ParserConfigurationException | TransformerConfigurationException |
                 InterruptedException | SAXException | ExecutionException e) {
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
            if (sharedPreferences.getBoolean("restore_last_node", false) && reader.doesNodeExist(sharedPreferences.getString("last_node_unique_id", null))) {
                // Restores node on startup if user set this in settings
                mainViewModel.setCurrentNode(reader.getSingleMenuItem(sharedPreferences.getString("last_node_unique_id", null)));
                if (mainViewModel.getCurrentNode().hasSubnodes()) { // Checks if menu has subnodes and creates appropriate menu
                    mainViewModel.setNodes(reader.getMenu(mainViewModel.getCurrentNode().getUniqueId()));
                } else {
                    mainViewModel.setNodes(reader.getParentWithSubnodes(mainViewModel.getCurrentNode().getUniqueId()));
                }
                loadNodeContent();
                setCurrentNodePosition();
            } else {
                currentNodePosition = -1;
                mainViewModel.setCurrentNode(null); // This needs to be placed before restoring the instance if there was one
                mainViewModel.setNodes(reader.getMainNodes());
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
        if (!isChangingConfigurations() && this.mainViewModel.getMultiDatabaseSync().getValue() != null) {
            this.mainViewModel.getMultiDatabaseSync().getValue().cancel(true);
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
                    this.openNodeEditor();
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
        if (this.mainViewModel.getCurrentNode() != null) {
            this.setToolbarTitle(this.mainViewModel.getCurrentNode().getName());
            this.adapter.markItemSelected(this.currentNodePosition);
            this.adapter.notifyItemChanged(this.currentNodePosition);
            // Even if LiveData can restore node content after screen rotation it has old context
            // and any clicks (like opening images) will cause a crash
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
    public void onSaveInstanceState(@Nullable Bundle outState) {
        // Saving some variables to make it possible to restore the content after screen rotation
        outState.putInt("currentNodePosition", this.currentNodePosition);
        outState.putInt("tempCurrentNodePosition", this.tempCurrentNodePosition);
        outState.putBoolean("bookmarksToggle", this.bookmarksToggle);
        outState.putBoolean("filterNodeToggle", this.filterNodeToggle);
        outState.putBoolean("findInNodeToggle", this.findInNodeToggle);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        if (this.sharedPreferences.getBoolean("restore_last_node", false) && this.mainViewModel.getCurrentNode() != null) {
            // Saving current nodeUniqueID to be able to load it on next startup
            SharedPreferences.Editor sharedPreferencesEditor = this.sharedPreferences.edit();
            sharedPreferencesEditor.putString("last_node_unique_id", this.mainViewModel.getCurrentNode().getUniqueId());
            sharedPreferencesEditor.apply();
        }
        super.onStop();
    }

    /**
     * Opens node that user selected by clicking anchor link
     * @param node array that holds data of one drawer menu / currentNode item
     */
    public void openAnchorLink(ScNode node) {
        if (node != null) {
            if (this.findInNodeToggle) {
                // Closes findInNode view to clear all variables
                // Otherwise loaded node in some cases might display previous node's content
                this.closeFindInNode();
            }
            this.mainViewModel.setCurrentNode(node);
            this.resetMenuToCurrentNode();
            this.loadNodeContent();
        } else {
            Toast.makeText(this, R.string.toast_error_node_does_not_exists, Toast.LENGTH_SHORT).show();
        }
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
            this.showBookmarks();
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
            // If attached filename has more than one . (dot) in it temporary filename will not have full original filename in it
            // most important that it will have correct extension
            File tmpAttachedFile = File.createTempFile(Filenames.getFileName(filename), "." + Filenames.getFileExtension(filename)); // Temporary file that will shared

            // Writes Base64 encoded string to the temporary file
            InputStream in = this.reader.getFileInputStream(nodeUniqueID, filename, time, control);
            FileOutputStream out = new FileOutputStream(tmpAttachedFile);
            byte[] buf = new byte[4 * 1024];
            int length;
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
            in.close();
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
     * Sets FindInNode UI
     */
    private void openFindInNode() {
        this.findInNodeToggle = true;
        this.mainViewModel.findInNodeStorageToggle(true); // Created an array to store nodeContent
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
        this.disableDrawerMenu();
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
        this.disableDrawerMenu();
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
        bundle.putString("nodeUniqueID", this.mainViewModel.getCurrentNode().getUniqueId());
        bundle.putInt("scrollY", scrollView.getScrollY());
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.main_view_fragment, NodeContentEditorFragment.class, bundle, "editNode")
                .addToBackStack("editNode")
                .commit();
        this.disableDrawerMenu();
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
        this.hideDrawerMenu();
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
        this.setToolbarTitle("Search");
        this.disableDrawerMenu();
    }

    /**
     * Opens node that was passed as an argument
     * Used to open search results
     * @param selectedNode Node that has to be opened
     */
    public void openSearchResult(ScNode selectedNode) {
        this.mainViewModel.setCurrentNode(selectedNode);
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
        this.resetMenuToCurrentNode();
        this.loadNodeContent();
    }

    /**
     * Clears existing menu and recreate with submenu of the currentNode
     */
    private void openSubmenu() {
        this.mainViewModel.setNodes(this.reader.getMenu(this.mainViewModel.getCurrentNode().getUniqueId()));
        this.currentNodePosition = 0;
        this.adapter.markItemSelected(this.currentNodePosition);
        this.adapter.notifyDataSetChanged();
    }

    /**
     * Checks if currently opened node is shown in drawer menu
     * @return position of the node in current drawer menu. -1 if node was not found
     */
    private int openedNodePositionInDrawerMenu() {
        int position = -1;
        if (this.mainViewModel.getCurrentNode() != null) {
            for (int i = 0; i < this.mainViewModel.getNodes().size(); i++) {
                if (this.mainViewModel.getCurrentNode().getUniqueId().equals(this.mainViewModel.getNodes().get(i).getUniqueId())) {
                    position = i;
                    break;
                }
            }
        }
        return position;
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
     * Removes node from bookmark list
     * Updates drawer menu if bookmarks are being displayed
     * @param nodeUniqueID unique ID of the node which to remove from bookmarks
     * @param position position of the node in drawer menu as reported by MenuItemAdapter
     */
    private void removeNodeFromBookmarks(String nodeUniqueID, int position) {
        this.reader.removeNodeFromBookmarks(nodeUniqueID);
        if (this.bookmarksToggle) {
            Iterator<ScNode> iterator = this.mainViewModel.getNodes().iterator();
            while(iterator.hasNext()) {
                ScNode node = iterator.next();
                if (node.getUniqueId().equals(nodeUniqueID)) {
                    iterator.remove();
                    this.adapter.notifyItemRemoved(position);
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
        Iterator<ScNode> iterator = this.mainViewModel.getTempNodes().iterator();
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
        Iterator<ScNode> iterator = this.mainViewModel.getTempSearchNodes().iterator();
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
        if (this.mainViewModel.getCurrentNode() != null) {
            if (MainView.this.mainViewModel.getCurrentNode().hasSubnodes()) {
                this.mainViewModel.setNodes(this.reader.getMenu(this.mainViewModel.getCurrentNode().getUniqueId()));
                this.currentNodePosition = 0;
                this.adapter.markItemSelected(this.currentNodePosition);
            } else {
                this.mainViewModel.setNodes(this.reader.getParentWithSubnodes(this.mainViewModel.getCurrentNode().getUniqueId()));
                for (int index = 0; index < this.mainViewModel.getNodes().size(); index++) {
                    if (this.mainViewModel.getNodes().get(index).getUniqueId().equals(this.mainViewModel.getCurrentNode().getUniqueId())) {
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
     * Restores TextView to original state that was changed with highlightFindInNodeResult() function.
     * Resets all other variables and UI elements associated with FindInNode too.
     */
    private void restoreHighlightedView() {
        LinearLayout contentFragmentLinearLayout = findViewById(R.id.content_fragment_linearlayout);
        if (this.currentFindInNodeMarked != -1 && contentFragmentLinearLayout != null && this.mainViewModel.getFindInNodeResultStorage().size() > 0) {
            int viewIndex = this.mainViewModel.getFindInNodeResult(this.currentFindInNodeMarked)[0];
            int counter = 0;
            for (int i = 0; i < contentFragmentLinearLayout.getChildCount(); i++) {
                View view = contentFragmentLinearLayout.getChildAt(i);
                if (view instanceof TextView) {
                    SpannableStringBuilder originalText = new SpannableStringBuilder(this.mainViewModel.getTextViewContent(counter));
                    ((TextView) view).setText(originalText);
                    counter++;
                } else if (view instanceof HorizontalScrollView) {
                    TableLayout tableLayout = (TableLayout) ((HorizontalScrollView) view).getChildAt(0);
                    for (int row = 0; row < tableLayout.getChildCount(); row++) {
                        TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
                        for (int cell = 0; cell < tableRow.getChildCount(); cell++) {
                            TextView currentCell = (TextView) tableRow.getChildAt(cell);
                            SpannableStringBuilder originalText = new SpannableStringBuilder(this.mainViewModel.getTextViewContent(counter));
                            currentCell.setText(originalText);
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
     * Function used when closing NodeEditorFragment depending on passed boolean variable displayed
     * node content will be reloaded or not. Node content is not read from database but read from
     * MainViewModel, because at avery save it is stored there before saving it into database.
     * Changes home button to hamburger button in toolbar
     * @param reloadContent true - reload node content
     */
    public void returnFromFragmentWithHomeButton(boolean reloadContent) {
        if (reloadContent) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            NodeContentFragment nodeContentFragment = (NodeContentFragment) fragmentManager.findFragmentByTag("main");
            // Sends ArrayList to fragment to be added added to view
            setToolbarTitle(mainViewModel.getCurrentNode().getName());
            loadNodeContent();
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
    }

    /**
     * Function to remove fragment from main view. Restore drawer indicator.
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
        if (this.mainViewModel.getCurrentNode() != null) {
            this.setToolbarTitle(this.mainViewModel.getCurrentNode().getName());
        }
        actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        onBackPressed();
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
        String saveOpenFilePreference = this.sharedPreferences.getString("preferences_save_open_file", "Ask");
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
            this.openFile(fileMimeType, nodeUniqueID, attachedFileFilename, time, control);
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
                int previousNodePosition = currentNodePosition;
                currentNodePosition = index;
                adapter.markItemSelected(currentNodePosition);
                adapter.notifyItemChanged(previousNodePosition);
                adapter.notifyItemChanged(currentNodePosition);
                break;
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
            if (this.mainViewModel.getNodes().get(index).getUniqueId().equals(this.mainViewModel.getCurrentNode().getUniqueId())) {
                this.currentNodePosition = index;
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
        ArrayList<ScNode> bookmarkedNodes = this.reader.getBookmarkedNodes();
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
     * Makes progress bar at the top of the content view visible. It should be shown to indicate
     * that Multifile database scan is in progress.
     * @param show true - show progress bar, false - hide progress bar
     */
    private void showHideProgressBar(boolean show) {
        this.handler.post(new Runnable() {
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
        if (this.mainViewModel.getMultiDatabaseSync().getValue() != null) {
            Toast.makeText(this, R.string.toast_error_scan_already_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            this.mainViewModel.getMultiDatabaseSync().postValue(this.executor.schedule(new CollectNodesBackgroundRunnable(
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
            this.mainViewModel.getMultiDatabaseSync().postValue(null);
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
                findInNodeEditTextMarkedIndex.setText(String.valueOf(MainView.this.currentFindInNodeMarked + 1));
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
        this.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
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
        if (this.mainViewModel.getCurrentNode() != null && this.mainViewModel.getNodes().get(position).getUniqueId().equals(this.mainViewModel.getCurrentNode().getUniqueId())) {
            // If opened node was changed - reloads node name in toolbar
            // and reloads node content if reloadNodeContent is true
            this.mainViewModel.getCurrentNode().setName(name);
            this.setToolbarTitle(name);
            if (reloadNodeContent) {
                this.loadNodeContent();
            }
        }
    }
}