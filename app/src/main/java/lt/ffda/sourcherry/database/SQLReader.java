/*
 * This file is part of SourCherry.
 *
 * SourCherry is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SourCherry is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SourCherry. If not, see <https://www.gnu.org/licenses/>.
 */

package lt.ffda.sourcherry.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lt.ffda.sourcherry.MainView;
import lt.ffda.sourcherry.MainViewModel;
import lt.ffda.sourcherry.R;
import lt.ffda.sourcherry.model.ScNode;
import lt.ffda.sourcherry.model.ScNodeContent;
import lt.ffda.sourcherry.model.ScNodeContentTable;
import lt.ffda.sourcherry.model.ScNodeContentText;
import lt.ffda.sourcherry.model.ScNodeProperties;
import lt.ffda.sourcherry.model.ScSearchNode;
import lt.ffda.sourcherry.spans.BackgroundColorSpanCustom;
import lt.ffda.sourcherry.spans.ClickableSpanFile;
import lt.ffda.sourcherry.spans.ClickableSpanLink;
import lt.ffda.sourcherry.spans.ClickableSpanNode;
import lt.ffda.sourcherry.spans.ImageSpanAnchor;
import lt.ffda.sourcherry.spans.ImageSpanFile;
import lt.ffda.sourcherry.spans.ImageSpanImage;
import lt.ffda.sourcherry.spans.ImageSpanLatex;
import lt.ffda.sourcherry.spans.StyleSpanBold;
import lt.ffda.sourcherry.spans.StyleSpanItalic;
import lt.ffda.sourcherry.spans.TypefaceSpanCodebox;
import lt.ffda.sourcherry.spans.TypefaceSpanFamily;
import lt.ffda.sourcherry.spans.URLSpanWebs;
import ru.noties.jlatexmath.JLatexMathDrawable;

public class SQLReader extends DatabaseReader implements DatabaseVacuum {
    private final SQLiteDatabase sqlite;
    private final Context context;
    private final Handler handler;
    private final MainViewModel mainViewModel;
    private final DocumentBuilder documentBuilder;
    private final Transformer transformer;

    /**
     * Class that opens databases based on SQL file. Provides all functions necessary to read and edit
     * the data in the database.
     * @param sqlite SQLiteDatabase object with opened database
     * @param context application context to display toast messages, get resources, handle clicks
     * @param handler to run methods on main thread
     * @param mainViewModel ViewModel of MainView activity to store data
     * @throws ParserConfigurationException Indicates a serious configuration error.
     * @throws TransformerConfigurationException Indicates a serious configuration error.
     */
    public SQLReader(SQLiteDatabase sqlite, Context context, Handler handler, MainViewModel mainViewModel) throws ParserConfigurationException, TransformerConfigurationException {
        this.sqlite = sqlite;
        this.context = context;
        this.handler = handler;
        this.mainViewModel = mainViewModel;
        this.documentBuilder = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder();
        this.transformer = TransformerFactory
                .newInstance()
                .newTransformer();
    }

    @Override
    public void addNodeToBookmarks(String nodeUniqueID) {
        // Adding bookmarks
        ContentValues contentValues = new ContentValues();
        contentValues.put("node_id", nodeUniqueID);
        contentValues.put("sequence", 1);
        this.sqlite.insert("bookmark", null, contentValues);
        this.fixBookmarkNodeSequence();
    }

    @Override
    public ScNode createNewNode(String nodeUniqueID, int relation, String name, String progLang, String noSearchMe, String noSearchCh){
        // Updating node table
        int newNodeUniqueID = this.getNodeMaxID() + 1;
        byte level = convertNoSearchToLevel(noSearchMe, noSearchCh);
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        ContentValues contentValues = new ContentValues();
        contentValues.put("node_id", newNodeUniqueID);
        contentValues.put("name", name);
        if (progLang.equals("custom-colors")) {
            contentValues.put("txt", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><node/>");
        } else {
            contentValues.put("txt", "");
        }
        contentValues.put("syntax", progLang);
        contentValues.put("tags", "");
        contentValues.put("is_ro", 0);
        contentValues.put("is_richtxt", progLang.equals("custom-colors") ? 1 : 0);
        contentValues.put("has_codebox", 0);
        contentValues.put("has_table", 0);
        contentValues.put("has_image", 0);
        contentValues.put("level", level);
        contentValues.put("ts_creation", timeStamp);
        contentValues.put("ts_lastsave", timeStamp);

        long createNewNodeResult = this.sqlite.insert("node", null, contentValues);
        if (createNewNodeResult == -1) {
            displayToast(this.context.getString(R.string.toast_error_failed_to_create_entry_into_node_table));
            return null;
        }

        // Updating children table
        contentValues.clear();
        boolean isSubnode = true;
        contentValues.put("node_id", newNodeUniqueID);
        if (relation == 0) {
            int parentNodeUniqueID = this.getParentNodeUniqueID(nodeUniqueID);
            contentValues.put("father_id", parentNodeUniqueID);
            // Searching for position for new node in parent node children sequence
            int newNodeSequenceNumber = -1;
            Cursor parentNodeChildrenSequenceCursor = this.sqlite.query("children", new String[]{"node_id", "sequence"}, "father_id=?", new String[]{String.valueOf(parentNodeUniqueID)}, null, null, "sequence ASC", null);
            while (parentNodeChildrenSequenceCursor.moveToNext()) {
                if (nodeUniqueID.equals(parentNodeChildrenSequenceCursor.getString(0))) {
                    // Found the node that was selected to create sibling node
                    newNodeSequenceNumber = parentNodeChildrenSequenceCursor.getInt(1) + 1;
                    break;
                }
            }
            parentNodeChildrenSequenceCursor.close();
            // Updating sequence position (+1) for all nodes
            // that follows new node in sequence
            Cursor updateChildrenTableCursor = sqlite.rawQuery("UPDATE children SET sequence = sequence + 1 WHERE father_id=? and sequence >= ?;", new String[]{String.valueOf(parentNodeUniqueID), String.valueOf(newNodeSequenceNumber)});
            updateChildrenTableCursor.moveToFirst();
            updateChildrenTableCursor.close();
            contentValues.put("sequence", newNodeSequenceNumber);
            if (parentNodeUniqueID == 0) {
                isSubnode = false;
            }
        } else {
            contentValues.put("father_id", Integer.parseInt(nodeUniqueID));
            contentValues.put("sequence", this.getNewNodeSequenceNumber(nodeUniqueID));
        }

        long childrenUpdateResult = this.sqlite.insert("children", null, contentValues);
        if (childrenUpdateResult == -1) {
            displayToast(this.context.getString(R.string.toast_error_failed_to_create_entry_into_children_table));
            return null;
        }
        return new ScNode(String.valueOf(newNodeUniqueID), name, false, false, isSubnode, progLang.equals("custom-colors"), false, "", 0, false);
    }

    @Override
    public void deleteNode(String nodeUniqueID) {
        String parentNodeUniqueID;
        this.sqlite.beginTransaction();
        try {
            Cursor parentNodeUniqueIDCursor = this.sqlite.query("children", new String[]{"father_id"}, "node_id=?", new String[]{nodeUniqueID}, null, null, null, null);
            parentNodeUniqueIDCursor.moveToFirst();
            parentNodeUniqueID = parentNodeUniqueIDCursor.getString(0);
            parentNodeUniqueIDCursor.close();
            this.sqlite.delete("bookmark", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("children", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("codebox", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("grid", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("image", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("node", "node_id = ?", new String[]{nodeUniqueID});
            Cursor childrenNodeUniqueID = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{nodeUniqueID}, null, null, null, null);
            while (childrenNodeUniqueID.moveToNext()) {
                this.deleteNodeChildren(childrenNodeUniqueID.getString(0));
            }
            childrenNodeUniqueID.close();
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
        this.fixChildrenNodeSequence(parentNodeUniqueID);
        this.fixBookmarkNodeSequence();
    }

    @Override
    public void displayToast(String message) {
        // Displays a toast on main thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SQLReader.this.context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean doesNodeExist(String nodeUniqueID) {
        if (nodeUniqueID == null) {
            return false;
        }
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name FROM node WHERE node.node_id=?", new String[]{nodeUniqueID});
        if (cursor.getCount() == 1) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    @Override
    public ArrayList<ScNode> getAllNodes(boolean noSearch) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.level, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);
            ArrayList<ScNode> nodes = this.returnSubnodeSearchArrayList(cursor);
            cursor.close();
            return nodes;
        } else {
            Cursor cursor = this.sqlite.query("node", new String[]{"name", "node_id", "is_richtxt", "syntax", "is_ro"}, null, null, null, null, null);
            ArrayList<ScNode> nodes = returnSubnodeArrayList(cursor, false);
            cursor.close();
            return nodes;
        }
    }

    @Override
    public ArrayList<ScNode> getBookmarkedNodes() {
        // Returns bookmarked nodes from the document
        // Returns null if there aren't any
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.syntax, node.is_ro FROM node INNER JOIN bookmark ON node.node_id=bookmark.node_id ORDER BY bookmark.sequence ASC", null);
        if(cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        ArrayList<ScNode> nodes = returnSubnodeArrayList(cursor, false);
        cursor.close();
        return nodes;
    }

    @Override
    public ImageSpan makeBrokenImageSpan(int type) {
        // Returns an image span that is used to display as placeholder image
        // used when cursor window is to small to get an image blob
        // pass 0 to get broken image span, pass 1 to get broken latex span
        SpannableStringBuilder brokenSpan = new SpannableStringBuilder();
        brokenSpan.append(" ");
        Drawable drawableBrokenImage;
        ImageSpan brokenImage;
        if (type == 0) {
            drawableBrokenImage = AppCompatResources.getDrawable(this.context, R.drawable.ic_outline_broken_image_48);
            brokenImage = new ImageSpanImage(drawableBrokenImage);
        } else {
            drawableBrokenImage =  AppCompatResources.getDrawable(this.context, R.drawable.ic_outline_broken_latex_48);
            brokenImage = new ImageSpanLatex(drawableBrokenImage);
        }
        //// Inserting image
        drawableBrokenImage.setBounds(0,0, drawableBrokenImage.getIntrinsicWidth(), drawableBrokenImage.getIntrinsicHeight());
        return brokenImage;
    }

    @Override
    public InputStream getFileInputStream(String nodeUniqueID, String filename, String time, String control) {
        // Returns byte array (stream) to be written to file or opened

        Cursor cursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND filename=? AND time=? AND offset=?", new String[]{nodeUniqueID, filename, time, control}, null, null, null);
        // Getting user choice how big the cursor window should be
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
        try {
            // Try needed to close the cursor. Otherwise ofter return statement it won't be closed;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Expands cursor window for API 28 (Android 9) and greater
                // This allows to save/open bigger files
                ((SQLiteCursor) cursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
            } else {
                // Setting cursorWindow as to 2 (default android value)
                // Android 8 and lower versions do not have this function
                // It's only that error toast would show a correct size
                cursorWindow = 2;
            }
            cursor.move(1);
            return new ByteArrayInputStream(cursor.getBlob(0));
        } catch (Exception SQLiteBlobTooBigException) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_open_file_large, cursorWindow));
            return null;
        } finally {
            cursor.close();
        }
    }

    @Override
    public InputStream getImageInputStream(String nodeUniqueID, String control) {
        // Returns image byte array to be displayed in ImageViewFragment because some of the images are too big to pass in a bundle
        Cursor cursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, control}, null, null, null);
        // Getting user choice how big the cursor window should be
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
        try {
            // Try is needed to close the cursor. Otherwise after return statement it won't be closed;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Expands cursor window for API 28 (Android 9) and greater
                // This allows to display bigger images
                ((SQLiteCursor) cursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
            } else {
                // Setting cursorWindow as to 2 (default android value)
                // Android 8 and lower versions do not have this function
                // It's only that error toast would show a correct size
                cursorWindow = 2;
            }
            cursor.move(1);
            return new ByteArrayInputStream(cursor.getBlob(0));
        } catch (Exception SQLiteBlobTooBigException) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_load_image_large, cursorWindow));
            return null;
        } finally {
            cursor.close();
        }
    }

    @Override
    public ArrayList<ScNode> getMainNodes() {
        // Returns main nodes from the database
        // Used to display menu when app starts
        ArrayList<ScNode> nodes = null;
        try (Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null)) {
            nodes = returnSubnodeArrayList(cursor, false);
        } catch (Exception SQLiteException) {
            ((MainView) context).exitWithError();
        }
        return nodes;
    }

    @Override
    public ArrayList<ScNode> getMenu(String nodeUniqueID) {
        // Returns Subnodes of the node which nodeUniqueID is provided
        Cursor cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{nodeUniqueID});
        ArrayList<ScNode> nodes = returnSubnodeArrayList(cursor, true);
        nodes.add(0, createParentNode(String.valueOf(nodeUniqueID)));
        cursor.close();
        return nodes;
    }

    @Override
    public void loadNodeContent(String nodeUniqueID) {
        // Original XML document has newline characters marked (hopefully it's the same with SQL database)
        // Returns ArrayList of objects implementing ScNodeContent interface
        ArrayList<ScNodeContent> nodeContent = new ArrayList<>();
        SpannableStringBuilder nodeContentStringBuilder = new SpannableStringBuilder(); // Temporary for text, codebox, image formatting
        ArrayList<ScNodeContentTable> nodeTables = new ArrayList<>(); // Temporary for table storage
        ArrayList<Integer> nodeTableCharOffsets = new ArrayList<>();
        //// This needed to calculate where to place span in to builder
        // Because after every insertion in the middle it displaces the next insertion
        // by the length of the inserted span.
        // During the loop lengths of the string elements (not images or tables) are added to this
        int totalCharOffset = 0;
        ////

        Cursor cursor = this.sqlite.query("node", new String[]{"txt", "syntax", "has_codebox", "has_table", "has_image"}, "node_id=?", new String[]{nodeUniqueID}, null, null, null); // Get node table entry with nodeUniqueID
        if (cursor.move(1)) { // Cursor items starts at 1 not 0!!!
            // syntax is the same as prog_lang attribute in XML database
            // It is used to set formatting for the node and separate between node types (Code Node)
            // The same attribute is used for codeboxes
            String nodeSyntax = cursor.getString(1);

            if (nodeSyntax.equals("custom-colors")) {
                // This is formatting for Rich Text and Plain Text nodes
                NodeList nodeContentNodeList = getDocumentFromString(cursor.getString(0)).getElementsByTagName("node").item(0).getChildNodes(); // Gets all the subnodes/childnodes of selected node
                for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                    // Loops through nodes of selected node
                    Node currentNode = nodeContentNodeList.item(x);
                    if (currentNode.hasAttributes()) {
                        nodeContentStringBuilder.append(makeFormattedRichText(currentNode));
                    } else {
                        nodeContentStringBuilder.append(currentNode.getTextContent());
                    }
                }

                int hasCodebox = cursor.getInt(2);
                int hasTable = cursor.getInt(3);
                int hasImage = cursor.getInt(4);

                // If it is marked that node has codebox, table or image
                if (hasCodebox == 1 || hasTable == 1 || hasImage == 1) {
                    //// Building string for SQLQuery
                    // Because every element is in it own table
                    // Only the tables that are marked that node have assets will be search
                    // Only offsets will be selected and second column will be created
                    // That will have 7 (codebox), 8 (table) or 9 (image) written in it
                    StringBuilder codeboxTableImageQueryString = new StringBuilder();

                    // Depending on how many tables will be searched
                    // instances of how many times nodeUniqueID will have to be inserted will differ
                    int queryCounter = 0; // This is the counter for that
                    if (hasCodebox == 1) {
                        // Means that node has has codeboxes in it
                        codeboxTableImageQueryString.append("SELECT offset, 7 FROM codebox WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasTable == 1) {
                        // Means that node has tables in it
                        if (hasCodebox == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT offset, 8 FROM grid WHERE node_id=? ");
                        queryCounter++;
                    }
                    if (hasImage == 1) {
                        // Means that node has has images (images, anchors or files) in it
                        if (hasCodebox == 1 || hasTable == 1) {
                            codeboxTableImageQueryString.append("UNION ");
                        }
                        codeboxTableImageQueryString.append("SELECT offset, 9 FROM image WHERE node_id=? ");
                        queryCounter++;
                    }
                    codeboxTableImageQueryString.append("ORDER BY offset ASC");

                    /// Creating the array that will be used to insert nodeUniqueIDs
                    String[] queryArguments = new String[queryCounter];
                    Arrays.fill(queryArguments, nodeUniqueID);
                    ///
                    ////
                    // Getting user choice how big the cursor window should be
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    long cursorWindow = sharedPreferences.getInt("preferences_cursor_window_size", 15);
                    Cursor codeboxTableImageCursor = this.sqlite.rawQuery(codeboxTableImageQueryString.toString(), queryArguments);

                    while (codeboxTableImageCursor.moveToNext()) {
                        int charOffset = codeboxTableImageCursor.getInt(0);
                        if (codeboxTableImageCursor.getInt(1) == 9) {
                            // Get image entry for current node_id and charOffset
                            Cursor imageCursor = this.sqlite.query("image", new String[]{"anchor", "filename", "time", "justification"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, String.valueOf(charOffset)}, null, null, null);
                            if (imageCursor.moveToFirst()) {
                                if (!imageCursor.getString(0).isEmpty()) {
                                    // Text in column "anchor" (0) means that this line is for anchor
                                    SpannableStringBuilder anchorImageSpan = makeAnchorImageSpan(imageCursor.getString(0));
                                    imageCursor.close();
                                    nodeContentStringBuilder.insert(charOffset + totalCharOffset, anchorImageSpan);
                                    continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                }
                                if (!imageCursor.getString(1).isEmpty()) {
                                    // Text in column "filename" (1) means that this line is for file OR LaTeX formula box
                                    if (imageCursor.getString(1).equals("__ct_special.tex")) {
                                        // For latex boxes
                                        Cursor latexBlobCursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, String.valueOf(charOffset)}, null, null, null);
                                        latexBlobCursor.moveToFirst();
                                        SpannableStringBuilder latexImageSpan = makeLatexImageSpan(latexBlobCursor.getBlob(0), imageCursor.getString(3));
                                        imageCursor.close();
                                        latexBlobCursor.close();
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, latexImageSpan);
                                        continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                    } else {
                                        // If it is not LaTex file (normal file)
                                        SpannableStringBuilder attachedFileSpan = makeAttachedFileSpan(nodeUniqueID, imageCursor.getString(1), String.valueOf(imageCursor.getDouble(2)), String.valueOf(charOffset), imageCursor.getString(3));
                                        imageCursor.close();
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, attachedFileSpan);
                                        totalCharOffset += attachedFileSpan.length() - 1;
                                        continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                    }
                                }
                                else {
                                    // Any other line should be an image
                                    imageCursor.close();
                                    Cursor imageBlobCursor = this.sqlite.query("image", new String[]{"png"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, String.valueOf(charOffset)}, null, null, null);
                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        // Expands cursor window for API 28 (Android 9) and greater
                                        // This allows to display bigger images and open/save bigger files
                                        // Right now limit is 15mb
                                        ((SQLiteCursor) imageBlobCursor).setWindow(new CursorWindow(null, 1024 * 1024 * cursorWindow));
                                    }  else {
                                        // Setting cursorWindow as to 2 (default android value)
                                        // Android 8 and lower versions do not have this function
                                        // It's only that error toast would show a correct size
                                        cursorWindow = 2;
                                    }
                                    try {
                                        // Tries to move to get image blob from DB. Might me too big.
                                        imageBlobCursor.moveToFirst();
                                        SpannableStringBuilder imageSpan = makeImageSpan(imageBlobCursor.getBlob(0), nodeUniqueID, String.valueOf(charOffset), cursor.getString(3)); // Blob is the image in byte[] form
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, imageSpan);
                                    } catch (Exception SQLiteBlobTooBigException) {
                                        // If image blob was to big for SQL Toast error message will be displayed
                                        // And placeholder image is placed
                                        SpannableStringBuilder brokenImageSpan = new SpannableStringBuilder();
                                        brokenImageSpan.append(" ");
                                        brokenImageSpan.setSpan(this.makeBrokenImageSpan(0), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        nodeContentStringBuilder.insert(charOffset + totalCharOffset, brokenImageSpan);
                                        this.displayToast(context.getString(R.string.toast_error_failed_to_load_image_large, cursorWindow));
                                    }
                                    imageBlobCursor.close();
                                    continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                                }
                            }
                        } else if (codeboxTableImageCursor.getInt(1) == 7) {
                            // codebox row
                            // Get codebox entry for current node_id and charOffset
                            Cursor codeboxCursor = this.sqlite.rawQuery(new String("SELECT * FROM codebox WHERE node_id = ? AND offset = ?"), new String[]{nodeUniqueID, String.valueOf(charOffset)});
                            if (codeboxCursor.moveToFirst()) {
                                SpannableStringBuilder codeboxText = makeFormattedCodeboxSpan(codeboxCursor.getString(2), codeboxCursor.getString(3), codeboxCursor.getString(4), codeboxCursor.getInt(5), codeboxCursor.getInt(6), codeboxCursor.getInt(7) == 1, codeboxCursor.getInt(8) == 1, codeboxCursor.getInt(9) == 1);
                                nodeContentStringBuilder.insert(charOffset + totalCharOffset, codeboxText);
                                codeboxCursor.close();
                                totalCharOffset += codeboxText.length() - 1;
                            }
                        } else if (codeboxTableImageCursor.getInt(1) == 8) {
                            // table row
                            // Get table row entry for current node_id and charOffset
                            Cursor tableCursor = this.sqlite.query("grid", new String[]{"txt", "col_min", "col_max", "justification"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, String.valueOf(charOffset)}, null, null, null);
                            if (tableCursor.moveToFirst()) {
                                int tableCharOffset = charOffset + totalCharOffset; // Place where SpannableStringBuilder will be split
                                nodeTableCharOffsets.add(tableCharOffset);
                                int cellMin = tableCursor.getInt(1);
                                int cellMax = tableCursor.getInt(2);
                                ArrayList<CharSequence[]> currentTableContent = new ArrayList<>();
                                Document document = getDocumentFromString(tableCursor.getString(0));
                                // All the rows of the table. Not like in XML database, there aren't any empty text nodes to be filtered out
                                NodeList tableRowsNodes = document.getElementsByTagName("table").item(0).getChildNodes();
                                byte lightInterface = 0;
                                if (!((Element) document.getElementsByTagName("table").item(0)).getAttribute("is_light").equals("")) {
                                    lightInterface = Byte.parseByte(((Element) document.getElementsByTagName("table").item(0)).getAttribute("is_light"));
                                }
                                // Tables in database are saved content first and the last row is the header of the table
                                currentTableContent.add(this.getTableRow(tableRowsNodes.item(tableRowsNodes.getLength() - 1)));
                                for (int row = 0; row < tableRowsNodes.getLength() - 1; row++) {
                                    currentTableContent.add(this.getTableRow(tableRowsNodes.item(row)));
                                }
                                ScNodeContentTable scNodeContentTable = new ScNodeContentTable((byte) 1, currentTableContent, cellMin, cellMax, lightInterface, tableCursor.getString(3), ((Element) document.getElementsByTagName("table").item(0)).getAttribute("col_widths"));
                                tableCursor.close();
                                nodeTables.add(scNodeContentTable);
                                // Instead of adding space for formatting reason
                                // it might be better to take one of totalCharOffset
                                totalCharOffset -= 1;
                            }
                        }
                    }
                    codeboxTableImageCursor.close();
                }
            } else if (nodeSyntax.equals("plain-text")) {
                // Plain text node does not have any formatting and has not node embedded in to it
                nodeContentStringBuilder.append(cursor.getString(0));
            } else {
                // Node is Code Node. It's just a big CodeBox with no dimensions
                nodeContentStringBuilder.append(makeFormattedCodeNodeSpan(cursor.getString(0)));
            }
        }
        cursor.close();

        int subStringStart = 0; // Holds start from where SpannableStringBuilder has to be split from
        if (nodeTables.size() > 0) {
            // If there are at least one table in the node
            // SpannableStringBuilder that holds are split in to parts
            for (int i = 0; i < nodeTables.size(); i++) {
                // Creating text part of this iteration
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, nodeTableCharOffsets.get(i));
                subStringStart = nodeTableCharOffsets.get(i); // Next string will be cut starting from this offset (previous end)
                ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, textPart);
                nodeContent.add(nodeContentText);
                // Creating table part of this iteration
                nodeContent.add(nodeTables.get(i));
            }
            // Last part of the SpannableStringBuilder (if there is one)
            if (subStringStart < nodeContentStringBuilder.length()) {
                SpannableStringBuilder textPart = (SpannableStringBuilder) nodeContentStringBuilder.subSequence(subStringStart, nodeContentStringBuilder.length());
                ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, textPart);
                nodeContent.add(nodeContentText);
            }
        } else {
            // If there are no tables in the Node content
            ScNodeContentText nodeContentText = new ScNodeContentText((byte) 0, nodeContentStringBuilder);
            nodeContent.add(nodeContentText);
        }
        this.mainViewModel.getNodeContent().postValue(nodeContent);
    }

    @Override
    public int getNodeMaxID() {
        Cursor cursor = this.sqlite.rawQuery("SELECT MAX(node_id) FROM node", null);
        cursor.moveToFirst();
        int nodeUniqueID = cursor.getInt(0);
        cursor.close();
        return nodeUniqueID;
    }

    @Override
    public ScNodeProperties getNodeProperties(String nodeUniqueID) {
        Cursor cursor = this.sqlite.query("node", new String[]{"name", "syntax", "level"}, "node_id=?", new String[]{nodeUniqueID}, null, null, null, null);
        cursor.moveToFirst();
        byte[] noSearch = this.convertLevelToNoSearch(cursor.getInt(2));
        ScNodeProperties nodeProperties = new ScNodeProperties(nodeUniqueID, cursor.getString(0), cursor.getString(1), noSearch[0], noSearch[1]);
        cursor.close();
        return nodeProperties;
    }

    @Override
    public ArrayList<ScNode> getParentWithSubnodes(String nodeUniqueID) {
        // Checks if it is possible to go up in document's node tree from given node's uniqueID
        // Returns array with appropriate nodes
        ArrayList<ScNode> nodes = null;
        String nodeParentID;
        Cursor cursor = this.sqlite.query("children", new String[]{"father_id"}, "node_id=?", new String[]{nodeUniqueID}, null, null, null);
        if (cursor.move(1)) { // Cursor items start at 1 not 0!!!
            nodeParentID = cursor.getString(0);
            cursor.close();
            if (nodeParentID.equals("0")) {
                nodes = getMainNodes();
            } else {
                cursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{nodeParentID});
                nodes = returnSubnodeArrayList(cursor, true);
                nodes.add(0, createParentNode(nodeParentID));
            }
        }
        cursor.close();
        return nodes;
    }

    @Override
    public ScNode getSingleMenuItem(String nodeUniqueID) {
        // Returns single menu item to be used when opening anchor links
        ScNode currentScNode = null;
        Cursor cursor = this.sqlite.query("node", new String[]{"name", "is_richtxt", "syntax", "is_ro"}, "node_id=?", new String[]{nodeUniqueID}, null, null,null);
        if (cursor.move(1)) { // Cursor items starts at 1 not 0!!!
            // Node name and unique_id always the same for the node
            String nameValue = cursor.getString(0);
            boolean isRichText = cursor.getString(2).equals("custom-colors");
            boolean isBold = ((cursor.getInt(1) >> 1) & 0x01) == 1;
            String foregroundColor = "";
            if (((cursor.getInt(1) >> 2) & 0x01) == 1) {
                foregroundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
            }
            int iconId = cursor.getInt(3) >> 1;
            boolean isReadOnly = (cursor.getInt(3) & 0x01) == 1;
            if (hasSubnodes(nodeUniqueID)) {
                // if node has subnodes, then it has to be opened as a parent node and displayed as such
                currentScNode = new ScNode(nodeUniqueID, nameValue, true, true, false, isRichText, isBold, foregroundColor, iconId, isReadOnly);
            } else {
                // If node doesn't have subnodes, then it has to be opened as subnode of some other node
                currentScNode = new ScNode(nodeUniqueID, nameValue, false, false, true, isRichText, isBold, foregroundColor, iconId, isReadOnly);
            }
        }
        cursor.close();
        return currentScNode;
    }

    @Override
    public boolean isNodeBookmarked(String nodeUniqueID) {
        Cursor cursor = this.sqlite.query("bookmark", new String[]{"node_id"}, "node_id = ?", new String[]{nodeUniqueID}, null, null, null, null);
        boolean isNodeBookmarked = cursor.getCount() > 0;
        cursor.close();
        return isNodeBookmarked;
    }

    @Override
    public SpannableStringBuilder makeAnchorImageSpan(String anchorValue) {
        // Makes an image span that displays an anchor to mark position of it.
        // It does not respond to touches in any way
        SpannableStringBuilder anchorImageSpan = new SpannableStringBuilder();
        anchorImageSpan.append(" ");
        // Inserting image
        Drawable drawableAttachedFileIcon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_anchor_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpanAnchor attachedFileIcon = new ImageSpanAnchor(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, anchorValue);
        anchorImageSpan.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return anchorImageSpan;
    }

    @Override
    public ClickableSpanNode makeAnchorLinkSpan(String nodeUniqueID, String linkAnchorName) {
        // Creates and returns clickable span that when touched loads another node which nodeUniqueID was passed as an argument
        // As in CherryTree it's foreground color #07841B
        ClickableSpanNode clickableSpanNode = new ClickableSpanNode() {
            @Override
            public void onClick(@NonNull View widget) {
                ((MainView) SQLReader.this.context).openAnchorLink(getSingleMenuItem(nodeUniqueID));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                // Formatting of span text
                ds.setColor(context.getColor(R.color.link_anchor));
                ds.setUnderlineText(true);
            }
        };
        clickableSpanNode.setNodeUniqueID(nodeUniqueID);
        if (linkAnchorName.length() > 0) {
            clickableSpanNode.setLinkAnchorName(linkAnchorName);
        }
        return clickableSpanNode;
    }

    @Override
    public ClickableSpanLink makeFileFolderLinkSpan(String type, String base64Filename) {
        // Creates and returns a span for a link to external file or folder
        // When user clicks on the link snackbar displays a path to the file that was saved in the original system
        ClickableSpanLink clickableSpanLink = new ClickableSpanLink() {
            @Override
            public void onClick(@NonNull View widget) {
                // Decoding of Base64 is done here
                ((MainView) SQLReader.this.context).fileFolderLinkFilepath(new String(Base64.decode(base64Filename, Base64.DEFAULT)));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                // Formatting of span text
                if (type.equals("file")) {
                    ds.setColor(context.getColor(R.color.link_file));
                } else {
                    ds.setColor(context.getColor(R.color.link_folder));
                }
                ds.setUnderlineText(true);
            }
        };
        clickableSpanLink.setLinkType(type);
        clickableSpanLink.setBase64Link(base64Filename);
        return clickableSpanLink;
    }

    @Override
    public boolean moveNode(String targetNodeUniqueID, String destinationNodeUniqueID) {
        if (areNodesRelated(targetNodeUniqueID, destinationNodeUniqueID)) {
            this.displayToast(context.getString(R.string.toast_error_new_parent_cant_be_one_of_its_children));
            return false;
        } else {
            // Getting current parent node's unique ID of the target node
            Cursor cursorTargetParent = this.sqlite.query("children", new String[]{"father_id"}, "node_id=?", new String[]{targetNodeUniqueID}, null, null, null, null);
            cursorTargetParent.moveToFirst();
            String targetParentUniqueID = cursorTargetParent.getString(0);
            cursorTargetParent.close();
            // Checks for when user wants to move node to the same parent node
            // It is not necessary write operation
            if (targetParentUniqueID.equals(destinationNodeUniqueID)) {
                return false;
            }
            // Getting next available children sequence spot of new parent node
            Cursor cursorMove = this.sqlite.query("children", new String[]{"COUNT(node_id)"}, "father_id=?", new String[]{destinationNodeUniqueID}, null, null, null, null);
            cursorMove.moveToFirst();
            int newAvailableParentSequencePosition = cursorMove.getInt(0) + 1;
            cursorMove.close();
            // Moving to new parent
            ContentValues contentValues = new ContentValues();
            contentValues.put("father_id", destinationNodeUniqueID);
            contentValues.put("sequence", newAvailableParentSequencePosition);
            this.sqlite.update("children", contentValues, "node_id = ?", new String[]{targetNodeUniqueID});
            this.fixChildrenNodeSequence(targetParentUniqueID);
            return true;
        }
    }

    @Override
    public void removeNodeFromBookmarks(String nodeUniqueID) {
        this.sqlite.delete("bookmark", "node_id = ?", new String[]{nodeUniqueID});
        this.fixBookmarkNodeSequence();
    }

    @Override
    public void saveNodeContent(String nodeUniqueID) {
        if (this.mainViewModel.getCurrentNode().isRichText()) {
            Document doc = this.documentBuilder.newDocument();
            StringWriter writer = new StringWriter();
            int next; // The end of the current span and the start of the next one
            int totalContentLength = 0; // Needed to calculate offset for the tag
            int currentPartContentLength = 0; // Needed to calculate offset for the tag
            ArrayList<Element> normalNodes = new ArrayList<>(); // Store all normal tags in order
            ArrayList<Object> offsetObjects = new ArrayList<>(); // Stores all objects with offset values to be processed later
            // Can't get justification for all items that have offset (except tables), so the best next
            // thing I can do is save last detected justification value and used it when creating those nodes
            String lastFoundJustification = "left";
            this.sqlite.beginTransaction();
            // Deleting data from codebox and grid tables. It can be recreated from the nodeContent
            this.sqlite.delete("codebox", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("grid", "node_id = ?", new String[]{nodeUniqueID});
            // Deleting images, latex code (but not files) from database
            this.sqlite.delete("image", "node_id = ? AND time = 0", new String[]{nodeUniqueID});
            try {
                for (ScNodeContent scNodeContent : this.mainViewModel.getNodeContent().getValue()) {
                    if (scNodeContent.getContentType() == 0) {
                        // To not add content of the the span that is being processed
                        // set addContent to false. Needed because not all elements of the node
                        // have content that is displayed for user in text form.
                        boolean addContent;
                        ScNodeContentText scNodeContentText = (ScNodeContentText) scNodeContent;
                        SpannableStringBuilder nodeContent = scNodeContentText.getContent();
                        // Iterating through the spans of the nodeContentText
                        // and adding information depending on Span class
                        // for more information on span open appropriate span class. Furthermore,
                        // other elements have different tag name than "rich_text". For those elements
                        // new (appropriate) tags will be created and added to appropriate list.
                        for (int i = 0; i < nodeContent.length(); i = next) {
                            addContent = true;
                            next = nodeContent.nextSpanTransition(i, nodeContent.length(), Object.class);
                            Object[] spans = nodeContent.getSpans(i, next, Object.class);
                            Element element = doc.createElement("rich_text");
                            for (Object span : spans) {
                                if (span instanceof AlignmentSpan) {
                                    AlignmentSpan alignmentSpan = (AlignmentSpan) span;
                                    if (alignmentSpan.getAlignment() == Layout.Alignment.ALIGN_CENTER) {
                                        element.setAttribute("justification", "center");
                                        lastFoundJustification = "center";
                                    } else if (alignmentSpan.getAlignment() == Layout.Alignment.ALIGN_OPPOSITE) {
                                        element.setAttribute("justification", "right");
                                        lastFoundJustification = "right";
                                    } else {
                                        lastFoundJustification = "left";
                                    }
                                } else if (span instanceof BackgroundColorSpanCustom) {
                                    BackgroundColorSpanCustom backgroundColorSpan = (BackgroundColorSpanCustom) span;
                                    String backgroundColor = String.format("#%1$s", Integer.toHexString(backgroundColorSpan.getBackgroundColor()).substring(2));
                                    element.setAttribute("background", backgroundColor);
                                } else if (span instanceof ClickableSpanNode) {
                                    ClickableSpanNode clickableSpanNode = (ClickableSpanNode) span;
                                    String attributeValue = String.format("node %1$s", clickableSpanNode.getNodeUniqueID());
                                    if (clickableSpanNode.getLinkAnchorName() != null) {
                                        attributeValue = String.format("%1$s %2$s", attributeValue, clickableSpanNode.getLinkAnchorName());
                                    }
                                    element.setAttribute("link", attributeValue);
                                } else if (span instanceof ClickableSpanLink) {
                                    ClickableSpanLink clickableSpanLink = (ClickableSpanLink) span;
                                    element.setAttribute("link", String.format("%1$s %2$s", clickableSpanLink.getLinkType(), clickableSpanLink.getBase64Link()));
                                } else if (span instanceof ImageSpanFile) {
                                    // Attached file
                                    addContent = false;
                                    ImageSpanFile imageSpanFile = (ImageSpanFile) span;
                                    imageSpanFile.setNewOffset(currentPartContentLength + totalContentLength);
                                    imageSpanFile.setJustification(lastFoundJustification);
                                    offsetObjects.add(imageSpanFile);
                                } else if (span instanceof ClickableSpanFile) {
                                    // Attached File text part
                                    addContent = false;
                                } else if (span instanceof ForegroundColorSpan) {
                                    ForegroundColorSpan foregroundColorSpan = (ForegroundColorSpan) span;
                                    String backgroundColor = String.format("#%1$s", Integer.toHexString(foregroundColorSpan.getForegroundColor()).substring(2));
                                    element.setAttribute("foreground", backgroundColor);
                                } else if (span instanceof ImageSpanAnchor) {
                                    addContent = false;
                                    ImageSpanAnchor imageSpanAnchor = (ImageSpanAnchor) span;
                                    imageSpanAnchor.setNewOffset(currentPartContentLength + totalContentLength);
                                    imageSpanAnchor.setJustification(lastFoundJustification);
                                    offsetObjects.add(imageSpanAnchor);
                                } else if (span instanceof ImageSpanImage) {
                                    addContent = false;
                                    ImageSpanImage imageSpanImage = (ImageSpanImage) span;
                                    imageSpanImage.setNewOffset(currentPartContentLength + totalContentLength);
                                    imageSpanImage.setJustification(lastFoundJustification);
                                    offsetObjects.add(imageSpanImage);
                                } else if (span instanceof ImageSpanLatex) {
                                    addContent = false;
                                    ImageSpanLatex imageSpanLatex = (ImageSpanLatex) span;
                                    imageSpanLatex.setNewOffset(currentPartContentLength + totalContentLength);
                                    imageSpanLatex.setJustification(lastFoundJustification);
                                    offsetObjects.add(imageSpanLatex);
                                } else if (span instanceof LeadingMarginSpan.Standard) {
                                    LeadingMarginSpan.Standard leadingMarginSpan = (LeadingMarginSpan.Standard) span;
                                    int indent = leadingMarginSpan.getLeadingMargin(true) / 40;
                                    element.setAttribute("indent", String.valueOf(indent));
                                } else if (span instanceof TypefaceSpanCodebox) {
                                    addContent = false;
                                    TypefaceSpanCodebox typefaceSpanCodebox = ((TypefaceSpanCodebox) span).clone();
                                    typefaceSpanCodebox.setSpanContent(nodeContent.subSequence(i, next).toString());
                                    typefaceSpanCodebox.setNewOffset(currentPartContentLength + totalContentLength);
                                    typefaceSpanCodebox.setJustification(lastFoundJustification);
                                    offsetObjects.add(typefaceSpanCodebox);
                                } else if (span instanceof RelativeSizeSpan) {
                                    element.setAttribute("scale", this.saveRelativeSizeSpan((RelativeSizeSpan) span));
                                } else if (span instanceof StrikethroughSpan) {
                                    element.setAttribute("strikethrough", "true");
                                } else if (span instanceof StyleSpanBold) {
                                    element.setAttribute("weight", "heavy");
                                } else if (span instanceof StyleSpanItalic) {
                                    element.setAttribute("style", "italic");
                                } else if (span instanceof SubscriptSpan) {
                                    element.setAttribute("scale", "sub");
                                } else if (span instanceof SuperscriptSpan) {
                                    element.setAttribute("scale", "sup");
                                } else if (span instanceof TypefaceSpanFamily) {
                                    element.setAttribute("family", "monospace");
                                } else if (span instanceof URLSpanWebs) {
                                    URLSpanWebs urlSpanWebs = (URLSpanWebs) span;
                                    element.setAttribute("link", String.format("webs %1$s", urlSpanWebs.getURL()));
                                } else if (span instanceof UnderlineSpan) {
                                    element.setAttribute("underline", "single");
                                }
                            }
                            if (addContent) {
                                element.setTextContent(nodeContent.subSequence(i, next).toString());
                                // If span content is being added to the element it's length
                                // has to be added to the length of the node
                                currentPartContentLength += (next - i);
                                normalNodes.add(element);
                            }
                        }
                        totalContentLength += currentPartContentLength;
                        currentPartContentLength = 0;
                    } else {
                        ScNodeContentTable scNodeContentTable = (ScNodeContentTable) scNodeContent;
                        scNodeContentTable.setNewOffset(currentPartContentLength + totalContentLength);
                        offsetObjects.add(scNodeContentTable);
                    }
                }
                // Extra char offset. Because when app creates nodeContent it had to take -1
                // off of the totalCharOffset, so while saving it has to add it back. Otherwise
                // content in the CherryTree (and SourCherry) will look differently every time.
                // In the end it will cause a crash.
                int extraCharOffset = 0;
                // Stores all attached file offsets. Later goes through all the node offset's in the
                // image table. If it does not find an offset in the list - deletes it from the table
                ArrayList<Integer> attachedFileOffset = new ArrayList<>();
                TypefaceSpanCodebox collectedCodebox = null;
                boolean hasImage = false;
                boolean hasCodebox = false;
                boolean hasTable = false;
                // Writing all offset elements to appropriate tables
                for (Object offsetObject : offsetObjects) {
                    if (offsetObject instanceof TypefaceSpanCodebox) {
                        // When inserting text (user typing) inside QuoteSpan EditText for some
                        // reason will create multiple spans following one another instead of one
                        // long span. That will create multiple Codebox entries for the same codebox
                        // in the database. Recreating codebox from multiple spans can produce
                        // unexpected results. To fix it all spans that have the same offset have to
                        // be merged into one.
                        if (collectedCodebox == null) {
                            // First time encountering codebox after writing another element
                            collectedCodebox = (TypefaceSpanCodebox) offsetObject;
                        } else {
                            // Multiple consecutive codeboxes
                            TypefaceSpanCodebox typefaceSpanCodebox = (TypefaceSpanCodebox) offsetObject;
                            if (typefaceSpanCodebox.getNewOffset() == collectedCodebox.getNewOffset()) {
                                // If offset is the same as in the previous codebox - merge content
                                collectedCodebox.setSpanContent(collectedCodebox.getSpanContent() + typefaceSpanCodebox.getSpanContent());
                            } else {
                                // If offsets are different join write the previous codebox to
                                // database and set the new one to the variable
                                hasCodebox = true;
                                this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                                extraCharOffset++;
                                collectedCodebox = typefaceSpanCodebox;
                            }
                        }
                    } else if (offsetObject instanceof ImageSpanFile) {
                        if (collectedCodebox != null) {
                            // Previous element was a codebox - write to database and set to null
                            hasCodebox = true;
                            this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                            extraCharOffset++;
                            collectedCodebox = null;
                        }
                        hasImage = true;
                        ImageSpanFile imageSpanFile = (ImageSpanFile) offsetObject;
                        this.saveImageSpanFile(
                                imageSpanFile,
                                nodeUniqueID,
                                imageSpanFile.getNewOffset() + extraCharOffset,
                                lastFoundJustification
                        );
                        attachedFileOffset.add(imageSpanFile.getNewOffset() + extraCharOffset);
                        extraCharOffset++;
                    } else if (offsetObject instanceof ImageSpanAnchor) {
                        if (collectedCodebox != null) {
                            // Previous element was a codebox - write to database and set to null
                            hasCodebox = true;
                            this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                            extraCharOffset++;
                            collectedCodebox = null;
                        }
                        hasImage = true;
                        ImageSpanAnchor imageSpanAnchor = (ImageSpanAnchor) offsetObject;
                        this.saveImageSpanAnchor(
                                imageSpanAnchor,
                                nodeUniqueID,
                                imageSpanAnchor.getNewOffset() + extraCharOffset,
                                lastFoundJustification
                        );
                        attachedFileOffset.add(imageSpanAnchor.getNewOffset() + extraCharOffset);
                        extraCharOffset++;
                    } else if (offsetObject instanceof ScNodeContentTable) {
                        if (collectedCodebox != null) {
                            // Previous element was a codebox - write to database and set to null
                            hasCodebox = true;
                            this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                            extraCharOffset++;
                            collectedCodebox = null;
                        }
                        hasTable = true;
                        ScNodeContentTable scNodeContentTable = (ScNodeContentTable) offsetObject;
                        this.saveScNodeContentTable(
                                doc,
                                writer,
                                scNodeContentTable,
                                nodeUniqueID,
                                scNodeContentTable.getNewOffset() + extraCharOffset
                        );
                        extraCharOffset++;
                    } else if (offsetObject instanceof ImageSpanLatex) {
                        if (collectedCodebox != null) {
                            // Previous element was a codebox - write to database and set to null
                            hasCodebox = true;
                            this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                            extraCharOffset++;
                            collectedCodebox = null;
                        }
                        hasImage = true;
                        ImageSpanLatex imageSpanLatex = (ImageSpanLatex) offsetObject;
                        this.saveImageSpanLatex(
                                imageSpanLatex,
                                nodeUniqueID,
                                imageSpanLatex.getNewOffset() + extraCharOffset,
                                lastFoundJustification
                        );
                        attachedFileOffset.add(imageSpanLatex.getNewOffset() + extraCharOffset);
                        extraCharOffset++;
                    } else if (offsetObject instanceof ImageSpanImage) {
                        if (collectedCodebox != null) {
                            // Previous element was a codebox - write to database and set to null
                            hasCodebox = true;
                            this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                            extraCharOffset++;
                            collectedCodebox = null;
                        }
                        hasImage = true;
                        ImageSpanImage imageSpanImage = (ImageSpanImage) offsetObject;
                        this.saveImageSpanImage(
                                imageSpanImage,
                                nodeUniqueID,
                                imageSpanImage.getNewOffset() + extraCharOffset,
                                lastFoundJustification
                        );
                        attachedFileOffset.add(imageSpanImage.getNewOffset() + extraCharOffset);
                        extraCharOffset++;
                    }
                }
                if (collectedCodebox != null) {
                    // Might be that last element if offsetObject was codebox - writing it to database
                    hasCodebox = true;
                    this.saveTypefaceSpanCodebox(collectedCodebox, nodeUniqueID, extraCharOffset);
                    collectedCodebox = null;
                }
                // Deleting all data from image table, that was removed by user from nodeContent
                Cursor cursor = this.sqlite.query("image", new String[]{"offset"}, "node_id = ?", new String[]{nodeUniqueID}, null, null, null);
                this.sqlite.beginTransaction();
                try {
                    while (cursor.moveToNext()) {
                        if (!attachedFileOffset.contains(cursor.getInt(0))) {
                            this.sqlite.delete("image", "node_id = ? AND offset = ?", new String[]{nodeUniqueID, cursor.getString(0)});
                        }
                    }
                    this.sqlite.setTransactionSuccessful();
                } finally {
                    this.sqlite.endTransaction();
                }
                cursor.close();
                Node node = doc.createElement("node");
                for (Element element : normalNodes) {
                    node.appendChild(element);
                }
                writer.getBuffer().setLength(0);
                try {
                    transformer.transform(new DOMSource(node), new StreamResult(writer));
                } catch (TransformerException e) {
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_node));
                    return;
                }
                // Updating nodeContent - text
                ContentValues contentValues = new ContentValues();
                contentValues.put("txt", writer.toString());
                contentValues.put("has_codebox", hasCodebox ? 1 : 0);
                contentValues.put("has_table", hasTable ? 1 : 0);
                contentValues.put("has_image", hasImage ? 1 : 0);
                contentValues.put("ts_lastsave", System.currentTimeMillis() / 1000);
                this.sqlite.update("node", contentValues, "node_id=?", new String[]{nodeUniqueID});
                this.sqlite.setTransactionSuccessful();
            } finally {
                this.sqlite.endTransaction();
            }
        } else {
            ScNodeContentText scNodeContentText = (ScNodeContentText) this.mainViewModel.getNodeContent().getValue().get(0);
            SpannableStringBuilder nodeContent = scNodeContentText.getContent();
            ContentValues contentValues = new ContentValues();
            contentValues.put("txt", nodeContent.toString());
            contentValues.put("ts_lastsave", System.currentTimeMillis() / 1000);
            this.sqlite.update("node", contentValues, "node_id=?", new String[]{nodeUniqueID});
        }
    }

    /**
     * Saves ImageSpan found in nodeContent in to the appropriate table of the database
     * @param imageSpanImage ImageSpan object from nodeContent
     * @param nodeUniqueID unique ID of the node associated with image
     * @param offset offset of the image
     * @param lastFoundJustification justification of the image
     */
    private void saveImageSpanImage(ImageSpanImage imageSpanImage, String nodeUniqueID, int offset, String lastFoundJustification) {
        Drawable drawable = imageSpanImage.getDrawable();
        // Hopefully it's always a Bitmap drawable, because I get it from the same source
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("node_id", nodeUniqueID);
            contentValues.put("offset", offset);
            contentValues.put("justification", imageSpanImage.getJustification());
            contentValues.put("anchor", "");
            contentValues.put("png", byteArrayOutputStream.toByteArray());
            contentValues.put("filename", "");
            contentValues.put("link", "");
            contentValues.put("time", 0);
            this.sqlite.insert("image", null, contentValues);
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Saves ImageSpanLatex found in nodeContent in to the appropriate table of the database
     * @param imageSpanLatex ImageSpanLatex object from nodeContent
     * @param nodeUniqueID unique ID of the node associated with LaTeX image
     * @param offset offset of the LaTeX image
     * @param lastFoundJustification justification of the LaTeX image
     */
    private void saveImageSpanLatex(ImageSpanLatex imageSpanLatex, String nodeUniqueID, int offset, String lastFoundJustification) {
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("node_id", nodeUniqueID);
            contentValues.put("offset", offset);
            contentValues.put("justification", imageSpanLatex.getJustification());
            contentValues.put("anchor", "");
            contentValues.put("png", imageSpanLatex.getLatexCode().getBytes());
            contentValues.put("filename", "__ct_special.tex");
            contentValues.put("link", "");
            contentValues.put("time", 0);
            this.sqlite.insert("image", null, contentValues);
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Saves ImageSpanAnchor found in nodeContent in to the appropriate table of the database
     * @param imageSpanAnchor ImageSpanAnchor object from nodeContent
     * @param nodeUniqueID unique ID of the node associated with Anchor image
     * @param offset offset of the Anchor image
     * @param lastFoundJustification justification of the Anchor image
     */
    private void saveImageSpanAnchor(ImageSpanAnchor imageSpanAnchor, String nodeUniqueID, int offset, String lastFoundJustification) {
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("node_id", nodeUniqueID);
            contentValues.put("offset", offset);
            contentValues.put("justification", imageSpanAnchor.getJustification());
            contentValues.put("anchor", imageSpanAnchor.getAnchorName());
            contentValues.put("filename", "");
            contentValues.put("link", "");
            contentValues.put("time", 0);
            this.sqlite.insert("image", null, contentValues);
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Saves ImageSpanFile found in nodeContent in to the appropriate table of the database
     * @param imageSpanFile ImageSpanFile object from nodeContent
     * @param nodeUniqueID unique ID of the node associated with the file
     * @param offset offset of the file
     * @param lastFoundJustification justification of the file
     */
    private void saveImageSpanFile(ImageSpanFile imageSpanFile, String nodeUniqueID, int offset, String lastFoundJustification) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("offset", offset);
        contentValues.put("justification", lastFoundJustification);
        try {
            this.sqlite.beginTransaction();
            if (imageSpanFile.isFromDatabase()) {
                // If file was loaded from the database, so only it's offset and justification changed
                // filename = '' is necessary to make sure that any other type of 'image' does not have
                // the same offset. Just in case it was written in to database before current file.
                // The same applies for the check for '__ct_special.tex'
                this.sqlite.update("image", contentValues, "node_id = ? AND offset = ? AND NOT filename = '' AND NOT filename = '__ct_special.tex'", new String[]{nodeUniqueID, imageSpanFile.getOriginalOffset()});
            } else {
                // Inserting the file in to the image table
                Uri fileUri = Uri.parse(imageSpanFile.getFileUri());
                try (
                        InputStream fileInputSteam = this.context.getContentResolver().openInputStream(fileUri);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
                ) {
                    byte[] buf = new byte[4 * 1024];
                    int length;
                    while ((length = fileInputSteam.read(buf)) != -1) {
                        byteArrayOutputStream.write(buf);
                    }
                    contentValues.put("png", byteArrayOutputStream.toByteArray());
                } catch (IOException e) {
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_database_changes));
                }
                contentValues.put("node_id", nodeUniqueID);
                contentValues.put("anchor", "");
                contentValues.put("filename", imageSpanFile.getFilename());
                contentValues.put("link", "");
                contentValues.put("time", String.valueOf(System.currentTimeMillis() / 1000));
                this.sqlite.insert("image", null, contentValues);
                // Updating node table to reflect that user inserted a file
                contentValues.clear();
                contentValues.put("has_image", 1);
                this.sqlite.update("node", contentValues, "node_id = ?", new String[]{nodeUniqueID});
            }
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Whites codebox entry in to codebox table
     * @param nodeUniqueID unique ID of the node to which codebox belongs to
     * @param typefaceSpanCodebox span holding most of the codebox data
     * @param extraCharOffset codebox offset it has to be inserted into the node content
     */
    private void saveTypefaceSpanCodebox(TypefaceSpanCodebox typefaceSpanCodebox, String nodeUniqueID, int extraCharOffset) {
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("node_id", nodeUniqueID);
            contentValues.put("offset", typefaceSpanCodebox.getNewOffset() + extraCharOffset);
            contentValues.put("justification", typefaceSpanCodebox.getJustification());
            contentValues.put("txt", typefaceSpanCodebox.getSpanContent());
            contentValues.put("syntax", typefaceSpanCodebox.getSyntaxHighlighting());
            contentValues.put("width", typefaceSpanCodebox.getFrameWidth());
            contentValues.put("height", typefaceSpanCodebox.getFrameHeight());
            contentValues.put("is_width_pix", typefaceSpanCodebox.isWidthInPixel());
            contentValues.put("do_highl_bra", typefaceSpanCodebox.isHighlightBrackets());
            contentValues.put("do_show_linenum", typefaceSpanCodebox.isShowLineNumbers());
            this.sqlite.insert("codebox", null, contentValues);
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Saves ScNodeContentTable found in nodeContent in to the appropriate table of the database
     * @param doc Document object instance to create Element objects
     * @param writer StringWriter object to help convert Document object to String
     * @param scNodeContentTable ScNodeContentTable from the nodeContent
     * @param nodeUniqueID unique ID of the node associated with the file
     * @param offset offset of the table
     */
    private void saveScNodeContentTable(Document doc, StringWriter writer, ScNodeContentTable scNodeContentTable, String nodeUniqueID, int offset) {
        Element tableElement = doc.createElement("table");
        tableElement.setAttribute("col_widths", scNodeContentTable.getColWidths());
        tableElement.setAttribute("is_light", String.valueOf(scNodeContentTable.getLightInterface()));
        // Adding table content
        for (int i = 1; i < scNodeContentTable.getContent().size(); i++) {
            Element rowElement = doc.createElement("row");
            for (CharSequence cell : scNodeContentTable.getContent().get(i)) {
                Element cellElement = doc.createElement("cell");
                cellElement.setTextContent(cell.toString());
                rowElement.appendChild(cellElement);
            }
            tableElement.appendChild(rowElement);
        }
        // Adding header at the end of the table tag
        Element headerRowElement = doc.createElement("row");
        for (CharSequence cell: scNodeContentTable.getContent().get(0)) {
            Element cellElement = doc.createElement("cell");
            cellElement.setTextContent(cell.toString());
            headerRowElement.appendChild(cellElement);
        }
        tableElement.appendChild(headerRowElement);
        writer.getBuffer().setLength(0);
        writer.getBuffer().trimToSize();
        try {
            this.transformer.transform(new DOMSource(tableElement), new StreamResult(writer));
        } catch (TransformerException e) {
            this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_table));
            return;
        }
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("node_id", nodeUniqueID);
            contentValues.put("offset", offset);
            contentValues.put("justification", scNodeContentTable.getJustification());
            contentValues.put("txt", writer.toString());
            contentValues.put("col_min", scNodeContentTable.getColMin());
            contentValues.put("col_max", scNodeContentTable.getColMax());
            this.sqlite.insert("grid", null, contentValues);
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Returns scale attribute value for the rich-text tag depending on the size change of the span
     * @param relativeSizeSpan RelativeSizeSpan from nodeContent
     * @return scale attribute value
     */
    private String saveRelativeSizeSpan(RelativeSizeSpan relativeSizeSpan) {
        float size = relativeSizeSpan.getSizeChange();
        if (size == 1.75f) {
            return "h1";
        } else if (size == 1.5f) {
            return "2";
        } else if (size == 1.25f) {
            return "h3";
        } else if (size == 1.20f) {
            return "h4";
        } else if (size == 1.15f) {
            return "h5";
        } else if (size == 1.10f) {
            return "h6";
        } else {
            // size == 0.75f
            return "small";
        }
    }

    @Override
    public ArrayList<ScSearchNode> search(Boolean noSearch, String query) {
        if (noSearch) {
            // If user marked that filter should omit nodes and/or node children from filter results
            ArrayList<ScSearchNode> searchResult = new ArrayList<>();

            Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);

            while (cursor.moveToNext()) {
                if (cursor.getInt(10) == 0) {
                    // If node and subnodes are not selected to be excluded from search
                    String nodeUniqueID = String.valueOf(cursor.getInt(0));
                    boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
                    // Main menu node will always be a parent
                    // Main menu item will always be displayed as a parent
                    ScSearchNode result = this.findInNode(cursor, query, hasSubnode, true, false);
                    if (result != null) {
                        searchResult.add(result);
                    }
                    if (hasSubnode) {
                        searchResult.addAll(searchNodesSkippingExcluded(nodeUniqueID, query));
                    }
                } else if (cursor.getInt(10) == 1) {
                    // If only the node is selected to be excluded from search
                    String nodeUniqueID = String.valueOf(cursor.getInt(0));
                    boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
                    if (hasSubnode) {
                        searchResult.addAll(searchNodesSkippingExcluded(nodeUniqueID, query));
                    }
                } else if (cursor.getInt(10) == 2) {
                    // if only subnodes are selected to be excluded from search
                    String nodeUniqueID = String.valueOf(cursor.getInt(0));
                    boolean hasSubnodes = this.hasSubnodes(nodeUniqueID);
                    // Main menu node will always be a parent
                    // Main menu item will always be displayed as parent
                    ScSearchNode result = this.findInNode(cursor, query, hasSubnodes, true, false);
                    if (result != null) {
                        searchResult.add(result);
                    }
                }
            }
            cursor.close();
            return searchResult;
        } else {
            Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=0 ORDER BY sequence ASC", null);
            ArrayList<ScSearchNode> searchResult = new ArrayList<>();
            while (cursor.moveToNext()) {
                String nodeUniqueID = String.valueOf(cursor.getInt(0));
                boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
                // Main menu node will always be parent
                // Main menu item will displayed as parent
                ScSearchNode result = this.findInNode(cursor, query, hasSubnode, true, false);
                if (result != null) {
                    searchResult.add(result);
                }
                if (hasSubnode) {
                    searchResult.addAll(this.searchAllNodes(nodeUniqueID, query));
                }
            }
            cursor.close();
            return searchResult;
        }
    }

    @Override
    public void updateNodeProperties(String nodeUniqueID, String name, String progLang, String noSearchMe, String noSearchCh) {
        Cursor cursor = this.sqlite.query("node", new String[]{"txt", "is_richtxt"}, "node_id=?", new String[]{nodeUniqueID}, null, null, null, null);
        cursor.moveToFirst();
        boolean isRichText = cursor.getInt(1) == 1;
        this.sqlite.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", name);
            if (isRichText && !progLang.equals("custom-colors")) {
                // If user chose to convert rich text type node to plain text or automatic system highlighting type
                contentValues.put("txt", this.convertRichTextNodeContentToPlainText(cursor.getString(0), nodeUniqueID).toString());
                this.sqlite.delete("codebox", "node_id = ?", new String[]{nodeUniqueID});
                this.sqlite.delete("grid", "node_id = ?", new String[]{nodeUniqueID});
                this.sqlite.delete("image", "node_id = ?", new String[]{nodeUniqueID});
                contentValues.put("has_codebox", 0);
                contentValues.put("has_table", 0);
                contentValues.put("has_image", 0);
            } else if (!isRichText && progLang.equals("custom-colors")) {
                // If user chose to convert plain text or automatic system highlighting type node to rich text type
                StringWriter writer = new StringWriter();
                Document doc = this.documentBuilder.newDocument();
                Node node = doc.createElement("node");
                Element element = doc.createElement("rich_text");
                element.setTextContent(cursor.getString(0));
                node.appendChild(element);
                try {
                    this.transformer.transform(new DOMSource(node), new StreamResult(writer));
                } catch (TransformerException e) {
                    this.displayToast(this.context.getString(R.string.toast_error_failed_to_save_node));
                    return;
                }
                contentValues.put("txt", writer.toString());
            }
            cursor.close();
            contentValues.put("syntax", progLang);
            contentValues.put("is_richtxt", progLang.equals("custom-colors") ? 1 : 0);
            contentValues.put("level", this.convertNoSearchToLevel(noSearchMe, noSearchCh));
            contentValues.put("ts_lastsave", String.valueOf(System.currentTimeMillis() / 1000));
            this.sqlite.update("node", contentValues, "node_id=?", new String[]{nodeUniqueID});
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Checks if node is a subnode if another node
     * Not really sure if it does not return false positives
     * However all my tests worked
     * @param targetNodeUniqueID unique ID of the node that needs to be check if it's a parent node
     * @param destinationNodeUniqueID unique ID of the node that has to be check if it's a child
     * @return true - if target node is a parent of destination node
     */
    private boolean areNodesRelated(String targetNodeUniqueID, String destinationNodeUniqueID) {
        ArrayList<String> heredity = new ArrayList<>();
        heredity.add(destinationNodeUniqueID);
        while (true) {
            Cursor cursor = this.sqlite.query("children", new String[]{"father_id"}, "node_id = ?", new String[]{destinationNodeUniqueID}, null, null, null, null);
            if (cursor.moveToFirst()) {
                destinationNodeUniqueID = cursor.getString(0);
                heredity.add(destinationNodeUniqueID);
                if (destinationNodeUniqueID.equals("0")) {
                    cursor.close();
                    break;
                }
            } else {
                cursor.close();
                break;
            }
            cursor.close();
        }
        return heredity.contains(targetNodeUniqueID);
    }

    /**
     * Coverts codebox string retrieved from codebox table in database to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param codebox string that needs to be converted
     * @return StringBuilder that can be added to the node StringBuilder at the proper offset
     */
    private StringBuilder convertCodeboxToPlainText(String codebox) {
        StringBuilder codeboxContent = new StringBuilder();
        codeboxContent.append("\n");
        codeboxContent.append(getSeparator());
        codeboxContent.append("\n");
        codeboxContent.append(codebox);
        codeboxContent.append("\n");
        codeboxContent.append(getSeparator());
        codeboxContent.append("\n");
        return codeboxContent;
    }

    /**
     * Converts latex string retrieved from image table in database to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param latex latex string that needs to be converted
     * @return StringBuilder that can be added to the content node StringBuilder at the proper offset
     */
    private StringBuilder convertLatexToPlainText(String latex) {
        StringBuilder latexContent = new StringBuilder();
        latexContent.append(latex);
        latexContent.delete(0, 79);
        latexContent.delete(latexContent.length()-14, latexContent.length());
        latexContent.insert(0,getSeparator());
        latexContent.insert(0, "\n");
        latexContent.append(getSeparator());
        latexContent.append("\n");
        return latexContent;
    }

    /**
     * Converts Exclude from search This node and The Subnodes values
     * from int that are saved in SQL databases to separate
     * noSearchMe and noSearchCh values that are used in XML databases and throughout
     * the code in this app
     * @param level value that was saved in SQL database
     * @return Array that holds values {noSearchMe, ne SearchCh}
     */
    private byte[] convertLevelToNoSearch(int level) {
        byte[] noSearch = new byte[2];
        switch (level) {
            case 0:
                noSearch[0] = 0;
                noSearch[1] = 0;
                break;
            case 1:
                noSearch[0] = 1;
                noSearch[1] = 0;
                break;
            case 2:
                noSearch[0] = 0;
                noSearch[1] = 1;
                break;
            case 3:
                noSearch[0] = 1;
                noSearch[1] = 1;
        }
        return noSearch;
    }

    /**
     * Convert noSearchMe and noSearchCh values that are used in XML databases and throughout
     * the code in this app to int value Level that is used in SQL type databases
     * @param noSearchMe exclude this node from search value. 0 - search the node, 1 - exclude
     * @param noSearchCh exclude the subnode from search value. 0 - search the subnodes, 1 - exclude
     * @return level value. 0 - search the node and subnodes, 1 - exclude the node, 2 - exclude subnodes, 3 - exclude both
     */
    private byte convertNoSearchToLevel(String noSearchMe, String noSearchCh) {
        byte level = 0;
        if (noSearchMe.equals("1") && noSearchCh.equals("1")) {
            level = 3;
        } else if (noSearchMe.equals("1")) {
            level = 1;
        } else if (noSearchCh.equals("1")) {
            level = 2;
        }
        return level;
    }

    /**
     * Coverts content of provided node (unique ID) from rich-text to plain-text or automatic-syntax-highlighting
     * Conversion adds all the content from the node's rich-text tags to StringBuilder
     * that can be added to the node table txt field
     * @param nodeUniqueID unique id of the node that needs to be converted
     * @return StringBuilder with all the node content without addition tags
     */
    private StringBuilder convertRichTextNodeContentToPlainText(String txt, String nodeUniqueID) {
        StringBuilder nodeContent = new StringBuilder();
        int totalCharOffset = 0;
        // Getting text data of the node
        NodeList nodeList =  this.getDocumentFromString(txt).getElementsByTagName("node").item(0).getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            nodeContent.append(node.getTextContent());
        }
        // Getting offset data for all images (latex, images, files), tables and codeboxes
        // Adding 7 - for codebox, 8 - for table and 9 for image as a second column
        Cursor codeboxTableImageCursor = this.sqlite.rawQuery(new String("SELECT offset, 7 FROM codebox WHERE node_id=? UNION SELECT offset, 8 FROM grid WHERE node_id=? UNION SELECT offset, 9 FROM image WHERE node_id=? ORDER BY offset ASC"), new String[]{nodeUniqueID, nodeUniqueID, nodeUniqueID});
        while (codeboxTableImageCursor.moveToNext()) {
            if (codeboxTableImageCursor.getInt(1) == 7) {
                Cursor cursorCodeboxes = this.sqlite.query("codebox", new String[]{"txt"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, codeboxTableImageCursor.getString(0)}, null, null, "offset ASC", null);
                while (cursorCodeboxes.moveToNext()) {
                    int charOffset = codeboxTableImageCursor.getInt(0) + totalCharOffset;
                    StringBuilder codeboxContent = this.convertCodeboxToPlainText(cursorCodeboxes.getString(0));
                    nodeContent.insert(charOffset, codeboxContent);
                    totalCharOffset += codeboxContent.length() - 1;
                }
                cursorCodeboxes.close();
            }
            if (codeboxTableImageCursor.getInt(1) == 8) {
                Cursor cursorTables = this.sqlite.query("grid", new String[]{"txt"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, codeboxTableImageCursor.getString(0)}, null, null, "offset ASC", null);
                while (cursorTables.moveToNext()) {
                    int charOffset = codeboxTableImageCursor.getInt(0) + totalCharOffset;
                    StringBuilder tableContent = this.convertTableContentToPlainText(cursorTables.getString(0));
                    nodeContent.insert(charOffset, tableContent);
                    totalCharOffset += tableContent.length() - 1;
                }
                cursorTables.close();
            }
            if (codeboxTableImageCursor.getInt(1) == 9) {
                Cursor cursorImages = this.sqlite.query("image", new String[]{"anchor", "png", "filename"}, "node_id=? AND offset=?", new String[]{nodeUniqueID, codeboxTableImageCursor.getString(0)}, null, null, "offset ASC", null);
                while (cursorImages.moveToNext()) {
                    if (cursorImages.getString(2).equals("__ct_special.tex")) {
                        int charOffset = codeboxTableImageCursor.getInt(0) + totalCharOffset;
                        StringBuilder imageContent = this.convertLatexToPlainText(new String(cursorImages.getBlob(1)));
                        nodeContent.insert(charOffset, imageContent);
                        totalCharOffset += imageContent.length() - 1;
                    } else {
                        // For every element, even ones that will not be added
                        // 1 has to be deducted from totalCharOffset
                        // to make node's data be displayed in order
                        totalCharOffset -= 1;
                    }
                }
                cursorImages.close();
            }
        }
        codeboxTableImageCursor.close();
        return nodeContent;
    }

    /**
     * Coverts table string retrieved from grid table in database to a StringBuilder
     * used as part of convertRichTextNodeContentToPlainText function
     * @param table string that needs to be converted
     * @return StringBuilder that can be added to the content node StringBuilder at the proper offset
     */
    private StringBuilder convertTableContentToPlainText(String table) {
        StringBuilder tableContent = new StringBuilder();
        NodeList nodeList = this.getDocumentFromString(table).getElementsByTagName("table").item(0).getChildNodes();
        int tableRowCount = nodeList.getLength();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals("row")) {
                // Header row for the table is kept at the end of the table
                // When converting to string it has to be added to the beginning
                // of the string fro the information to make sense
                if (tableRowCount > 1) {
                    tableContent.append(this.convertTableRowToPlainText(node));
                } else {
                    tableContent.insert(0, this.convertTableRowToPlainText(node));
                }
                tableRowCount--;
            }
        }
        tableContent.insert(0, "\n");
        return tableContent;
    }

    /**
     * Parent node (top) in the drawer menu
     * Used when creating a drawer menu
     * @param nodeUniqueID unique ID of the node that is parent node
     * @return ScNode object with properties of a parent node
     */
    private ScNode createParentNode(String nodeUniqueID) {
        // Creates and returns the node that will be added to the node array as parent node
        Cursor cursor = this.sqlite.query("node", new String[]{"name", "is_richtxt", "syntax", "is_ro"}, "node_id=?", new String[]{String.valueOf(nodeUniqueID)}, null, null,null);
        String parentNodeName;
        if (cursor.move(1)) { // Cursor items start at 1 not 0!!!
            parentNodeName = cursor.getString(0);
        } else {
            return null;
        }
        boolean parentNodeHasSubnodes = hasSubnodes(nodeUniqueID);
        boolean isRichText = cursor.getString(2).equals("custom-colors");
        boolean isBold = ((cursor.getInt(1) >> 1) & 0x01) == 1;
        String foregoundColor = "";
        if (((cursor.getInt(1) >> 2) & 0x01) == 1) {
            foregoundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
        }
        int iconId = cursor.getInt(3) >> 1;
        boolean isReadOnly = (cursor.getInt(3) & 0x01) == 1;
        ScNode node = new ScNode(nodeUniqueID, parentNodeName, true, parentNodeHasSubnodes, false, isRichText, isBold, foregoundColor, iconId, isReadOnly);
        cursor.close();
        return node;
    }

    /**
     * Deletes node and it subnodes from database
     * Difference from deleteNode() is that this function does not do
     * any cleanup functions like fixing sequences of bookmarks
     * and original node's parent sequence of children node
     * @param nodeUniqueID unique ID of the node to delete
     */
    private void deleteNodeChildren(String nodeUniqueID) {
        this.sqlite.beginTransaction();
        try {
            this.sqlite.delete("bookmark", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("children", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("codebox", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("grid", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("image", "node_id = ?", new String[]{nodeUniqueID});
            this.sqlite.delete("node", "node_id = ?", new String[]{nodeUniqueID});
            Cursor childrenNodeUniqueID = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{nodeUniqueID}, null, null, null, null);
            while (childrenNodeUniqueID.moveToNext()) {
                this.deleteNodeChildren(childrenNodeUniqueID.getString(0));
            }
            childrenNodeUniqueID.close();
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Searches through node's content
     * @param cursor cursor that holds all the data of the of the node to search through
     * @param query string to search for
     * @param hasSubnodes true if node has subnodes, else - false
     * @param isParent true if node is a parent node, else - false
     * @param isSubnode isSubnode true if node is a subnode, else - false
     * @return search result object or null if nothing was found
     */
    private ScSearchNode findInNode(Cursor cursor, String query, boolean hasSubnodes, boolean isParent, boolean isSubnode) {
        // This string builder will hold oll text content of the node
        StringBuilder nodeContent = new StringBuilder();
        // As in reader that all the text would be in order user sees it
        // filenames, table and codebox content hast to be inserted in correct location of the string
        // To help calculate that location totalCharOffset is used
        int totalCharOffset = 0;

        // ***Creating node content string
        String nodeSyntax = cursor.getString(3);

        if (nodeSyntax.equals("custom-colors")) {
            // This is formatting for Rich Text and Plain Text nodes
            // Gets all the subnodes/childnodes of selected node
            NodeList nodeContentNodeList = this.getDocumentFromString(cursor.getString(2)).getElementsByTagName("node").item(0).getChildNodes();
            for (int x = 0; x < nodeContentNodeList.getLength(); x++) {
                // Loops through nodes/tags of selected node
                nodeContent.append(nodeContentNodeList.item(x).getTextContent());
            }
            int hasCodebox = cursor.getInt(7);
            int hasTable = cursor.getInt(8);
            int hasImage = cursor.getInt(9);

            // If it is marked that node has codebox, table or image
            if (hasCodebox == 1 || hasTable == 1 || hasImage == 1) {
                //// Building string for SQLQuery
                // Because every type of element (image, table, codeboxes) are in it's own table
                // Only the ones that actually are in the node will be searched
                // For search only text is needed so only offset, and text (filenames too) will be selected
                StringBuilder codeboxTableImageQueryString = new StringBuilder();

                // Depending on how many tables will be searched
                // instances of how many time nodeUniqueID will have to be inserted will differ
                int queryCounter = 0; // This is the counter for that
                if (hasCodebox == 1) {
                    // Means that node has has codeboxes in it
                    codeboxTableImageQueryString.append("SELECT offset, txt, 7 FROM codebox WHERE node_id=? ");
                    queryCounter++;
                }
                if (hasTable == 1) {
                    // Means that node has tables in it
                    if (hasCodebox == 1) {
                        codeboxTableImageQueryString.append("UNION ");
                    }
                    codeboxTableImageQueryString.append("SELECT offset, txt, 8 FROM grid WHERE node_id=? ");
                    queryCounter++;
                }
                if (hasImage == 1) {
                    // Means that node has images (images, anchors or files) in it
                    if (hasCodebox == 1 || hasTable == 1) {
                        codeboxTableImageQueryString.append("UNION ");
                    }
                    codeboxTableImageQueryString.append("SELECT offset, filename, 9 FROM image WHERE node_id=? ");
                    queryCounter++;
                }
                codeboxTableImageQueryString.append("ORDER BY offset ASC");

                /// Creating the array that will be used to insert nodeUniqueIDs
                String[] queryArguments = new String[queryCounter];
                Arrays.fill(queryArguments, cursor.getString(0));
                ///
                ////

                Cursor codeboxTableImageCursor = this.sqlite.rawQuery(codeboxTableImageQueryString.toString(), queryArguments);

                while (codeboxTableImageCursor.moveToNext()) {
                    int charOffset = codeboxTableImageCursor.getInt(0);
                    if (codeboxTableImageCursor.getInt(2) == 9) {
                        if (!codeboxTableImageCursor.getString(1).isEmpty()) {
                            // Text in column 5 means that this line is for file OR LaTeX formula box
                            if (!codeboxTableImageCursor.getString(1).equals("__ct_special.tex")) {
                                // If it is not LaTex file
                                String attachedFileFilename = " " + codeboxTableImageCursor.getString(1) + " ";
                                if (nodeContent.length() < charOffset + totalCharOffset) {
                                    // This check most likely needed in Searcher, but not in Reader
                                    // Because in search some objects (like images) are being skipped, however their offset is still being counted
                                    nodeContent.append(attachedFileFilename);
                                } else {
                                    nodeContent.insert(charOffset + totalCharOffset, attachedFileFilename);
                                }
                                totalCharOffset += attachedFileFilename.length() - 1;
                                continue; // Needed. Otherwise error toast will be displayed. Maybe switch statement would solve this issue.
                            }
                        }
                    } else if (codeboxTableImageCursor.getInt(2) == 7) {
                        // codebox row
                        String codeboxText = codeboxTableImageCursor.getString(1);
                        if (nodeContent.length() < charOffset + totalCharOffset) {
                            // This check most likely needed in Searcher, but not in Reader
                            // Because in search some objects (like images) are being skipped, however their offset is still being counted
                            nodeContent.append(codeboxText);
                        } else {
                            nodeContent.insert(charOffset + totalCharOffset, codeboxText);
                        }
                        totalCharOffset += codeboxText.length() - 1;
                    } else if (codeboxTableImageCursor.getInt(2) == 8) {
                        StringBuilder tableContent = new StringBuilder();
                        // table row
                        NodeList tableRows = this.getDocumentFromString(codeboxTableImageCursor.getString(1)).getElementsByTagName("table").item(0).getChildNodes();
                        // Adding all rows to arraylist
                        ArrayList<String> tableRowArray = new ArrayList<>();
                        for (int row = 0; row < tableRows.getLength(); row++) {
                            if (tableRows.item(row).getNodeName().equals("row")) {
                                // For table content from SQL database spaces around each cell needs to be added
                                // because there aren't any
                                // All cells from one row has to be connected to one string that represents a row
                                // Otherwise it might be not possible to put table header to the top of the table
                                StringBuilder rowStringBuilder = new StringBuilder();
                                NodeList cells = tableRows.item(row).getChildNodes();
                                for (int cell = 0; cell < cells.getLength(); cell++) {
                                    rowStringBuilder.append(" ").append(cells.item(cell).getTextContent()).append(" ");
                                }
                                tableRowArray.add(rowStringBuilder.toString());
                            }
                        }

                        // Adding the last row of the table to string builder as first because that's where header of the table is located
                        tableContent.append(tableRowArray.get(tableRowArray.size() - 1));
                        // Rest of the rows can be added in order
                        for (int x = 0; x < tableRowArray.size() - 1; x++) {
                            tableContent.append(tableRowArray.get(x));
                        }

                        // Adding table's content to nodes content string builder
                        if (nodeContent.length() < charOffset + totalCharOffset) {
                            // This check most likely needed in Searcher, but not in Reader
                            // Because in search some objects (like images) are being skipped, however their offset is still being counted
                            nodeContent.append(tableContent);
                        } else {
                            nodeContent.insert(charOffset + totalCharOffset, tableContent);
                        }
                        // Changing total offset value with a value of the table content, because CherryTree uses different GUI toolkit
                        // And without doing this the first element with offset would mess node content order (or maybe that's by design)
                        totalCharOffset += tableContent.length() - 1;
                    }
                }
                codeboxTableImageCursor.close();
            }
        } else if (nodeSyntax.equals("plain-text")) {
            // Plain text node does not have any formatting and has not node embedded in to it
            nodeContent.append(cursor.getString(2));
        } else {
            // Node is Code Node. It's just a big CodeBox with no dimensions
            nodeContent.append(cursor.getString(2));
        }

        // ***Search
        int queryLength = query.length();
        int resultCount = 0;
        int index = 0;
        StringBuilder samples = new StringBuilder(); // This will hold 3 samples to show to user

        // Removing all spaces and new line character from the node content string
        String preparedNodeContent = nodeContent.toString().toLowerCase().replaceAll("\n", " ").replaceAll(" +", " ");

        while (index != -1) {
            index = preparedNodeContent.indexOf(query, index);
            if (index != -1) {
                // if match to search query was found in the node's content
                if (resultCount < 3 ) {
                    // Results display only first three found instances of search query
                    int startIndex = 0; // Start of sample substring that will be created
                    int endIndex = preparedNodeContent.length(); // End of sample substring that will be created
                    String sampleStart = "";
                    String sampleEnd = "";
                    if (index > 20) {
                        // if index is further than 20 symbols from the start of the node content
                        // ... are added to the start of the sample
                        // and only 20 preceding symbols before query match are showed
                        startIndex = index - 20;
                        sampleStart = "...";
                    }
                    if ((index + queryLength + 20) < endIndex) {
                        // if index is more than 20 symbols from the end of the node content
                        // ... are added to the end of the sample
                        // and only 20 proceeding symbols before query match are showed
                        endIndex = index + queryLength + 20;
                        sampleEnd = "...";
                    }

                    // Building a sample for search result from using previously formatted parts
                    StringBuilder sample = new StringBuilder();
                    sample.append(sampleStart);
                    sample.append(preparedNodeContent.substring(startIndex, endIndex).trim());
                    sample.append(sampleEnd);
                    sample.append("<br/>");
                    samples.append(sample);
                }

                resultCount++;
                index += queryLength; // moving search start to the end of the last position that search query was found
            }
        }

        if (resultCount > 0) {
            // if node count of matches is more than 0 that a match of q query was found
            boolean isBold = ((cursor.getInt(6) >> 1) & 0x01) == 1;
            String foregroundColor = "";
            if (((cursor.getInt(6) >> 2) & 0x01) == 1) {
                foregroundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
            }
            int iconId = cursor.getInt(5) >> 1;
            boolean isReadOnly = (cursor.getInt(5) & 0x01) == 1;
            return new ScSearchNode(cursor.getString(0), cursor.getString(1), isParent, hasSubnodes, isSubnode, cursor.getString(3).equals("custom-colors"), isBold, foregroundColor, iconId, isReadOnly, query, resultCount, samples.toString());
        } else {
            return null;
        }
    }

    /**
     * Reorders bookmark table's node sequence
     * Removes any gaps that might have been left after deleting node
     * or removing it from bookmarks
     */
    private void fixBookmarkNodeSequence() {
        this.sqlite.beginTransaction();
        try {
            Cursor cursor = this.sqlite.query("bookmark", new String[]{"node_id"}, null, null, null, null, "node_id ASC", null);
            int counter = 1;
            ContentValues contentValues = new ContentValues();
            while (cursor.moveToNext()) {
                contentValues.clear();
                contentValues.put("sequence", counter);
                this.sqlite.update("bookmark", contentValues, "node_id = ?", new String[]{cursor.getString(0)});
                counter++;
            }
            cursor.close();
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
    }

    /**
     * Orders children of the node in sequence (children table)
     * Needed after moving the node to a different parent
     * That might create an empty spot in a middle of the children sequence
     * @param nodeUniqueID unique ID of the node which children sequence needs to be fixed
     */
    private void fixChildrenNodeSequence(String nodeUniqueID) {
        int sequenceCounter = 1;
        Cursor cursor = this.sqlite.query("children", new String[]{"node_id", "sequence"}, "father_id = ?", new String[]{nodeUniqueID}, null, null, "sequence ASC", null);
        this.sqlite.beginTransaction();
        try {
            while (cursor.moveToNext()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("sequence", sequenceCounter);
                this.sqlite.update("children", contentValues, "node_id=?", new String[]{cursor.getString(0)});
                sequenceCounter++;
            }
            this.sqlite.setTransactionSuccessful();
        } finally {
            this.sqlite.endTransaction();
        }
        cursor.close();
    }

    /**
     * SQL Database has a XML document inserted in to it in a form of the String
     * With all the tags an attributes the same way as in XML document
     * So SQL document is just a XML document with extra steps
     * @param nodeString String object with all the information of the node or it's table
     * @return NodeList object with content of the node
     */
    private Document getDocumentFromString(String nodeString) {
        try {
            return this.documentBuilder.parse(new ByteArrayInputStream(nodeString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            this.displayToast(context.getString(R.string.toast_error_failed_to_convert_string_to_nodelist));
        }

        return null;
    }

    /**
     * Returns next available children's sequence number of the node
     * sequence number is used to order nodes in the drawer menu
     * @param nodeUniqueID unique id of the node which next available children's sequence number to return
     * @return next available sequence number
     */
    private int getNewNodeSequenceNumber(String nodeUniqueID) {
        Cursor cursor = this.sqlite.rawQuery("SELECT MAX(sequence) FROM children WHERE father_id = ?", new String[] {nodeUniqueID});
        cursor.moveToFirst();
        int sequence = cursor.getInt(0);
        cursor.close();
        return sequence + 1;
    }

    /**
     * Get unique id of parent node of provided node
     * @param nodeUniqueID unique ID of the node which parent unique ID to find
     * @return unique id of the node
     */
    private int getParentNodeUniqueID(String nodeUniqueID) {
        Cursor cursor = this.sqlite.rawQuery("SELECT father_id FROM children WHERE node_id = ?", new String[] {nodeUniqueID});
        cursor.moveToFirst();
        int parentNodeUniqueID = cursor.getInt(0);
        cursor.close();
        return parentNodeUniqueID;
    }

    /**
     * Checks if provided Node object has a subnode(s)
     * @param nodeUniqueID unique ID of the node that is being checked for subnodes
     * @return true if node has a subnode, false - if not
     */
    private boolean hasSubnodes(String nodeUniqueID) {
        // Checks if node with provided unique_id has subnodes
        Cursor cursor = this.sqlite.query("children", new String[]{"node_id"}, "father_id=?", new String[]{nodeUniqueID},null,null,null);
        if (cursor.getCount() > 0) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    /**
     * Creates a clickable span that initiates a context to open/save attached file
     * Arguments that a passed to this function has to be retrieved from the appropriate tables in the database
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeUniqueID unique ID of the node that has file attached in it
     * @param attachedFileFilename filename of the attached file
     * @param time datetime of when file was attached to the node
     * @param originalOffset offset value that the file was originally saved in the database with
     * @param justification justification value that has to be set for the span. It can be retrieved from database. Possible values: left, right, center. Justified - value does not have any value effect.
     * @return Clickable spannableStringBuilder that has spans with image and filename
     */
    private SpannableStringBuilder makeAttachedFileSpan(String nodeUniqueID, String attachedFileFilename, String time, String originalOffset, String justification) {
        SpannableStringBuilder formattedAttachedFile = new SpannableStringBuilder();
        formattedAttachedFile.append(" "); // Needed to insert image
        // Inserting image
        Drawable drawableAttachedFileIcon = AppCompatResources.getDrawable(context, R.drawable.ic_outline_attachment_24);
        drawableAttachedFileIcon.setBounds(0,0, drawableAttachedFileIcon.getIntrinsicWidth(), drawableAttachedFileIcon.getIntrinsicHeight());
        ImageSpanFile attachedFileIcon = new ImageSpanFile(drawableAttachedFileIcon, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        attachedFileIcon.setFromDatabase(true);
        attachedFileIcon.setNodeUniqueId(nodeUniqueID);
        attachedFileIcon.setFilename(attachedFileFilename);
        attachedFileIcon.setTimestamp(time);
        attachedFileIcon.setOriginalOffset(originalOffset);
        formattedAttachedFile.setSpan(attachedFileIcon,0,1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        formattedAttachedFile.append(attachedFileFilename); // Appending filename
        // Detects touches on icon and filename
        ClickableSpanFile imageClickableSpan = new ClickableSpanFile() {
            @Override
            public void onClick(@NonNull View widget) {
            // Launches function in MainView that checks if there is a default action in for attached files
            ((MainView) SQLReader.this.context).saveOpenFile(nodeUniqueID, attachedFileFilename, time, originalOffset);
            }
        };
        formattedAttachedFile.setSpan(imageClickableSpan, 0, attachedFileFilename.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
        if (justification.equals("right")) {
            formattedAttachedFile.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedAttachedFile.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justification.equals("center")) {
            formattedAttachedFile.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedAttachedFile.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedAttachedFile;
    }

    /**
     * Creates SpannableStringBuilder with the content of the CodeNode
     * CodeNode is just a CodeBox that do not have height and width (dimensions)
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param nodeContent content of the code node
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    private SpannableStringBuilder makeFormattedCodeNodeSpan(String nodeContent) {
        SpannableStringBuilder formattedCodeNode = new SpannableStringBuilder();
        formattedCodeNode.append(nodeContent);

        // Changes font
        TypefaceSpan tf = new TypefaceSpan("monospace");
        formattedCodeNode.setSpan(tf, 0, formattedCodeNode.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // Changes background color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
            formattedCodeNode.setSpan(lbs, 0, formattedCodeNode.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return formattedCodeNode;
    }

    /**
     * Creates a codebox span from the provided nodeContent string
     * Formatting depends on new line characters nodeContent string
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param justification justification value that has to be set for the span. It can be retrieved from database. Possible values: left, right, center. Justified - value does not have any value effect.
     * @param nodeContent content of the codebox
     * @param syntax type of syntax used in the codebox. It does not have any difference in SourCherry
     * @param width width value of the codebox that was saved in the database. It does not have any effect in SourCherry
     * @param height height value of the codebox that was saved in the database. It does not have any effect in SourCherry
     * @param widthInPixels is width calculated in pixels or percentages value that was saved in the database. It does not have any effect in SourCherry
     * @param highlightBrackets should codebox highlight brackets. Value should be retrieved from database. It does not have any effect in SourCherry
     * @param showLineNumbers should codebox display line numbers. Value should be retrieved from database. It does not have any effect in SourCherry
     * @return SpannableStringBuilder that has spans marked for string formatting
     */
    private SpannableStringBuilder makeFormattedCodeboxSpan(String justification, String nodeContent, String syntax, int width, int height, boolean widthInPixels, boolean highlightBrackets, boolean showLineNumbers) {
        // Returns SpannableStringBuilder that has spans marked for string formatting
        SpannableStringBuilder formattedCodebox = new SpannableStringBuilder();
        formattedCodebox.append(nodeContent);
        // Changes font
        TypefaceSpanCodebox typefaceSpanCodebox = new TypefaceSpanCodebox("monospace");
        // Saving codebox attribute to the span
        typefaceSpanCodebox.setFrameWidth(width);
        typefaceSpanCodebox.setFrameHeight(height);
        typefaceSpanCodebox.setWidthInPixel(widthInPixels);
        typefaceSpanCodebox.setSyntaxHighlighting(syntax);
        typefaceSpanCodebox.setHighlightBrackets(highlightBrackets);
        typefaceSpanCodebox.setShowLineNumbers(showLineNumbers);
        if (nodeContent.contains("\n")) {
            formattedCodebox.setSpan(typefaceSpanCodebox, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            // Adds vertical line in front the paragraph, to make it stand out as quote
            QuoteSpan qs;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                qs = new QuoteSpan(Color.parseColor("#AC1111"), 5, 30);
            } else {
                qs = new QuoteSpan(Color.RED);
            }
            formattedCodebox.setSpan(qs, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            // Changes background color
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                LineBackgroundSpan.Standard lbs = new LineBackgroundSpan.Standard(this.context.getColor(R.color.codebox_background));
                formattedCodebox.setSpan(lbs, 0, formattedCodebox.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } else {
            formattedCodebox.setSpan(typefaceSpanCodebox, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            BackgroundColorSpan bcs = new BackgroundColorSpan(this.context.getColor(R.color.codebox_background));
            formattedCodebox.setSpan(bcs, 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (justification.equals("right")) {
            formattedCodebox.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justification.equals("center")) {
            formattedCodebox.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedCodebox.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedCodebox;
    }

    /**
     * Creates a SpannableStringBuilder with image in it
     * Image is created from byte[]
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param imageBlob byte[] that has data for the image
     * @param nodeUniqueID unique ID of the node that has the image embedded
     * @param imageOffset offset of the image in the node
     * @param justification justification value that has to be set for the span. It can be retrieved from database. Possible values: left, right, center. Justified - value does not have any value effect.
     * @return SpannableStringBuilder that has spans with image in them
     */
    private SpannableStringBuilder makeImageSpan(byte[] imageBlob, String nodeUniqueID, String imageOffset, String justification) {
        // Returns SpannableStringBuilder that has spans with images in them
        // Images are decoded from byte array that was passed to the function
        SpannableStringBuilder formattedImage = new SpannableStringBuilder();
        ImageSpanImage imageSpanImage;
        //* Adds image to the span
        try {
            formattedImage.append(" ");
            Bitmap decodedByte = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
            Drawable image = new BitmapDrawable(context.getResources(),decodedByte);
            image.setBounds(0,0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            imageSpanImage = new ImageSpanImage(image);
            formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            if (image.getIntrinsicWidth() > width) {
                // If image is wider than screen it is scaled down to fit the screen
                // otherwise it will not load/be displayed
                float scale = ((float) width / image.getIntrinsicWidth()) - (float) 0.1;
                int newWidth = (int) (image.getIntrinsicWidth() * scale);
                int newHeight = (int) (image.getIntrinsicHeight() * scale);
                image.setBounds(0, 0, newWidth, newHeight);
            }

            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting fragment to view enlarged zoomable image
                    ((MainView) context).openImageView(nodeUniqueID, imageOffset);
                }
            };
            formattedImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken image span to display in node content
            imageSpanImage = (ImageSpanImage) this.makeBrokenImageSpan(0);
            formattedImage.setSpan(imageSpanImage, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.displayToast(context.getString(R.string.toast_error_failed_to_load_image));
        }
        //*
        if (justification.equals("right")) {
            formattedImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justification.equals("center")) {
            formattedImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return formattedImage;
    }

    /**
     * Creates a SpannableStringBuilder with image with drawn Latex formula in it
     * This function should not be called directly from any other class
     * It is used in getNodeContent function
     * @param imageBlob byte[] that is actually a String that contains LaTex formula
     * @param justification justification value that has to be set for the span. It can be retrieved from database. Possible values: left, right, center. Justified - value does not have any value effect.
     * @return SpannableStringBuilder that has span with Latex image in them
     */
    private SpannableStringBuilder makeLatexImageSpan(byte[] imageBlob, String justification) {
        // Image is created from byte[] that is passed as an arguments
        SpannableStringBuilder formattedLatexImage = new SpannableStringBuilder();
        ImageSpanLatex imageSpanLatex;
        //* Creates and adds image to the span
        try {
            formattedLatexImage.append(" ");
            String latexString = new String(imageBlob)
                .replace("\\documentclass{article}\n" +
                        "\\pagestyle{empty}\n" +
                        "\\usepackage{amsmath}\n" +
                        "\\begin{document}\n" +
                        "\\begin{align*}", "")
                .replace("\\end{align*}\n\\end{document}", "")
                .replaceAll("&=", "="); // Removing '&' sing, otherwise latex image fails to compile

            final JLatexMathDrawable latexDrawable = JLatexMathDrawable.builder(latexString)
                    .textSize(40)
                    .padding(8)
                    .background(0xFFffffff)
                    .align(JLatexMathDrawable.ALIGN_RIGHT)
                    .build();

            latexDrawable.setBounds(0, 0, latexDrawable.getIntrinsicWidth(), latexDrawable.getIntrinsicHeight());

            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            if (latexDrawable.getIntrinsicWidth() > width - 50) {
                // If image is wider than screen-50 px it is scaled down to fit the screen
                // otherwise it will not load/be display
                float scale = ((float) width / latexDrawable.getIntrinsicWidth()) - (float) 0.2;
                int newWidth = (int) (latexDrawable.getIntrinsicWidth() * scale);
                int newHeight = (int) (latexDrawable.getIntrinsicHeight() * scale);
                latexDrawable.setBounds(0, 0, newWidth, newHeight);
            }

            imageSpanLatex = new ImageSpanLatex(latexDrawable);
            formattedLatexImage.setSpan(imageSpanLatex, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //** Detects image touches/clicks
            ClickableSpan imageClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Starting fragment to view enlarged zoomable image
                    ((MainView) SQLReader.this.context).openImageView(latexString);
                }
            };
            formattedLatexImage.setSpan(imageClickableSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Setting clickableSpan on image
            //**
        } catch (Exception e) {
            // Displays a toast message and appends broken latex image span to display in node content
            imageSpanLatex = (ImageSpanLatex) this.makeBrokenImageSpan(1);
            formattedLatexImage.setSpan(imageSpanLatex, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.displayToast(context.getString(R.string.toast_error_failed_to_compile_latex));
        }
        //*
        if (justification.equals("right")) {
            formattedLatexImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, formattedLatexImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (justification.equals("center")) {
            formattedLatexImage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, formattedLatexImage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        imageSpanLatex.setLatexCode(new String(imageBlob));
        return formattedLatexImage;
    }

    /**
     * This function scans provided Cursor to collect all the nodes from it to be displayed as subnodes in drawer menu
     * Most of the time it is used to collect information about subnodes of the node that is being opened
     * However, it can be used to create information Main menu items
     * In that case isSubnode should passed as false
     * If true this value will make node look indented
     * @param cursor SQL Cursor object that contains nodes from which to make a node list
     * @param isSubnode true - means that node is a subnode and should not be displayed indented in the drawer menu. false - apposite of that
     * @return ArrayList of node's subnodes.
     */
    private ArrayList<ScNode> returnSubnodeArrayList(Cursor cursor, boolean isSubnode) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        while (cursor.moveToNext()) {
            String nodeUniqueID = cursor.getString(1);
            String nameValue = cursor.getString(0);
            boolean hasSubnodes = hasSubnodes(nodeUniqueID);
            boolean isRichText = cursor.getString(3).equals("custom-colors");
            boolean isBold = ((cursor.getInt(2) >> 1) & 0x01) == 1;
            String foregroundColor = "";
            if (((cursor.getInt(2) >> 2) & 0x01) == 1) {
                foregroundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
            }
            int iconId = cursor.getInt(4) >> 1;
            boolean isReadOnly = (cursor.getInt(4) & 0x01) == 1;
            // There is only one parent Node and its added manually in getSubNodes()
            nodes.add(new ScNode(nodeUniqueID, nameValue, false, hasSubnodes, isSubnode, isRichText, isBold, foregroundColor, iconId, isReadOnly));
        }
        return nodes;
    }

    /**
     * Creates an ArrayList of ScNode objects that can be used to display nodes during drawer menu search/filter function
     * ArrayList is created based on node.level value (to exclude node/subnodes from search)
     * @param cursor SQL Cursor object that contains nodes from which to make a node list
     * @return ArrayList that contains all the nodes of the provided cursor object
     */
    private ArrayList<ScNode> returnSubnodeSearchArrayList(Cursor cursor) {
        ArrayList<ScNode> nodes = new ArrayList<>();
        while (cursor.moveToNext()) {
            if (cursor.getInt(3) == 0) {
                // If node and subnodes are not selected to be excluded from search
                String nodeUniqueID = cursor.getString(1);
                String nameValue = cursor.getString(0);
                boolean hasSubnodes = hasSubnodes(nodeUniqueID);
                boolean isRichText = cursor.getString(2).equals("custom-colors");
                boolean isBold = ((cursor.getInt(2) >> 1) & 0x01) == 1;
                String foregroundColor = "";
                if (((cursor.getInt(2) >> 2) & 0x01) == 1) {
                    foregroundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
                }
                int iconId = cursor.getInt(5) >> 1;
                boolean isReadOnly = (cursor.getInt(5) & 0x01) == 1;
                // There are no "parent" nodes in search. All nodes displayed without indentation
                nodes.add(new ScNode(nodeUniqueID, nameValue, false, hasSubnodes, false, isRichText, isBold, foregroundColor, iconId, isReadOnly));
                if (hasSubnodes) {
                    Cursor subCursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.level, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{String.valueOf(nodeUniqueID)});
                    nodes.addAll(returnSubnodeSearchArrayList(subCursor));
                    subCursor.close();
                }
            } else if (cursor.getInt(3) == 1) {
                // If only node is selected to be excluded from search
                String nodeUniqueID = cursor.getString(1);
                boolean hasSubnodes = hasSubnodes(nodeUniqueID);
                if (hasSubnodes) {
                    Cursor subCursor = this.sqlite.rawQuery("SELECT node.name, node.node_id, node.is_richtxt, node.level, node.syntax, node.is_ro FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{String.valueOf(nodeUniqueID)});
                    nodes.addAll(returnSubnodeSearchArrayList(subCursor));
                    subCursor.close();
                }
            } else if (cursor.getInt(3) == 2) {
                // if only subnodes are selected to be excluded from search
                String nodeUniqueID = cursor.getString(1);
                String nameValue = cursor.getString(0);
                boolean hasSubnodes = hasSubnodes(nodeUniqueID);
                boolean isRichText = cursor.getString(2).equals("custom-colors");
                boolean isBold = ((cursor.getInt(2) >> 1) & 0x01) == 1;
                String foregroundColor = "";
                if (((cursor.getInt(2) >> 2) & 0x01) == 1) {
                    foregroundColor = String.format("#%06x", ((cursor.getInt(2) >> 3) & 0xffffff));
                }
                int iconId = cursor.getInt(5) >> 1;
                boolean isReadOnly = (cursor.getInt(5) & 0x01) == 1;
                // There is only one parent Node and its added manually in getSubNodes()
                nodes.add(new ScNode(nodeUniqueID, nameValue, false, hasSubnodes, false, isRichText, isBold, foregroundColor, iconId, isReadOnly));
            }
        }
        return nodes;
    }

    /**
     * Searches through all nodes without skipping marked to exclude nodes
     * @param parentUniqueID unique ID of the node to search in
     * @param query string to search for
     * @return ArrayList of search result objects
     */
    private ArrayList<ScSearchNode> searchAllNodes(String parentUniqueID, String query) {
        // It actually just filters node and it's subnodes
        // The search of the string is done in findInNode()
        Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{parentUniqueID});
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();
        while (cursor.moveToNext()) {
            String nodeUniqueID = String.valueOf(cursor.getInt(0));
            boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
            boolean isParent = false;
            boolean isSubnode = true;
            if (hasSubnode) {
                isParent = true;
                isSubnode = false;
            }
            ScSearchNode result = this.findInNode(cursor, query, hasSubnode, isParent, isSubnode);
            if (result != null) {
                searchResult.add(result);
            }
            if (hasSubnode) {
                searchResult.addAll(this.searchAllNodes(nodeUniqueID, query));
            }
        }
        cursor.close();
        return searchResult;
    }

    /**
     * Searches through nodes skipping marked to exclude
     * @param parentUniqueID unique ID of the node to search in
     * @param query string to search for
     * @return ArrayList of search result objects
     */
    private ArrayList<ScSearchNode> searchNodesSkippingExcluded(String parentUniqueID, String query) {
        // If user marked that filter should omit nodes and/or node children from filter results
        ArrayList<ScSearchNode> searchResult = new ArrayList<>();

        Cursor cursor = this.sqlite.rawQuery("SELECT * FROM node INNER JOIN children ON node.node_id=children.node_id WHERE children.father_id=? ORDER BY sequence ASC", new String[]{parentUniqueID});

        while (cursor.moveToNext()) {
            if (cursor.getInt(10) == 0) {
                // If node and subnodes are not selected to be excluded from search
                String nodeUniqueID = String.valueOf(cursor.getInt(0));
                boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
                boolean isParent = false;
                boolean isSubnode = true;
                if (hasSubnode) {
                    isParent = true;
                    isSubnode = false;
                }
                ScSearchNode result = this.findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
                if (hasSubnode) {
                    searchResult.addAll(searchNodesSkippingExcluded(nodeUniqueID, query));
                }
            } else if (cursor.getInt(10) == 1) {
                // If only the node is selected to be excluded from search
                String nodeUniqueID = String.valueOf(cursor.getInt(0));
                String hasSubnode = String.valueOf(hasSubnodes(nodeUniqueID));
                if (hasSubnode.equals("true")) {
                    searchResult.addAll(searchNodesSkippingExcluded(nodeUniqueID, query));
                }
            } else if (cursor.getInt(10) == 2) {
                // if only subnodes are selected to be excluded from search
                String nodeUniqueID = String.valueOf(cursor.getInt(0));
                boolean hasSubnode = this.hasSubnodes(nodeUniqueID);
                boolean isParent = false;
                boolean isSubnode = true;
                if (hasSubnode) {
                    isParent = true;
                    isSubnode = false;
                }
                ScSearchNode result = this.findInNode(cursor, query, hasSubnode, isParent, isSubnode);
                if (result != null) {
                    searchResult.add(result);
                }
            }
        }
        cursor.close();
        return searchResult;
    }

    @Override
    public void vacuum() {
        this.sqlite.execSQL("VACUUM");
    }
}
